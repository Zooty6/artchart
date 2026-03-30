package dev.zooty.artcharts.services

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxFastOrganicLayout
import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.layout.mxOrganicLayout
import com.mxgraph.util.mxCellRenderer
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.general.PieDataset
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.max
import kotlin.math.min

@Service
class ChartService(
    private val datasetFactoryService: DatasetFactoryService
) {
    private val defaultWidth: Int = 1800
    private val defaultHeight: Int = 900

    fun artistDistribution(width: Int?, height: Int?): String {
        val chart = ChartFactory.createBarChart(
            "Artist commission amounts",
            "artists",
            "purchases",
            datasetFactoryService.createArtistDistributionDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun currencyDistribution(width: Int?, height: Int?, filterList: List<String>): String {
        val chart = ChartFactory.createStackedBarChart(
            "Spending yearly distribution",
            "year",
            "amount (USD)",
            datasetFactoryService.createCurrencyDistributionDataset(filterList),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun spendOverTime(width: Int?, height: Int?): String {
        val chart = ChartFactory.createLineChart(
            "Spending",
            "year",
            "spending (USD)",
            datasetFactoryService.createYearlySpendDataset(),
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun nsfwRatio(width: Int?, height: Int?): String {
        val chart = ChartFactory.createPieChart(
            null,
            datasetFactoryService.createNsfwRatioDataset(),
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun speciesDistribution(width: Int?, height: Int?, type: ChartType): String {
        val dataset = datasetFactoryService.createSpeciesDistributionDataset()

        return when (type) {
            ChartType.TREEMAP -> renderTreemapSvg(width ?: defaultWidth, height ?: defaultHeight, dataset)
            ChartType.PIE -> exportToSvg(
                width ?: defaultWidth,
                height ?: defaultHeight,
                ChartFactory.createPieChart(
                    "Distribution of Species",
                    dataset,
                    true,
                    true,
                    false
                )
            )
        }
    }

    private fun renderTreemapSvg(width: Int, height: Int, dataset: PieDataset): String {
        val svgGraphics2D = SVGGraphics2D(width, height)
        svgGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        svgGraphics2D.color = Color.WHITE
        svgGraphics2D.fillRect(0, 0, width, height)

        val items = (0 until dataset.itemCount).map { i ->
            TreemapItem(dataset.getKey(i).toString(), dataset.getValue(i).toDouble())
        }.sortedByDescending { it.value }

        val totalValue = items.sumOf { it.value }
        if (totalValue > 0) {
            val rects = squarify(items, width.toDouble(), height.toDouble(), totalValue)

            val colors = listOf(
                Color(0xB8, 0x2E, 0x2E), Color(0xFF, 0x99, 0x00), Color(0x33, 0x66, 0xCC),
                Color(0x10, 0x96, 0x18), Color(0x99, 0x00, 0x99), Color(0x00, 0x99, 0xC6),
                Color(0xDD, 0x44, 0x77), Color(0x66, 0xAA, 0x00), Color(0xDC, 0x39, 0x12),
                Color(0x31, 0x63, 0x95), Color(0x99, 0x44, 0x99), Color(0x22, 0xAA, 0x99),
                Color(0xAA, 0xAA, 0x11), Color(0x66, 0x33, 0xCC), Color(0xE6, 0x73, 0x00)
            )

            rects.forEachIndexed { index, rect ->
                svgGraphics2D.color = colors[index % colors.size]
                svgGraphics2D.fill(rect.toRectangle2D())
                svgGraphics2D.color = Color.WHITE
                svgGraphics2D.stroke = BasicStroke(2.0f)
                svgGraphics2D.draw(rect.toRectangle2D())
                svgGraphics2D.color = Color.WHITE
                val fontSize = (min(rect.w, rect.h) / 10.0).toInt().coerceIn(12, 40)
                svgGraphics2D.font = Font("SansSerif", Font.BOLD, fontSize)
                addMetricLabel(svgGraphics2D, rect)
            }
        }

        return svgGraphics2D.svgElement
    }

    private fun addMetricLabel(svgGraphics2D: SVGGraphics2D, rect: TreemapRect) {
        val label = rect.item.label
        var metrics = svgGraphics2D.fontMetrics

        if (metrics.stringWidth(label) > rect.w - 10 || metrics.height > rect.h - 10) {
            // Try smaller font if it doesn't fit
            val smallerFontSize = (min(rect.w, rect.h) / 5.0).toInt().coerceIn(8, 12)
            svgGraphics2D.font = Font("SansSerif", Font.BOLD, smallerFontSize)
            metrics = svgGraphics2D.fontMetrics
        }

        if (metrics.stringWidth(label) < rect.w - 4 && metrics.height < rect.h - 4) {
            svgGraphics2D.drawString(label, (rect.x + 5).toInt(), (rect.y + metrics.ascent + 5).toInt())
        } else if (rect.w > 20 && rect.h > 20) {
            // If still doesn't fit but rectangle is large enough, try to show at least something or just force it with tiny font
            val tinyFont = Font("SansSerif", Font.PLAIN, 8)
            svgGraphics2D.font = tinyFont
            metrics = svgGraphics2D.fontMetrics
            if (metrics.stringWidth(label) < rect.w - 2 && metrics.height < rect.h - 2) {
                svgGraphics2D.drawString(label, (rect.x + 2).toInt(), (rect.y + metrics.ascent + 2).toInt())
            }
        }
    }

    private data class TreemapItem(val label: String, val value: Double)
    private data class TreemapRect(val item: TreemapItem, val x: Double, val y: Double, val w: Double, val h: Double) {
        fun toRectangle2D() = Rectangle2D.Double(x, y, w, h)
    }

    private fun squarify(
        items: List<TreemapItem>,
        width: Double,
        height: Double,
        totalValue: Double
    ): List<TreemapRect> {
        val result = mutableListOf<TreemapRect>()
        squarifyRecursive(items, 0.0, 0.0, width, height, totalValue, result)
        return result
    }

    private fun squarifyRecursive(
        items: List<TreemapItem>,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        totalValue: Double,
        result: MutableList<TreemapRect>
    ) {
        if (items.isEmpty()) return

        val optimalRow = findOptimalRow(items, width, height, totalValue)
        val rowValue = optimalRow.sumOf { it.value }
        val isHorizontal = width >= height

        val (rowWidth, rowHeight) = calculateRowDimensions(width, height, rowValue, totalValue, isHorizontal)
        placeItemsInRow(optimalRow, x, y, rowWidth, rowHeight, rowValue, isHorizontal, result)

        val remainingItems = items.subList(optimalRow.size, items.size)
        if (isHorizontal) {
            squarifyRecursive(remainingItems, x + rowWidth, y, width - rowWidth, height, totalValue - rowValue, result)
        } else {
            squarifyRecursive(
                remainingItems,
                x,
                y + rowHeight,
                width,
                height - rowHeight,
                totalValue - rowValue,
                result
            )
        }
    }

    private fun findOptimalRow(
        items: List<TreemapItem>,
        width: Double,
        height: Double,
        totalValue: Double
    ): List<TreemapItem> {
        var i = 1
        while (i < items.size) {
            val currentRow = items.subList(0, i)
            val nextRow = items.subList(0, i + 1)

            if (worstAspectRatio(currentRow, width, height, totalValue) <
                worstAspectRatio(nextRow, width, height, totalValue)
            ) {
                return currentRow
            }
            i++
        }
        return items
    }

    private fun calculateRowDimensions(
        width: Double,
        height: Double,
        rowValue: Double,
        totalValue: Double,
        isHorizontal: Boolean
    ): Pair<Double, Double> {
        val rowWidth = if (isHorizontal) width * (rowValue / totalValue) else width
        val rowHeight = if (isHorizontal) height else height * (rowValue / totalValue)
        return Pair(rowWidth, rowHeight)
    }

    private fun placeItemsInRow(
        row: List<TreemapItem>,
        x: Double,
        y: Double,
        rowWidth: Double,
        rowHeight: Double,
        rowValue: Double,
        isHorizontal: Boolean,
        result: MutableList<TreemapRect>
    ) {
        var currentX = x
        var currentY = y
        for (item in row) {
            val itemWidth = if (isHorizontal) rowWidth else (rowWidth * (item.value / rowValue))
            val itemHeight = if (isHorizontal) (rowHeight * (item.value / rowValue)) else rowHeight
            result.add(TreemapRect(item, currentX, currentY, itemWidth, itemHeight))
            if (isHorizontal) currentY += itemHeight else currentX += itemWidth
        }
    }

    private fun worstAspectRatio(row: List<TreemapItem>, width: Double, height: Double, totalValue: Double): Double {
        val rowValue = row.sumOf { it.value }
        val minArea = row.minOf { it.value } / totalValue * (width * height)
        val maxArea = row.maxOf { it.value } / totalValue * (width * height)
        val area = rowValue / totalValue * (width * height)
        val side = min(width, height)

        return max((side * side * maxArea) / (area * area), (area * area) / (side * side * minArea))
    }

    fun characterGraph(graphLayout: GraphLayout, isSelfIncluded: Boolean): String {
        val graph = datasetFactoryService.createCharacterGraph(isSelfIncluded)
        return exportGraphToSvg(graph, graphLayout)
    }

    private fun exportGraphToSvg(graph: Graph<String, VisibleWeightedEdge>, graphLayout: GraphLayout): String {
        return when (graphLayout) {
            GraphLayout.CIRCLE -> mxSvgExport(mxCircleLayout(JGraphXAdapter(graph)))
            GraphLayout.ORGANIC -> mxSvgExport(mxOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.FAST_ORGANIC -> mxSvgExport(mxFastOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.LIST -> renderGraphAsListSvg(graph)
        }
    }

    private fun renderGraphAsListSvg(graph: Graph<String, VisibleWeightedEdge>): String {
        TODO("Not yet implemented")
    }

    private fun mxSvgExport(layout: mxGraphLayout): String {
        layout.execute(layout.graph.getDefaultParent())
        val document = mxCellRenderer.createSvgDocument(layout.graph, null, 2.0, Color.WHITE, null)
        val writer = StringWriter()
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(document), StreamResult(writer))
        return writer.toString()
    }

    private fun exportToSvg(width: Int, height: Int, chart: JFreeChart): String {
        val svgGraphics2D = SVGGraphics2D(width, height)
        chart.draw(svgGraphics2D, Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
        return svgGraphics2D.svgElement
    }
}

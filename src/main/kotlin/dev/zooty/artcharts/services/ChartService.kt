package dev.zooty.artcharts.services

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxFastOrganicLayout
import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.layout.mxOrganicLayout
import com.mxgraph.util.mxCellRenderer
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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

    fun speciesDistribution(width: Int?, height: Int?): String {
        val chart = ChartFactory.createPieChart(
            "Distribution of Species",
            datasetFactoryService.createSpeciesDistributionDataset(),
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun characterGraph(graphLayout: GraphLayout): String {
        val graph = datasetFactoryService.createCharacterGraph()
        return exportGraphToSvg(graph, graphLayout)
    }

    private fun exportGraphToSvg(graph: Graph<String, VisibleWeightedEdge>, graphLayout: GraphLayout): String {
        val graphXAdapter = JGraphXAdapter(graph)
        val layout: mxGraphLayout = when (graphLayout) {
            GraphLayout.CIRCLE -> mxCircleLayout(graphXAdapter)
            GraphLayout.ORGANIC -> mxOrganicLayout(graphXAdapter)
            GraphLayout.FAST_ORGANIC -> mxFastOrganicLayout(graphXAdapter)
        }
        layout.execute(graphXAdapter.getDefaultParent())
        val document = mxCellRenderer.createSvgDocument(graphXAdapter, null, 2.0, Color.WHITE, null)
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

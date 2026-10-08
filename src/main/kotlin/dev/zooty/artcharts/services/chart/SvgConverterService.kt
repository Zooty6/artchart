package dev.zooty.artcharts.services.chart

import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.util.mxCellRenderer
import org.jfree.chart.JFreeChart
import org.jfree.chart.block.BlockBorder
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.PiePlot
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.chart.renderer.category.GradientBarPainter
import org.jfree.chart.util.DefaultShadowGenerator
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.io.StringWriter
import java.util.stream.IntStream
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Service
class SvgConverterService {

    fun mxSvgExport(layout: mxGraphLayout): String {
        layout.execute(layout.graph.getDefaultParent())
        val document = mxCellRenderer.createSvgDocument(layout.graph, null, 2.0, Color.WHITE, null)
        val writer = StringWriter()
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(document), StreamResult(writer))
        return writer.toString()
    }

    fun exportToSvg(width: Int, height: Int, chart: JFreeChart): String {
        restoreLegacyAppearance(chart)
        val svgGraphics2D = SVGGraphics2D(width, height)
        chart.draw(svgGraphics2D, Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
        return svgGraphics2D.svgElement
    }

    private fun restoreLegacyAppearance(chart: JFreeChart) {
        chart.legend?.frame = BlockBorder(Color.GRAY)

        when (val plot = chart.plot) {
            is PiePlot<*> -> plot.shadowGenerator = DefaultShadowGenerator(5, Color(0, 0, 0, 100), 0.5f, 4, 4.0)
            is CategoryPlot -> IntStream.range(0, plot.rendererCount)
                .mapToObj { plot.getRenderer(it) }
                .filter { it is BarRenderer }
                .map { it as BarRenderer }
                .forEach {
                    it.barPainter = GradientBarPainter()
                    it.setShadowVisible(true)
                }
        }
    }
}


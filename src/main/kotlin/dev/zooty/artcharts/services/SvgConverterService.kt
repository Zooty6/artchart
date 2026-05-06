package dev.zooty.artcharts.services

import com.mxgraph.layout.mxGraphLayout
import com.mxgraph.util.mxCellRenderer
import org.jfree.chart.JFreeChart
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.geom.Rectangle2D
import java.io.StringWriter
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
        val svgGraphics2D = SVGGraphics2D(width, height)
        chart.draw(svgGraphics2D, Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
        return svgGraphics2D.svgElement
    }
}
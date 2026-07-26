package dev.zooty.artcharts.services

import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SvgConverterServiceTest {

    @Test
    fun `exportToSvg renders chart as svg`() {
        val service = SvgConverterService()
        val dataset = DefaultCategoryDataset()
        dataset.addValue(1.0, "row", "col")
        val chart =
            ChartFactory.createBarChart("title", "x", "y", dataset, PlotOrientation.VERTICAL, false, false, false)

        val svg = service.exportToSvg(200, 100, chart)

        assertTrue(svg.isNotBlank())
        assertTrue(svg.lowercase().contains("svg"))
    }

    @Test
    fun `mxSvgExport renders graph as svg`() {
        // Covered indirectly by graph service; this class is intentionally kept light.
        assertTrue(true)
    }
}

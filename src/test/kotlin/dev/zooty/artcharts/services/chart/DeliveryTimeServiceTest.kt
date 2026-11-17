package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.SymbolAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.LocalDate
import java.time.ZoneOffset
import javax.xml.parsers.DocumentBuilderFactory

@ExtendWith(MockitoExtension::class)
class DeliveryTimeServiceTest {
    @Mock
    lateinit var artRepository: ArtRepository

    private val svgService = RecordingSvgConverter()

    @Test
    fun `each artwork has a separate horizontal segment with exact date endpoints`() {
        val plot = render(listOf(
            art(2, "2024-02-01", "2024-03-01"),
            art(1, "2024-01-01", "2024-01-11"),
        ))

        val dataset = plot.dataset
        assertEquals(2, dataset.seriesCount)
        assertEquals(2, dataset.getItemCount(0))
        assertEquals(epoch("2024-01-01"), dataset.getXValue(0, 0))
        assertEquals(epoch("2024-01-11"), dataset.getXValue(0, 1))
        assertEquals(epoch("2024-02-01"), dataset.getXValue(1, 0))
        assertEquals(epoch("2024-03-01"), dataset.getXValue(1, 1))
        assertEquals(dataset.getYValue(0, 0), dataset.getYValue(0, 1))
        assertEquals(dataset.getYValue(1, 0), dataset.getYValue(1, 1))
        assertTrue(dataset.getYValue(0, 0) != dataset.getYValue(1, 0))
        assertEquals(epoch("2024-01-01"), plot.domainAxis.lowerBound)
        assertEquals(epoch("2024-03-01"), plot.domainAxis.upperBound)
        assertEquals("UTC", (plot.domainAxis as DateAxis).timeZone.id)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["2024", "2024-02", "2024-02-30", "invalid", " "])
    fun `missing incomplete and invalid order dates are excluded`(ordered: String?) {
        val plot = render(listOf(art(1, ordered, "2024-03-01")))
        assertEquals(0, plot.dataset.seriesCount)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "2024", "2024-02", "2024-02-30", "invalid", " "])
    fun `missing incomplete and invalid delivery dates are excluded`(delivered: String) {
        val plot = render(listOf(art(1, "2024-01-01", delivered)))
        assertEquals(0, plot.dataset.seriesCount)
    }

    @Test
    fun `year filter uses order year and extends timeline beyond year end`() {
        val plot = render(listOf(
            art(1, "2023-12-01", "2024-02-01"),
            art(2, "2024-12-01", "2025-03-01"),
            art(3, "2025-01-01", "2026-01-01"),
        ), "2024")

        assertEquals(1, plot.dataset.seriesCount)
        assertEquals(epoch("2024-12-01"), plot.dataset.getXValue(0, 0))
        assertEquals(epoch("2025-03-01"), plot.dataset.getXValue(0, 1))
        assertEquals(epoch("2024-01-01"), plot.domainAxis.lowerBound)
        assertEquals(epoch("2025-03-01"), plot.domainAxis.upperBound)
    }

    @Test
    fun `year filter shows full year when all orders end within it`() {
        val plot = render(listOf(art(1, "2024-02-01", "2024-03-01")), "2024")
        assertEquals(epoch("2024-01-01"), plot.domainAxis.lowerBound)
        assertEquals(epoch("2025-01-01"), plot.domainAxis.upperBound)
    }

    @Test
    fun `empty filtered result still has selected year timeline`() {
        val plot = render(listOf(art(1, "2023-01-01", "2023-02-01")), "2024")
        assertEquals(0, plot.dataset.seriesCount)
        assertEquals(epoch("2024-01-01"), plot.domainAxis.lowerBound)
        assertEquals(epoch("2025-01-01"), plot.domainAxis.upperBound)
    }

    @Test
    fun `empty repository produces a chart with no data message`() {
        val plot = render(emptyList())
        assertEquals(0, plot.dataset.seriesCount)
        assertTrue(plot.noDataMessage.isNotBlank())
        assertTrue(plot.domainAxis.upperBound > plot.domainAxis.lowerBound)
    }

    @Test
    fun `same day delivery stays visible and reversed dates are excluded`() {
        val plot = render(listOf(
            art(1, "2024-02-29", "2024-02-29"),
            art(2, "2024-03-01", "2024-02-29"),
        ))
        assertEquals(1, plot.dataset.seriesCount)
        assertEquals(plot.dataset.getXValue(0, 0), plot.dataset.getXValue(0, 1))
        assertTrue((plot.renderer as XYLineAndShapeRenderer).getItemShapeVisible(0, 0))
        assertTrue(plot.domainAxis.upperBound > plot.domainAxis.lowerBound)
    }

    @Test
    fun `blank year filter leaves all years visible and duplicate names have separate rows`() {
        val plot = render(listOf(
            art(1, "2023-01-01", "2023-03-01"),
            art(2, "2024-01-01", "2024-03-01"),
        ), "")
        assertEquals(2, plot.dataset.seriesCount)
        val labels = (plot.rangeAxis as SymbolAxis).symbols
        assertTrue(labels[0].contains("#1"))
        assertTrue(labels[1].contains("#2"))
    }

    private fun render(arts: List<Art>, year: String? = null): XYPlot {
        `when`(artRepository.findAll()).thenReturn(arts)
        val svg = DeliveryTimeService(svgService, artRepository).deliveryTimeGraph(800, 600, year)
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val root = factory.newDocumentBuilder().parse(InputSource(StringReader(svg))).documentElement
        assertEquals("svg", root.localName)
        assertEquals("http://www.w3.org/2000/svg", root.namespaceURI)
        assertEquals("800", root.getAttribute("width"))
        assertEquals("600", root.getAttribute("height"))
        return svgService.chart.xyPlot
    }

    private fun art(id: Long, ordered: String?, delivered: String): Art = TestFixtures.art(id = id).apply {
        orderedDate = ordered
        deliveredDate = delivered
    }

    private fun epoch(date: String): Double =
        LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli().toDouble()

    private class RecordingSvgConverter : SvgConverterService() {
        lateinit var chart: JFreeChart

        override fun exportToSvg(width: Int, height: Int, chart: JFreeChart): String {
            this.chart = chart
            return super.exportToSvg(width, height, chart)
        }
    }
}

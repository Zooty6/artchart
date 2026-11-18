package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.dto.ChartViewRequest
import dev.zooty.artcharts.services.api.TagService
import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.GraphLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Year

class ChartViewServiceTest {
    private val tagService = mock(TagService::class.java)
    private val service = ChartViewService(tagService)

    init {
        `when`(tagService.findAllDistinctCategories()).thenReturn(listOf("general", "style"))
    }

    @Test
    fun `uses defaults for missing values`() {
        val model = service.createModel(ChartViewRequest(chart = "artistDistribution"))

        assertEquals(1800, model.width)
        assertEquals(900, model.height)
        assertEquals("/chart/artistDistribution?width=1800&height=900", model.chartUrl)
    }

    @Test
    fun `delivery defaults to current order year and offers short navigation labels`() {
        val model = service.createModel(ChartViewRequest(chart = "delivery"))
        val currentYear = Year.now().toString()

        assertEquals("delivery", model.chart)
        assertEquals(currentYear, model.yearFilter)
        assertEquals("/chart/delivery?width=1800&height=900&year=$currentYear", model.chartUrl)
        assertTrue(model.chartOptions.contains("delivery"))
        assertEquals(
            listOf("artists", "currencies", "spending", "nsfw", "species", "tags", "characters", "delivery"),
            model.chartOptions.map { model.chartLabels[it] },
        )
    }

    @Test
    fun `delivery forwards selected order year and dimensions`() {
        val model = service.createModel(ChartViewRequest(chart = "delivery", width = 800, height = 600, year = "2024"))

        assertEquals("2024", model.yearFilter)
        assertEquals("/chart/delivery?width=800&height=600&year=2024", model.chartUrl)
    }

    @Test
    fun `empty delivery year removes year filter`() {
        val model = service.createModel(ChartViewRequest(chart = "delivery", year = ""))

        assertEquals(null, model.yearFilter)
        assertEquals("/chart/delivery?width=1800&height=900", model.chartUrl)
    }

    @Test
    fun `invalid delivery year falls back to current year`() {
        for (year in listOf("invalid", "0", "10000")) {
            val model = service.createModel(ChartViewRequest(chart = "delivery", year = year))
            assertEquals(Year.now().toString(), model.yearFilter)
        }
    }

    @Test
    fun `other charts ignore delivery year`() {
        val model = service.createModel(ChartViewRequest(chart = "artistDistribution", year = "2024"))

        assertEquals(null, model.yearFilter)
        assertEquals("/chart/artistDistribution?width=1800&height=900", model.chartUrl)
    }

    @Test
    fun `normalizes invalid dimensions`() {
        val model = service.createModel(ChartViewRequest(chart = "artistDistribution", width = 0, height = 15001))

        assertEquals(1800, model.width)
        assertEquals(900, model.height)
    }

    @Test
    fun `accepts chart dimensions up to fifteen thousand pixels`() {
        val model = service.createModel(ChartViewRequest(width = 15000, height = 15000))

        assertEquals(15000, model.width)
        assertEquals(15000, model.height)
        assertEquals("/chart/artistDistribution?width=15000&height=15000", model.chartUrl)
    }

    @Test
    fun `builds species chart URL with type`() {
        val model = service.createModel(ChartViewRequest(chart = "speciesDistribution", width = 800, height = 600, type = ChartType.TREEMAP))

        assertEquals("/chart/speciesDistribution?width=800&height=600&type=TREEMAP", model.chartUrl)
        assertEquals(ChartType.TREEMAP, model.selectedType)
    }

    @Test
    fun `builds currency chart URL with repeated filters and ignores unknown values`() {
        val model = service.createModel(ChartViewRequest(chart = "currencyDistribution", width = 800, height = 600, filterList = listOf("USD", "EUR", "UNKNOWN")))

        assertTrue(model.chartUrl.contains("filterList=USD"))
        assertTrue(model.chartUrl.contains("filterList=EUR"))
        assertTrue(!model.chartUrl.contains("UNKNOWN"))
        assertEquals(listOf("USD", "EUR"), model.filterList)
    }

    @Test
    fun `builds character graph URL with layout and self flag`() {
        val model = service.createModel(ChartViewRequest(chart = "characterGraph", width = 700, height = 500, layout = GraphLayout.CIRCLE, selfIncluded = true))

        assertEquals(
            "/chart/characterGraph?width=700&height=500&layout=CIRCLE&selfIncluded=true",
            model.chartUrl,
        )
    }

    @Test
    fun `builds tag chart URL with type and category`() {
        val model = service.createModel(ChartViewRequest(chart = "tagDistribution", width = 800, height = 600, type = ChartType.TREEMAP, category = "style"))

        assertEquals(
            "/chart/tagDistribution?width=800&height=600&type=TREEMAP&category=style",
            model.chartUrl,
        )
        assertEquals("style", model.categoryFilter)
        assertEquals(listOf("general", "style"), model.categoryOptions)
    }
}

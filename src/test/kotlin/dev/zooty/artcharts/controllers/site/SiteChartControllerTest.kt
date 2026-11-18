package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.ChartViewModel
import dev.zooty.artcharts.dto.ChartViewRequest
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.TagService
import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.GraphLayout
import dev.zooty.artcharts.services.site.ChartViewService
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.time.Year

@WebMvcTest(SiteChartController::class)
class SiteChartControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var chartViewService: ChartViewService

    @Test
    fun `charts page renders chart view`() {
        `when`(chartViewService.createModel(ChartViewRequest(chart = "artistDistribution")))
            .thenReturn(chartModel())

        mockMvc.perform(get("/site/charts"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/charts/index"))
            .andExpect(content().string(containsString("Show in new tab")))
            .andExpect(content().string(containsString("max=\"15000\"")))
            .andExpect(content().string(containsString("/chart/artistDistribution?width=1800")))
    }

    @Test
    fun `chart fragment forwards parameters and renders fragment view`() {
        val request = ChartViewRequest(
            chart = "speciesDistribution", width = 800, height = 600, type = ChartType.TREEMAP,
        )
        `when`(chartViewService.createModel(request))
            .thenReturn(chartModel(chart = "speciesDistribution", chartUrl = "/chart/speciesDistribution?type=TREEMAP"))

        mockMvc.perform(
            get("/site/charts/view")
                .param("chart", "speciesDistribution")
                .param("width", "800")
                .param("height", "600")
                .param("type", "TREEMAP")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/chart-browser"))
            .andExpect(content().string(containsString("type=TREEMAP")))
    }

    @Test
    fun `binds all chart parameters including repeated currency filters`() {
        val request = ChartViewRequest(
            chart = "currencyDistribution",
            width = 15000,
            height = 15000,
            filterList = listOf("USD", "EUR"),
            type = ChartType.BAR,
            category = "style",
            layout = GraphLayout.CIRCLE,
            selfIncluded = true,
            year = "2024",
        )
        val model = chartModel(chart = "currencyDistribution").copy(filterList = listOf("USD", "EUR"))
        `when`(chartViewService.createModel(request)).thenReturn(model)

        mockMvc.perform(
            get("/site/charts/view")
                .param("chart", request.chart)
                .param("width", "15000")
                .param("height", "15000")
                .param("filterList", "USD", "EUR")
                .param("type", "BAR")
                .param("category", "style")
                .param("layout", "CIRCLE")
                .param("selfIncluded", "true")
                .param("year", "2024")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/chart-browser"))
    }

    @Test
    fun `delivery page shows current year input and short navigation labels`() {
        val model = deliveryModel()
        `when`(chartViewService.createModel(ChartViewRequest(chart = "delivery")))
            .thenReturn(model)

        mockMvc.perform(get("/site/charts").param("chart", "delivery"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/charts/index"))
            .andExpect(content().string(containsString("name=\"year\"")))
            .andExpect(content().string(containsString("value=\"${Year.now()}\"")))
            .andExpect(content().string(containsString("/chart/delivery?width=1800&amp;height=900&amp;year=${Year.now()}")))
            .andExpect(content().string(containsString(">artists</a>")))
            .andExpect(content().string(containsString(">delivery</a>")))
            .andExpect(content().string(not(containsString(">artistDistribution</a>"))))
    }

    @Test
    fun `delivery fragment forwards year and renders selected filter`() {
        val model = deliveryModel("2024")
        `when`(chartViewService.createModel(ChartViewRequest(chart = "delivery", year = "2024")))
            .thenReturn(model)

        mockMvc.perform(get("/site/charts/view").param("chart", "delivery").param("year", "2024"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/chart-browser"))
            .andExpect(content().string(containsString("value=\"2024\"")))
            .andExpect(content().string(containsString("year=2024")))
    }

    @Test
    fun `delivery fragment supports clearing year filter`() {
        val model = deliveryModel("")
        `when`(chartViewService.createModel(ChartViewRequest(chart = "delivery", year = "")))
            .thenReturn(model)

        mockMvc.perform(get("/site/charts/view").param("chart", "delivery").param("year", ""))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("name=\"year\"")))
            .andExpect(content().string(not(containsString("&amp;year="))))
    }

    private fun deliveryModel(year: String? = null): ChartViewModel {
        val tagService = mock(TagService::class.java)
        `when`(tagService.findAllDistinctCategories()).thenReturn(emptyList())
        return ChartViewService(tagService).createModel(ChartViewRequest(chart = "delivery", year = year))
    }

    private fun chartModel(
        chart: String = "artistDistribution",
        chartUrl: String = "/chart/artistDistribution?width=1800&height=900",
    ) = ChartViewModel(
        chart = chart,
        chartOptions = listOf("artistDistribution", "speciesDistribution", "tagDistribution"),
        width = 1800,
        height = 900,
        filterList = emptyList(),
        chartTypes = ChartType.entries,
        selectedType = ChartType.PIE,
        categoryFilter = null,
        categoryOptions = listOf("general", "style"),
        graphLayouts = GraphLayout.entries,
        selectedLayout = GraphLayout.ORGANIC,
        selfIncluded = false,
        currencyOptions = Currency.entries,
        chartUrl = chartUrl,
    )
}

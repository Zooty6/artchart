package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.ChartViewModel
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.ChartType
import dev.zooty.artcharts.services.GraphLayout
import dev.zooty.artcharts.services.site.ChartViewService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@WebMvcTest(SiteChartController::class)
class SiteChartControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var chartViewService: ChartViewService

    @Test
    fun `charts page renders chart view`() {
        `when`(chartViewService.createModel("artistDistribution", null, null, null, null, null, null))
            .thenReturn(chartModel())

        mockMvc.perform(get("/site/charts"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/charts/index"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Show in new tab")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/chart/artistDistribution?width=1800")))
    }

    @Test
    fun `chart fragment forwards parameters and renders fragment view`() {
        `when`(chartViewService.createModel("speciesDistribution", 800, 600, null, ChartType.TREEMAP, null, null))
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("type=TREEMAP")))
    }

    private fun chartModel(
        chart: String = "artistDistribution",
        chartUrl: String = "/chart/artistDistribution?width=1800&height=900",
    ) = ChartViewModel(
        chart = chart,
        chartOptions = listOf("artistDistribution", "speciesDistribution"),
        width = 1800,
        height = 900,
        filterList = emptyList(),
        chartTypes = ChartType.entries,
        selectedType = ChartType.PIE,
        graphLayouts = GraphLayout.entries,
        selectedLayout = GraphLayout.ORGANIC,
        selfIncluded = false,
        currencyOptions = Currency.entries,
        chartUrl = chartUrl,
    )
}

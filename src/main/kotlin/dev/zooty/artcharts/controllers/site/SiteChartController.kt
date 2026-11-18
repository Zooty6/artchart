package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.ChartViewRequest
import dev.zooty.artcharts.services.site.ChartViewService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/site/charts")
class SiteChartController(private val chartViewService: ChartViewService) {
    companion object {
        private const val VIEW_CHART_INDEX = "site/charts/index"
        private const val VIEW_CHART_BROWSER = "site/fragments/chart-browser"
    }

    @GetMapping
    fun charts(
        @ModelAttribute request: ChartViewRequest,
        model: Model,
    ): String {
        model.addAttribute(
            "chartModel",
            chartViewService.createModel(request)
        )
        return VIEW_CHART_INDEX
    }

    @GetMapping("/view")
    fun chartView(
        @ModelAttribute request: ChartViewRequest,
        model: Model,
    ): String {
        model.addAttribute(
            "chartModel",
            chartViewService.createModel(request)
        )
        return VIEW_CHART_BROWSER
    }
}

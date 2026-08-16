package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.services.ChartType
import dev.zooty.artcharts.services.GraphLayout
import dev.zooty.artcharts.services.site.ChartViewService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/site/charts")
class SiteChartController(private val chartViewService: ChartViewService) {
    companion object {
        private const val VIEW_CHART_INDEX = "site/charts/index"
        private const val VIEW_CHART_BROWSER = "site/fragments/chart-browser"
        private const val DEFAULT_CHART = "artistDistribution"
    }

    @GetMapping
    fun charts(
        @RequestParam(required = false, defaultValue = DEFAULT_CHART) chart: String,
        @RequestParam(required = false) width: Int?,
        @RequestParam(required = false) height: Int?,
        @RequestParam("filterList", required = false) filterList: List<String>?,
        @RequestParam(required = false) type: ChartType?,
        @RequestParam(required = false) layout: GraphLayout?,
        @RequestParam(required = false) selfIncluded: Boolean?,
        model: Model,
    ): String {
        model.addAttribute(
            "chartModel",
            chartViewService.createModel(chart, width, height, filterList, type, layout, selfIncluded)
        )
        return VIEW_CHART_INDEX
    }

    @GetMapping("/view")
    fun chartView(
        @RequestParam chart: String,
        @RequestParam(required = false) width: Int?,
        @RequestParam(required = false) height: Int?,
        @RequestParam("filterList", required = false) filterList: List<String>?,
        @RequestParam(required = false) type: ChartType?,
        @RequestParam(required = false) layout: GraphLayout?,
        @RequestParam(required = false) selfIncluded: Boolean?,
        model: Model,
    ): String {
        model.addAttribute(
            "chartModel",
            chartViewService.createModel(chart, width, height, filterList, type, layout, selfIncluded)
        )
        return VIEW_CHART_BROWSER
    }
}

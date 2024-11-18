package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.services.ChartService
import dev.zooty.artcharts.services.GraphLayout
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartController(private val chartService: ChartService) {
    @GetMapping("/chart/artistDistribution", produces = ["image/svg+xml"])
    fun artistDistribution(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.artistDistribution(width, height)
    }

    @GetMapping("/chart/currencyDistribution", produces = ["image/svg+xml"])
    fun currencyDistribution(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?, @RequestParam("filterList") filterList: List<String>?): String {
        return chartService.currencyDistribution(width, height, filterList ?: listOf())
    }

    @GetMapping("/chart/spendOverTime", produces = ["image/svg+xml"])
    fun spendOverTime(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.spendOverTime(width, height)
    }

    @GetMapping("/chart/nsfwRatio", produces = ["image/svg+xml"])
    fun nsfwRatio(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.nsfwRatio(width, height)
    }

    @GetMapping("/chart/speciesDistribution", produces = ["image/svg+xml"])
    fun speciesDistribution(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.speciesDistribution(width, height)
    }

    @GetMapping("/chart/characterGraph", produces = ["image/svg+xml"])
    fun characterGraph(@RequestParam("layout") graphLayout: GraphLayout?): String {
        return chartService.characterGraph(graphLayout ?: GraphLayout.ORGANIC )
    }
}
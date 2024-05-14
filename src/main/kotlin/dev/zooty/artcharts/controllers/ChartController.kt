package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.services.ChartService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartController(private val chartService: ChartService) {
    @GetMapping("/chart/artistDistribution", produces = ["image/svg+xml"])
    fun artistDistribution(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.artistDistribution(width, height)
    }

    @GetMapping("/chart/spendYearly", produces = ["image/svg+xml"])
    fun spendYearly(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?, @RequestParam("filterList") filterList: List<String>?): String {
        return chartService.spendYearly(width, height, filterList ?: listOf())
    }

    @GetMapping("/chart/spendOverTime", produces = ["image/svg+xml"])
    fun spendOverTime(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String {
        return chartService.spendOverTime(width, height)
    }
}
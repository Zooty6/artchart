package dev.zooty.artcharts.persistence.controllers

import dev.zooty.artcharts.persistence.services.ChartService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartController(private val chartService: ChartService) {
    @GetMapping("/chart/artistDistribution", produces = ["image/svg+xml"])
    fun artistDistribution(): String {
        return chartService.artistDistribution()
    }
}
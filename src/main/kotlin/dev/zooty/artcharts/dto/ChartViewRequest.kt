package dev.zooty.artcharts.dto

import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.GraphLayout

data class ChartViewRequest(
    val chart: String = "artistDistribution",
    val width: Int? = null,
    val height: Int? = null,
    val filterList: List<String>? = null,
    val type: ChartType? = null,
    val category: String? = null,
    val layout: GraphLayout? = null,
    val selfIncluded: Boolean? = null,
    val year: String? = null,
)

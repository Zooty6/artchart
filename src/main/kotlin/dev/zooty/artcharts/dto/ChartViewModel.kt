package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.GraphLayout

data class ChartViewModel(
    val chart: String,
    val chartOptions: List<String>,
    val width: Int,
    val height: Int,
    val filterList: List<String>,
    val chartTypes: List<ChartType>,
    val selectedType: ChartType,
    val categoryFilter: String?,
    val graphLayouts: List<GraphLayout>,
    val selectedLayout: GraphLayout,
    val selfIncluded: Boolean,
    val currencyOptions: List<Currency>,
    val chartUrl: String,
)

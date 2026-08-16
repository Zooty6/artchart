package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.dto.ChartViewModel
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.ChartType
import dev.zooty.artcharts.services.GraphLayout
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class ChartViewService {
    companion object {
        private const val DEFAULT_CHART = "artistDistribution"
        private const val DEFAULT_WIDTH = 1800
        private const val DEFAULT_HEIGHT = 900
        private const val MAX_CHART_DIMENSION = 5000
        private const val CHART_ARTIST_DISTRIBUTION = "artistDistribution"
        private const val CHART_CURRENCY_DISTRIBUTION = "currencyDistribution"
        private const val CHART_SPEND_OVER_TIME = "spendOverTime"
        private const val CHART_NSFW_RATIO = "nsfwRatio"
        private const val CHART_SPECIES_DISTRIBUTION = "speciesDistribution"
        private const val CHART_CHARACTER_GRAPH = "characterGraph"
    }

    private val chartOptions = listOf(
        CHART_ARTIST_DISTRIBUTION,
        CHART_CURRENCY_DISTRIBUTION,
        CHART_SPEND_OVER_TIME,
        CHART_NSFW_RATIO,
        CHART_SPECIES_DISTRIBUTION,
        CHART_CHARACTER_GRAPH,
    )

    fun createModel(
        chart: String,
        width: Int?,
        height: Int?,
        filterList: List<String>?,
        type: ChartType?,
        layout: GraphLayout?,
        selfIncluded: Boolean?,
    ): ChartViewModel {
        val selectedChart = chartOptions.firstOrNull { it == chart } ?: DEFAULT_CHART
        val selectedWidth = normalizeDimension(width, DEFAULT_WIDTH)
        val selectedHeight = normalizeDimension(height, DEFAULT_HEIGHT)
        val currencyOptions = Currency.entries.filter { it != Currency.Gift && it != Currency.UNKNOWN }
        val selectedFilters = filterList.orEmpty().filter { value -> currencyOptions.any { it.name == value } }
        val selectedType = type ?: ChartType.PIE
        val selectedLayout = layout ?: GraphLayout.ORGANIC
        val selectedSelfIncluded = selfIncluded ?: false

        return ChartViewModel(
            chart = selectedChart,
            chartOptions = chartOptions,
            width = selectedWidth,
            height = selectedHeight,
            filterList = selectedFilters,
            chartTypes = ChartType.entries,
            selectedType = selectedType,
            graphLayouts = GraphLayout.entries,
            selectedLayout = selectedLayout,
            selfIncluded = selectedSelfIncluded,
            currencyOptions = currencyOptions,
            chartUrl = createChartUrl(
                selectedChart,
                selectedWidth,
                selectedHeight,
                selectedFilters,
                selectedType,
                selectedLayout,
                selectedSelfIncluded,
            ),
        )
    }

    private fun normalizeDimension(value: Int?, default: Int): Int =
        value?.takeIf { it in 1..MAX_CHART_DIMENSION } ?: default

    private fun createChartUrl(
        chart: String,
        width: Int,
        height: Int,
        filterList: List<String>,
        type: ChartType,
        layout: GraphLayout,
        selfIncluded: Boolean,
    ): String {
        val endpoint = when (chart) {
            CHART_CURRENCY_DISTRIBUTION -> "/chart/$CHART_CURRENCY_DISTRIBUTION"
            CHART_SPEND_OVER_TIME -> "/chart/$CHART_SPEND_OVER_TIME"
            CHART_NSFW_RATIO -> "/chart/$CHART_NSFW_RATIO"
            CHART_SPECIES_DISTRIBUTION -> "/chart/$CHART_SPECIES_DISTRIBUTION"
            CHART_CHARACTER_GRAPH -> "/chart/$CHART_CHARACTER_GRAPH"
            else -> "/chart/$CHART_ARTIST_DISTRIBUTION"
        }
        val builder = UriComponentsBuilder.fromPath(endpoint)
            .queryParam("width", width)
            .queryParam("height", height)

        when (chart) {
            CHART_CURRENCY_DISTRIBUTION -> filterList.forEach { builder.queryParam("filterList", it) }
            CHART_SPECIES_DISTRIBUTION -> builder.queryParam("type", type.name)
            CHART_CHARACTER_GRAPH -> builder
                .queryParam("layout", layout.name)
                .queryParam("selfIncluded", selfIncluded)
        }
        return builder.build().encode().toUriString()
    }
}

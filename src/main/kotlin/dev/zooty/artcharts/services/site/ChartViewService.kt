package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.dto.ChartViewModel
import dev.zooty.artcharts.dto.ChartViewRequest
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.TagService
import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.GraphLayout
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.time.Year

private const val DEFAULT_CHART = "artistDistribution"
private const val DEFAULT_WIDTH = 1800
private const val DEFAULT_HEIGHT = 900
private const val MAX_CHART_DIMENSION = 15000
private const val CHART_ARTIST_DISTRIBUTION = "artistDistribution"
private const val CHART_CURRENCY_DISTRIBUTION = "currencyDistribution"
private const val CHART_SPEND_OVER_TIME = "spendOverTime"
private const val CHART_NSFW_RATIO = "nsfwRatio"
private const val CHART_SPECIES_DISTRIBUTION = "speciesDistribution"
private const val CHART_TAG_DISTRIBUTION = "tagDistribution"
private const val CHART_CHARACTER_GRAPH = "characterGraph"
private const val CHART_DELIVERY = "delivery"

@Service
class ChartViewService(private val tagService: TagService) {

    private val chartLabels = mapOf(
        CHART_ARTIST_DISTRIBUTION to "artists",
        CHART_CURRENCY_DISTRIBUTION to "currencies",
        CHART_SPEND_OVER_TIME to "spending",
        CHART_NSFW_RATIO to "nsfw",
        CHART_SPECIES_DISTRIBUTION to "species",
        CHART_TAG_DISTRIBUTION to "tags",
        CHART_CHARACTER_GRAPH to "characters",
        CHART_DELIVERY to "delivery",
    )
    private val chartOptions = chartLabels.keys.toList()

    fun createModel(request: ChartViewRequest): ChartViewModel {
        val selectedChart = chartOptions.firstOrNull { it == request.chart } ?: DEFAULT_CHART
        val selectedWidth = normalizeDimension(request.width, DEFAULT_WIDTH)
        val selectedHeight = normalizeDimension(request.height, DEFAULT_HEIGHT)
        val currencyOptions = Currency.entries.filter { it != Currency.Gift && it != Currency.UNKNOWN }
        val selectedFilters = request.filterList.orEmpty().filter { value -> currencyOptions.any { it.name == value } }
        val selectedType = request.type ?: ChartType.PIE
        val selectedCategoryFilter = request.category?.takeIf { it.isNotBlank() }?.trim()
        val selectedLayout = request.layout ?: GraphLayout.ORGANIC
        val selectedSelfIncluded = request.selfIncluded ?: false
        val selectedYear = if (selectedChart == CHART_DELIVERY) normalizeYear(request.year) else null

        val model = ChartViewModel(
            chart = selectedChart,
            chartOptions = chartOptions,
            chartLabels = chartLabels,
            yearFilter = selectedYear,
            width = selectedWidth,
            height = selectedHeight,
            filterList = selectedFilters,
            chartTypes = ChartType.entries,
            selectedType = selectedType,
            categoryFilter = selectedCategoryFilter,
            categoryOptions = tagService.findAllDistinctCategories(),
            graphLayouts = GraphLayout.entries,
            selectedLayout = selectedLayout,
            selfIncluded = selectedSelfIncluded,
            currencyOptions = currencyOptions,
            chartUrl = "",
        )
        return model.copy(chartUrl = createChartUrl(model))
    }

    private fun normalizeDimension(value: Int?, default: Int): Int =
        value?.takeIf { it in 1..MAX_CHART_DIMENSION } ?: default

    private fun normalizeYear(value: String?): String? = when {
        value != null && value.isBlank() -> null
        else -> value?.toIntOrNull()?.takeIf { it in 1000..9999 }?.toString() ?: Year.now().toString()
    }

    private fun createChartUrl(model: ChartViewModel): String {
        val endpoint = when (model.chart) {
            CHART_CURRENCY_DISTRIBUTION -> "/chart/$CHART_CURRENCY_DISTRIBUTION"
            CHART_SPEND_OVER_TIME -> "/chart/$CHART_SPEND_OVER_TIME"
            CHART_NSFW_RATIO -> "/chart/$CHART_NSFW_RATIO"
            CHART_SPECIES_DISTRIBUTION -> "/chart/$CHART_SPECIES_DISTRIBUTION"
            CHART_TAG_DISTRIBUTION -> "/chart/$CHART_TAG_DISTRIBUTION"
            CHART_CHARACTER_GRAPH -> "/chart/$CHART_CHARACTER_GRAPH"
            CHART_DELIVERY -> "/chart/$CHART_DELIVERY"
            else -> "/chart/$CHART_ARTIST_DISTRIBUTION"
        }
        val builder = UriComponentsBuilder.fromPath(endpoint)
            .queryParam("width", model.width)
            .queryParam("height", model.height)

        when (model.chart) {
            CHART_DELIVERY -> model.yearFilter?.let { builder.queryParam("year", it) }
            CHART_CURRENCY_DISTRIBUTION -> model.filterList.forEach { builder.queryParam("filterList", it) }
            CHART_SPECIES_DISTRIBUTION -> builder.queryParam("type", model.selectedType.name)
            CHART_TAG_DISTRIBUTION -> builder
                .queryParam("type", model.selectedType.name)
                .apply { model.categoryFilter?.let { queryParam("category", it) } }
            CHART_CHARACTER_GRAPH -> builder
                .queryParam("layout", model.selectedLayout.name)
                .queryParam("selfIncluded", model.selfIncluded)
        }
        return builder.build().encode().toUriString()
    }
}

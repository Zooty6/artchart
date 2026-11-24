package dev.zooty.artcharts.mcp

import dev.zooty.artcharts.services.chart.ArtistDistributionService
import dev.zooty.artcharts.services.chart.CharacterGraphService
import dev.zooty.artcharts.services.chart.ChartType
import dev.zooty.artcharts.services.chart.DeliveryTimeService
import dev.zooty.artcharts.services.chart.GraphLayout
import dev.zooty.artcharts.services.chart.NsfwService
import dev.zooty.artcharts.services.chart.SpeciesDistributionService
import dev.zooty.artcharts.services.chart.SpendingService
import dev.zooty.artcharts.services.chart.TagDistributionService
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.jfree.data.category.CategoryDataset
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Year

@Component
@ConditionalOnProperty("spring.ai.mcp.server.enabled", havingValue = "true")
internal class McpCharts(
    private val artists: ArtistDistributionService,
    private val spending: SpendingService,
    private val nsfw: NsfwService,
    private val species: SpeciesDistributionService,
    private val tags: TagDistributionService,
    private val characters: CharacterGraphService,
    private val delivery: DeliveryTimeService,
    private val links: McpPublicLinks,
) {
    data class Dimensions(@field:Positive val width: Int = DEFAULT_WIDTH, @field:Positive val height: Int = DEFAULT_HEIGHT)
    data class CurrencyDistribution(val filterList: List<String> = emptyList())
    data class SpeciesDistribution(val type: ChartType = ChartType.PIE)
    data class TagDistribution(val type: ChartType = ChartType.PIE, val category: String? = null)
    data class CharacterGraph(val layout: GraphLayout = GraphLayout.ORGANIC, val selfIncluded: Boolean = false)
    data class Delivery(@field:Pattern(regexp = "[0-9]{4}") val year: String? = null)

    data class Result(val svgUrl: String, val data: Any)
    data class SpendingPoint(val year: Int, val usd: Number, val currency: String?)

    fun artistDistribution(input: Dimensions) = result("artistDistribution", mapOf("width" to input.width, "height" to input.height), artists.artistCounts())

    fun currencyDistribution(input: CurrencyDistribution) = result(
        "currencyDistribution",
        mapOf("width" to DEFAULT_WIDTH, "height" to DEFAULT_HEIGHT, "filterList" to input.filterList),
        points(spending.createCurrencyDistributionDataset(input.filterList), true),
    )

    fun spendOverTime(input: Dimensions) = result(
        "spendOverTime", mapOf("width" to input.width, "height" to input.height),
        points(spending.createYearlySpendDataset(), false),
    )

    fun nsfwRatio(input: Dimensions) = result("nsfwRatio", mapOf("width" to input.width, "height" to input.height), nsfw.visibilityCounts())

    fun speciesDistribution(input: SpeciesDistribution) = result(
        "speciesDistribution", mapOf("width" to DEFAULT_WIDTH, "height" to DEFAULT_HEIGHT, "type" to input.type),
        species.speciesCounts(),
    )

    fun tagDistribution(input: TagDistribution) = result(
        "tagDistribution", mapOf("width" to DEFAULT_WIDTH, "height" to DEFAULT_HEIGHT, "type" to input.type, "category" to input.category),
        tags.tagCounts(input.category),
    )

    fun characterGraph(input: CharacterGraph) = result(
        "characterGraph", mapOf("width" to DEFAULT_WIDTH, "height" to DEFAULT_HEIGHT, "layout" to input.layout, "selfIncluded" to input.selfIncluded),
        characters.characterConnections(input.selfIncluded),
    )

    fun delivery(input: Delivery) = result(
        "delivery", mapOf("width" to DEFAULT_WIDTH, "height" to DEFAULT_HEIGHT, "year" to input.year),
        delivery.deliveryTimes(input.year?.let(Year::parse)),
    )

    private fun result(path: String, query: Map<String, Any?>, data: Any) =
        Result(links.url("/chart/$path", query), data)

    private fun points(dataset: CategoryDataset, byCurrency: Boolean): List<SpendingPoint> =
        (0 until dataset.columnCount).flatMap { column ->
            val year = dataset.getColumnKey(column).toString().toInt()
            (0 until dataset.rowCount).mapNotNull { row ->
                val currency = if (byCurrency) dataset.getRowKey(row).toString() else null
                dataset.getValue(row, column)?.let { SpendingPoint(year, it, currency) }
            }
        }

    companion object {
        const val DEFAULT_WIDTH = 1800
        const val DEFAULT_HEIGHT = 900
    }
}

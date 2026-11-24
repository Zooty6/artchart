package dev.zooty.artcharts.mcp

import dev.zooty.artcharts.services.chart.*
import jakarta.validation.constraints.Pattern
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
    enum class Chart(val path: String) {
        ARTIST_DISTRIBUTION("artistDistribution"),
        CURRENCY_DISTRIBUTION("currencyDistribution"),
        SPEND_OVER_TIME("spendOverTime"),
        NSFW_RATIO("nsfwRatio"),
        SPECIES_DISTRIBUTION("speciesDistribution"),
        TAG_DISTRIBUTION("tagDistribution"),
        CHARACTER_GRAPH("characterGraph"),
        DELIVERY("delivery"),
    }

    data class Query(
        val chart: Chart,
        val filterList: List<String> = emptyList(),
        val type: ChartType? = null,
        val category: String? = null,
        val layout: GraphLayout? = null,
        val selfIncluded: Boolean? = null,
        @field:Pattern(regexp = "[0-9]{4}") val year: String? = null,
    )

    data class Result(val chart: Chart, val svgUrl: String, val data: Any)
    data class SpendingPoint(val year: Int, val usd: Number, val currency: String?)

    fun query(input: Query): Result {
        val (data, query) = when (input.chart) {
            Chart.ARTIST_DISTRIBUTION -> artists.artistCounts() to emptyMap()
            Chart.CURRENCY_DISTRIBUTION -> points(spending.createCurrencyDistributionDataset(input.filterList), true) to
                mapOf("filterList" to input.filterList)
            Chart.SPEND_OVER_TIME -> points(spending.createYearlySpendDataset(), false) to emptyMap()
            Chart.NSFW_RATIO -> nsfw.visibilityCounts() to emptyMap()
            Chart.SPECIES_DISTRIBUTION -> species.speciesCounts() to mapOf("type" to input.type)
            Chart.TAG_DISTRIBUTION -> tags.tagCounts(input.category) to mapOf("type" to input.type, "category" to input.category)
            Chart.CHARACTER_GRAPH -> characters.characterConnections(input.selfIncluded ?: false) to
                mapOf("layout" to input.layout, "selfIncluded" to input.selfIncluded)
            Chart.DELIVERY -> delivery.deliveryTimes(input.year?.let(Year::parse)) to mapOf("year" to input.year)
        }
        return Result(input.chart, links.url("/chart/${input.chart.path}", query), data)
    }

    private fun points(dataset: CategoryDataset, byCurrency: Boolean): List<SpendingPoint> =
        (0 until dataset.columnCount).flatMap { column ->
            val year = dataset.getColumnKey(column).toString().toInt()
            (0 until dataset.rowCount).mapNotNull { row ->
                val currency = if (byCurrency) dataset.getRowKey(row).toString() else null
                dataset.getValue(row, column)?.let { SpendingPoint(year, it, currency) }
            }
        }
}

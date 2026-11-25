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
import org.jfree.data.category.DefaultCategoryDataset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.Year

class McpChartsTest {
    private val artists = Mockito.mock(ArtistDistributionService::class.java)
    private val spending = Mockito.mock(SpendingService::class.java)
    private val nsfw = Mockito.mock(NsfwService::class.java)
    private val species = Mockito.mock(SpeciesDistributionService::class.java)
    private val tags = Mockito.mock(TagDistributionService::class.java)
    private val characters = Mockito.mock(CharacterGraphService::class.java)
    private val delivery = Mockito.mock(DeliveryTimeService::class.java)
    private val charts = McpCharts(artists, spending, nsfw, species, tags, characters, delivery, McpPublicLinks("https://charts.example"))

    @Test
    fun `artist chart uses requested dimensions and service data`() {
        val data = mapOf("A" to 3L)
        `when`(artists.artistCounts()).thenReturn(data)

        val result = charts.artistDistribution(McpCharts.Dimensions(640, 480))

        assertEquals("https://charts.example/chart/artistDistribution?width=640&height=480", result.svgUrl)
        assertEquals(data, result.data)
    }

    @Test
    fun `currency and yearly spend datasets become chart points`() {
        val currencyData = DefaultCategoryDataset().apply {
            addValue(12.5, "EUR", 2024)
            addValue(8, "USD", 2024)
            addValue(4, "EUR", 2025)
        }
        val yearlyData = DefaultCategoryDataset().apply {
            addValue(20.0, "", 2024)
            addValue(30, "", 2025)
        }
        `when`(spending.createCurrencyDistributionDataset(listOf("USD"))).thenReturn(currencyData)
        `when`(spending.createYearlySpendDataset()).thenReturn(yearlyData)

        val currency = charts.currencyDistribution(McpCharts.CurrencyDistribution(listOf("USD")))
        val spend = charts.spendOverTime(McpCharts.Dimensions(700, 350))

        assertEquals("https://charts.example/chart/currencyDistribution?width=1800&height=900&filterList=USD", currency.svgUrl)
        assertEquals(
            listOf(
                McpCharts.SpendingPoint(2024, 12.5, "EUR"),
                McpCharts.SpendingPoint(2024, 8, "USD"),
                McpCharts.SpendingPoint(2025, 4, "EUR"),
            ),
            currency.data,
        )
        assertEquals("https://charts.example/chart/spendOverTime?width=700&height=350", spend.svgUrl)
        assertEquals(listOf(McpCharts.SpendingPoint(2024, 20.0, null), McpCharts.SpendingPoint(2025, 30, null)), spend.data)
    }

    @Test
    fun `chart methods pass options and service data through`() {
        val nsfwData = mapOf("safe" to 2)
        val speciesData = mapOf("Canine" to 4)
        val tagData = mapOf("blue" to 3L)
        val characterData = listOf(CharacterGraphService.GraphConnection("Rex", "Sam", 2))
        val deliveryData = listOf(DeliveryTimeService.DeliveryTime(7, "2023-01-01", "2023-02-01"))
        `when`(nsfw.visibilityCounts()).thenReturn(nsfwData)
        `when`(species.speciesCounts()).thenReturn(speciesData)
        `when`(tags.tagCounts("color & style")).thenReturn(tagData)
        `when`(characters.characterConnections(true)).thenReturn(characterData)
        `when`(delivery.deliveryTimes(Year.of(2023))).thenReturn(deliveryData)

        assertEquals(nsfwData, charts.nsfwRatio(McpCharts.Dimensions()).data)
        val speciesResult = charts.speciesDistribution(McpCharts.SpeciesDistribution(ChartType.BAR))
        assertEquals("https://charts.example/chart/speciesDistribution?width=1800&height=900&type=BAR", speciesResult.svgUrl)
        assertEquals(speciesData, speciesResult.data)
        val tagResult = charts.tagDistribution(McpCharts.TagDistribution(ChartType.TREEMAP, "color & style"))
        assertEquals("https://charts.example/chart/tagDistribution?width=1800&height=900&type=TREEMAP&category=color%20%26%20style", tagResult.svgUrl)
        assertEquals(tagData, tagResult.data)
        val characterResult = charts.characterGraph(McpCharts.CharacterGraph(GraphLayout.LIST, true))
        assertEquals("https://charts.example/chart/characterGraph?width=1800&height=900&layout=LIST&selfIncluded=true", characterResult.svgUrl)
        assertEquals(characterData, characterResult.data)
        val deliveryResult = charts.delivery(McpCharts.Delivery("2023"))
        assertEquals("https://charts.example/chart/delivery?width=1800&height=900&year=2023", deliveryResult.svgUrl)
        assertEquals(deliveryData, deliveryResult.data)
        Mockito.verify(delivery).deliveryTimes(Year.of(2023))
    }

    @Test
    fun `delivery without year passes null`() {
        `when`(delivery.deliveryTimes(null)).thenReturn(emptyList())

        val result = charts.delivery(McpCharts.Delivery())

        assertEquals("https://charts.example/chart/delivery?width=1800&height=900", result.svgUrl)
        assertEquals(emptyList<DeliveryTimeService.DeliveryTime>(), result.data)
        Mockito.verify(delivery).deliveryTimes(null)
    }
}

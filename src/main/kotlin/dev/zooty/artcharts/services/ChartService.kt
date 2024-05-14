package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.model.Currency
import dev.zooty.artcharts.persistence.model.Price
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.springframework.stereotype.Service
import java.awt.geom.Rectangle2D

@Service
class ChartService(
    private val artRepository: ArtRepository,
    private val artistRepository: ArtistRepository,
    private val currencyService: CurrencyService
) {
    private val defaultWidth: Int = 1800
    private val defaultHeight: Int = 800

    fun artistDistribution(width: Int?, height: Int?): String {
        val chart = ChartFactory.createBarChart(
            null,
            "artists",
            "purchases",
            createArtistDistributionDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun spendYearly(width: Int?, height: Int?, filterList: List<String>): String {
        val chart = ChartFactory.createStackedBarChart(
            null,
            "year",
            "amount",
            createYearlySpentDataSet(filterList),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun spendOverTime(width: Int?, height: Int?): String {
        val chart = ChartFactory.createLineChart(
            null,
            "year",
            "spending (USD)",
            createYearlySpendDataSet(),
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        currencyService.convertCurrency(4, "HUF", "USD").toInt()
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    private fun createYearlySpendDataSet(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAll()
            .filter { it.price.currency != Currency.UNKNOWN && it.price.currency != Currency.Gift }
            .groupBy { getYear(it.deliveredDate!!) }
            .mapValues {
                it.value.map {
                    if (it.price.currency != Currency.USD) convertToUsd(it.price) else it.price.amount
                }.sum()
            }
            .forEach { dataset.addValue(it.value, "", it.key) }
        return dataset
    }

    private fun convertToUsd(price: Price): Int {
        return currencyService.convertCurrency(price.amount, price.currency.name, "USD").toInt()
    }

    private fun createYearlySpentDataSet(filterList: List<String>): DefaultCategoryDataset {
        val arts = artRepository.findAll()
        val dataset = DefaultCategoryDataset()
        arts.filter { it.price.currency != Currency.Gift && it.price.currency != Currency.UNKNOWN }
            .associate {
                val year = getYear(it.deliveredDate!!)
                year to arts
                    .asSequence()
                    .filter { art -> getYear(art.deliveredDate!!) == year }
                    .filter { art -> art.price.currency != Currency.UNKNOWN && art.price.currency != Currency.Gift }
                    .filter { art -> !filterList.contains(art.price.currency.name) }
                    .groupBy { art -> art.price.currency }
                    .map { entry -> entry.key to entry.value.sumOf { art -> art.price.amount } }
                    .toList()
                    .forEach { currencySumPair ->
                        dataset.addValue(
                            currencySumPair.second,
                            currencySumPair.first,
                            year.toString(),
                        )
                    }
            }
        return dataset
    }

    private fun getYear(date: String): Int {
        return date.split("-")[0].toInt()
    }

    private fun createArtistDistributionDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAllGroupByArtist()
            .filter { it[0] as String != "unknown" }
            .forEach { dataset.addValue(it[1] as Long, it[0] as String, "") }
        return dataset
    }

    private fun exportToSvg(width: Int, height: Int, chart: JFreeChart): String {
        val svgGraphics2D = SVGGraphics2D(width, height)
        chart.draw(svgGraphics2D, Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
        return svgGraphics2D.svgElement
    }
}

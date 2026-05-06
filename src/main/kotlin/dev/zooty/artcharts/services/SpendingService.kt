package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import org.springframework.stereotype.Service

@Service
class SpendingService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
    private val currencyService: CurrencyService,
) {
    fun currencyDistribution(width: Int, height: Int, filterList: List<String>): String {
        val chart = ChartFactory.createStackedBarChart(
            "Spending yearly distribution",
            "year",
            "amount (USD)",
            createCurrencyDistributionDataset(filterList),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        )
        return svgService.exportToSvg(width, height, chart)
    }

    fun spendOverTime(width: Int, height: Int): String {
        val chart = ChartFactory.createLineChart(
            "Spending",
            "year",
            "spending (USD)",
            createYearlySpendDataset(),
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        return svgService.exportToSvg(width, height, chart)
    }

    private fun createCurrencyDistributionDataset(filterList: List<String>): DefaultCategoryDataset {
        val arts = artRepository.findAll().toList()
        val dataset = DefaultCategoryDataset()
        arts.filter { it.price.currency != Currency.Gift && it.price.currency != Currency.UNKNOWN }
            .associate {
                val year = getYear(it.deliveredDate)
                year to arts
                    .asSequence()
                    .filter { art -> getYear(art.deliveredDate) == year }
                    .filter { art -> art.price.currency != Currency.UNKNOWN && art.price.currency != Currency.Gift }
                    .filter { art -> !filterList.contains(art.price.currency.name) }
                    .groupBy { art -> art.price.currency }
                    .map { entry -> entry.key to entry.value.sumOf { art -> art.price.amount } }
                    .toList()
                    .forEach { currencySumPair ->
                        dataset.addValue(
                            convertToUsd(currencySumPair.first.name, currencySumPair.second),
                            currencySumPair.first,
                            year.toString(),
                        )
                    }
            }
        return dataset
    }

    private fun createYearlySpendDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAll()
            .filter { it.price.currency != Currency.UNKNOWN && it.price.currency != Currency.Gift }
            .groupBy { getYear(it.deliveredDate) }
            .mapValues {
                it.value.sumOf { art ->
                    when {
                        art.price.currency == Currency.USD -> art.price.amount
                        else -> convertToUsd(art.price)
                    }
                }
            }
            .forEach { dataset.addValue(it.value, "", it.key) }
        return dataset
    }

    private fun getYear(date: String): Int {
        return date.split("-")[0].toInt()
    }

    private fun convertToUsd(price: Price): Double {
        return currencyService.convertCurrency(price.amount, price.currency.name, "USD")
    }

    private fun convertToUsd(fromCurrency: String, amount: Double): Int {
        return currencyService.convertCurrency(amount, fromCurrency, "USD").toInt()
    }
}
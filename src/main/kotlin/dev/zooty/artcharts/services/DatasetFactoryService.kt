package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.model.Currency
import dev.zooty.artcharts.persistence.model.Price
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.general.PieDataset
import org.springframework.stereotype.Service

@Service
class DatasetFactoryService(
    private val artRepository: ArtRepository,
    private val currencyService: CurrencyService
) {
    fun createSpeciesDistributionDataset(): PieDataset {
        val dataset = DefaultPieDataset()
        artRepository.findAll()
            .groupingBy { it.species.split(",")[0] }
            .eachCount()
            .forEach { (species, count) -> dataset.setValue("$species($count)", count) }
        return dataset
    }

    fun createNsfwRatioDataset(): PieDataset {
        val dataset = DefaultPieDataset()
        artRepository.findAll()
            .groupingBy { it.isNsfw }
            .eachCount()
            .forEach {
                when (it.key) {
                    true -> dataset.setValue("NSFW", it.value)
                    false -> dataset.setValue("SFW", it.value)
                }
            }
        return dataset
    }

    fun createCurrencyDistributionDataset(filterList: List<String>): DefaultCategoryDataset {
        val arts = artRepository.findAll()
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

    fun createArtistDistributionDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAllGroupByArtist()
            .filter { it[0] as String != "unknown" }
            .forEach { dataset.addValue(it[1] as Long, it[0] as String, "") }
        return dataset
    }

    fun createYearlySpendDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAll()
            .filter { it.price.currency != Currency.UNKNOWN && it.price.currency != Currency.Gift }
            .groupBy { getYear(it.deliveredDate) }
            .mapValues {
                it.value.sumOf { art ->
                    if (art.price.currency != Currency.USD) convertToUsd(art.price) else art.price.amount
                }
            }
            .forEach { dataset.addValue(it.value, "", it.key) }
        return dataset
    }

    private fun convertToUsd(price: Price): Double {
        return currencyService.convertCurrency(price.amount, price.currency.name, "USD")
    }

    private fun convertToUsd(fromCurrency: String, amount: Double): Int {
        return currencyService.convertCurrency(amount, fromCurrency, "USD").toInt()
    }

    private fun getYear(date: String): Int {
        return date.split("-")[0].toInt()
    }
}

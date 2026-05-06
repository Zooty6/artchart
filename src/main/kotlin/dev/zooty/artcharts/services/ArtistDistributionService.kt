package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import org.springframework.stereotype.Service

@Service
class ArtistDistributionService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
) {

    fun artistDistribution(width: Int, height: Int): String {
        val chart = ChartFactory.createBarChart(
            "Artist commission amounts",
            "artists",
            "purchases",
            createArtistDistributionDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return svgService.exportToSvg(width, height, chart)
    }

    private fun createArtistDistributionDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAllGroupByArtist()
            .filter { it[0] as String != "unknown" }
            .forEach { dataset.addValue(it[1] as Long, it[0] as String + "(${it[1]})", "") }
        return dataset
    }
}
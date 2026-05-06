package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import org.jfree.chart.ChartFactory
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.general.PieDataset
import org.springframework.stereotype.Service

@Service
class NsfwService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
) {

    fun nsfwRatio(width: Int, height: Int): String {
        val chart = ChartFactory.createPieChart(
            null,
            createNsfwRatioDataset(),
            true,
            true,
            false
        )
        return svgService.exportToSvg(width, height, chart)
    }

    private fun createNsfwRatioDataset(): PieDataset {
        val dataset = DefaultPieDataset()
        artRepository.findAll()
            .groupingBy { it.isNsfw }
            .eachCount()
            .forEach {
                when (it.key) {
                    true -> dataset.setValue("NSFW(${it.value})", it.value)
                    false -> dataset.setValue("SFW(${it.value})", it.value)
                }
            }
        return dataset
    }
}
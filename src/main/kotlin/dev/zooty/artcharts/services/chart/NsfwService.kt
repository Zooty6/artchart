package dev.zooty.artcharts.services.chart

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

    fun visibilityCounts(): Map<String, Int> = artRepository.findAll()
        .groupingBy { if (it.isNsfw) "NSFW" else "SFW" }
        .eachCount()

    private fun createNsfwRatioDataset(): PieDataset<String> {
        val dataset = DefaultPieDataset<String>()
        visibilityCounts().forEach { (label, count) -> dataset.setValue("$label($count)", count) }
        return dataset
    }
}

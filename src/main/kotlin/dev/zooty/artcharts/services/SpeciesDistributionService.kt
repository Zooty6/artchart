package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import org.jfree.chart.ChartFactory
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.general.PieDataset
import org.springframework.stereotype.Service

@Service
class SpeciesDistributionService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
    private val treeMapRendererService: TreeMapRendererService
) {

    fun speciesDistribution(width: Int, height: Int, type: ChartType): String {
        val dataset = createSpeciesDistributionDataset()

        return when (type) {
            ChartType.TREEMAP -> treeMapRendererService.renderTreemapSvg(width, height, dataset)
            ChartType.PIE -> svgService.exportToSvg(
                width,
                height,
                ChartFactory.createPieChart(
                    "Distribution of Species",
                    dataset,
                    true,
                    true,
                    false
                )
            )
        }
    }

    private fun createSpeciesDistributionDataset(): PieDataset {
        val dataset = DefaultPieDataset()
        artRepository.findAll()
            .groupingBy { it.species }
            .eachCount()
            .forEach { (species, count) -> dataset.setValue("$species($count)", count) }
        return dataset
    }
}
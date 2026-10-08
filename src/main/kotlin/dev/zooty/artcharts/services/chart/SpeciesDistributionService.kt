package dev.zooty.artcharts.services.chart

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
            ChartType.PIE -> {
                val chart = ChartFactory.createPieChart(
                    "Distribution of Species",
                    dataset,
                    true,
                    true,
                    false
                )
                svgService.exportToSvg(width, height, chart)
            }
        }
    }

    private fun createSpeciesDistributionDataset(): PieDataset<String> {
        val dataset = DefaultPieDataset<String>()
        artRepository.findAll()
            .groupingBy { it.species }
            .eachCount()
            .forEach { (species, count) -> dataset.setValue("$species($count)", count) }
        return dataset
    }
}

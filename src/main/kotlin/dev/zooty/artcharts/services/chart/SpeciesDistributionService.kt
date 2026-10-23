package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
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
        return when (type) {
            ChartType.TREEMAP -> treeMapRendererService.renderTreemapSvg(width, height, createSpeciesDistributionDataset())
            ChartType.PIE -> createPieChart(width, height)
            ChartType.BAR -> createBarChart(width, height)
        }
    }

    private fun createBarChart(width: Int, height: Int): String {
        val chart = ChartFactory.createBarChart(
            "Species distribution",
            "Species",
            "Amount",
            createSpeciesDistributionCategoryDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return svgService.exportToSvg(width, height, chart)
    }
    
    private fun createPieChart(width: Int, height: Int): String {
        val chart = ChartFactory.createPieChart(
            "Distribution of Species",
            createSpeciesDistributionDataset(),
            true,
            true,
            false
        )
        return svgService.exportToSvg(width, height, chart)
    }
    
    private fun createSpeciesDistributionCategoryDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAll()
            .groupingBy { it.species }
            .eachCount()
            .forEach { (species, count) -> dataset.addValue(count.toLong(), species, "") }
        return dataset
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

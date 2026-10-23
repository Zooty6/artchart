package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import jakarta.transaction.Transactional
import org.jfree.chart.ChartFactory
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.general.PieDataset
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class TagDistributionService(
    private val svgConverterService: SvgConverterService,
    private val artRepository: ArtRepository,
    private val treeMapRendererService: TreeMapRendererService
) {
    @Transactional
    fun tagDistribution(width: Int, height: Int, chartType: ChartType, categoryFilter: String?): String {
        return when(chartType) {
            ChartType.TREEMAP -> treeMapRendererService.renderTreemapSvg(width, height, createDataset(categoryFilter))
            ChartType.PIE -> createPieChart(width, height, categoryFilter)
            ChartType.BAR -> createBarChart(width, height, categoryFilter)
        }
    }

    private fun createPieChart(width: Int, height: Int, categoryFilter: String?): String {
        val chart = ChartFactory.createPieChart(
            "Distribution of ${categoryFilter ?: ""} tags",
            createDataset(categoryFilter),
            true,
            true,
            false
        )
        return svgConverterService.exportToSvg(width, height, chart) 
    }

    private fun createBarChart(width: Int, height: Int, categoryFilter: String?): String {
        val chart = ChartFactory.createBarChart(
            "Distribution of ${categoryFilter ?: ""} tags",
            "Tags",
            "Amount",
            createCategoryDataset(categoryFilter),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return svgConverterService.exportToSvg(width, height, chart)
    }

    private fun createCategoryDataset(categoryFilter: String?): CategoryDataset {
        val dataset = DefaultCategoryDataset()
        artRepository.findAllBy()
            .flatMap { it.tags.stream() }
            .filter { tag -> categoryFilter?.equals(tag.category) ?: true }
            .collect(Collectors.groupingByConcurrent({ tag -> tag.name }, Collectors.counting()))
            .forEach { (tagName, counts) -> dataset.addValue(counts.toLong(), tagName, "") }
        return dataset
    }

    private fun createDataset(categoryFilter: String?): PieDataset<String> {
        val dataset = DefaultPieDataset<String>()
        artRepository.findAllBy()
            .flatMap { it.tags.stream() }
            .filter { tag -> categoryFilter?.equals(tag.category) ?: true }
            .collect(Collectors.groupingByConcurrent({ tag -> tag.name }, Collectors.counting()))
            .forEach { (tagName, counts) -> dataset.setValue(tagName, counts) }
        return dataset
    }
}

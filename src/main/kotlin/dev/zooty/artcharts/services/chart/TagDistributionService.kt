package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import jakarta.transaction.Transactional
import org.jfree.chart.ChartFactory
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
        val dataset = createDataset(categoryFilter)
        
        return when(chartType) {
            ChartType.TREEMAP -> treeMapRendererService.renderTreemapSvg(width, height, dataset)
            ChartType.PIE -> svgConverterService.exportToSvg(
                width,
                height,
                ChartFactory.createPieChart(
                    "Distribution of ${categoryFilter ?: ""} tags",
                    dataset,
                    true,
                    true,
                    false
                )
            )
        }
    }


    private fun createDataset(categoryFilter: String?): PieDataset {
        val dataset = DefaultPieDataset()
        artRepository.findAllBy()
            .flatMap { it.tags.stream() }
            .filter { tag -> categoryFilter?.equals(tag.category) ?: true }
            .collect(Collectors.groupingByConcurrent({ tag -> tag.name }, Collectors.counting()))
            .forEach { (tagName, counts) -> dataset.setValue(tagName, counts) }
        return dataset
    }
}
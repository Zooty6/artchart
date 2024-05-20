package dev.zooty.artcharts.services

import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.springframework.stereotype.Service
import java.awt.geom.Rectangle2D

@Service
class ChartService(
    private val datasetFactoryService: DatasetFactoryService
) {
    private val defaultWidth: Int = 1800
    private val defaultHeight: Int = 900

    fun artistDistribution(width: Int?, height: Int?): String {
        val chart = ChartFactory.createBarChart(
            "Artist commission amounts",
            "artists",
            "purchases",
            datasetFactoryService.createArtistDistributionDataset(),
            PlotOrientation.VERTICAL,
            true,
            true,
            true
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun currencyDistribution(width: Int?, height: Int?, filterList: List<String>): String {
        val chart = ChartFactory.createStackedBarChart(
            "Spending yearly distribution",
            "year",
            "amount (USD)",
            datasetFactoryService.createCurrencyDistributionDataset(filterList),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun spendOverTime(width: Int?, height: Int?): String {
        val chart = ChartFactory.createLineChart(
            "Spending",
            "year",
            "spending (USD)",
            datasetFactoryService.createYearlySpendDataset(),
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun nsfwRatio(width: Int?, height: Int?): String {
        val chart = ChartFactory.createPieChart(
            null,
            datasetFactoryService.createNsfwRatioDataset(),
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    fun speciesDistribution(width: Int?, height: Int?): String {
        val chart = ChartFactory.createPieChart(
            "Distribution of Species",
            datasetFactoryService.createSpeciesDistributionDataset(),
            true,
            true,
            false
        )
        return exportToSvg(width ?: defaultWidth, height ?: defaultHeight, chart)
    }

    private fun exportToSvg(width: Int, height: Int, chart: JFreeChart): String {
        val svgGraphics2D = SVGGraphics2D(width, height)
        chart.draw(svgGraphics2D, Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
        return svgGraphics2D.svgElement
    }
}

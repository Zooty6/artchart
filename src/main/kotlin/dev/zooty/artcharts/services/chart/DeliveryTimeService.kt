package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.SymbolAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.geom.Ellipse2D
import java.time.LocalDate
import java.time.Year
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.*

@Service
class DeliveryTimeService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
) {
    fun deliveryTimeGraph(width: Int, height: Int, yearFilter: String?): String {
        val year = yearFilter?.takeIf { it.isNotBlank() }?.let(Year::parse)
        val deliveries = artRepository.findAll()
            .mapNotNull(::deliveryPeriod)
            .filter { year == null || it.ordered.year == year.value }
            .sortedWith(compareBy({ it.ordered }, { it.art.id }))
        val dataset = XYSeriesCollection()
        deliveries.forEachIndexed { index, delivery ->
            val series = XYSeries(index)
            series.add(delivery.ordered.toEpochMillis(), index.toDouble())
            series.add(delivery.delivered.toEpochMillis(), index.toDouble())
            dataset.addSeries(series)
        }

        val timeline = createTimeline(deliveries, year)
        val labels = deliveries.map { "${it.art.artist.name} / ${it.art.fileName} (#${it.art.id})" }
        val rows = SymbolAxis("Artwork", labels.toTypedArray()).apply {
            isGridBandsVisible = false
            isInverted = true
            setRange(-0.5, maxOf(deliveries.size - 0.5, 0.5))
        }
        val renderer = XYLineAndShapeRenderer(true, true).apply {
            defaultStroke = BasicStroke(2.0f)
            defaultShape = Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0)
        }
        val plot = XYPlot(dataset, timeline, rows, renderer).apply {
            isRangeGridlinesVisible = false
            noDataMessage = "No completed orders with full dates"
        }
        val chart = JFreeChart("Delivery time", JFreeChart.DEFAULT_TITLE_FONT, plot, false)
        return svgService.exportToSvg(width, height, chart)
    }

    private fun deliveryPeriod(art: Art): DeliveryPeriod? {
        val ordered = parseDate(art.orderedDate) ?: return null
        val delivered = parseDate(art.deliveredDate) ?: return null
        return if (delivered.isBefore(ordered)) null else DeliveryPeriod(art, ordered, delivered)
    }

    private fun parseDate(value: String?): LocalDate? = try {
        value?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun createTimeline(deliveries: List<DeliveryPeriod>, year: Year?): DateAxis {
        val start = year?.atDay(1) ?: deliveries.minOfOrNull { it.ordered } ?: Year.now().atDay(1)
        val end = maxOf(
            year?.plusYears(1)?.atDay(1) ?: start.plusDays(1),
            deliveries.maxOfOrNull { it.delivered } ?: start.plusDays(1),
        )
        return DateAxis("Date", TimeZone.getTimeZone("UTC"), Locale.getDefault()).apply {
            setRange(start.toEpochMillis(), end.toEpochMillis())
        }
    }

    private fun LocalDate.toEpochMillis(): Double = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli().toDouble()

    private data class DeliveryPeriod(val art: Art, val ordered: LocalDate, val delivered: LocalDate)
}

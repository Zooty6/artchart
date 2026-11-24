package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Art
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
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

    private companion object {
        val COLOR_PALETTE: List<Color> = listOf(
            Color(0x1F4E79), // navy blue
            Color(0x8B1E3F), // burgundy
            Color(0x25623C), // forest green
            Color(0x5B2C83), // purple
            Color(0xA65000), // burnt orange
            Color(0x006064), // teal
            Color(0x6D3B1F), // brown
            Color(0x8A246B), // dark magenta
        )
    }
    
    fun deliveryTimeGraph(width: Int, height: Int, yearFilter: String?): String {
        val year = yearFilter?.takeIf { it.isNotBlank() }?.let(Year::parse)
        val deliveries = findDeliveryPeriods(year)
        val chart = createChart(deliveries, year)
        return svgService.exportToSvg(width, height, chart)
    }

    fun deliveryTimes(year: Year?): List<DeliveryTime> = findDeliveryPeriods(year).map {
        DeliveryTime(it.art.id, it.ordered.toString(), it.delivered.toString())
    }

    data class DeliveryTime(val artId: Long, val orderedDate: String, val deliveredDate: String)

    private fun findDeliveryPeriods(year: Year?): List<DeliveryPeriod> =
        artRepository.findAll()
            .mapNotNull(::deliveryPeriod)
            .filter { year == null || it.ordered.year == year.value }
            .sortedWith(compareBy({ it.ordered }, { it.art.id }))

    private fun createChart(deliveries: List<DeliveryPeriod>, year: Year?): JFreeChart {
        val colors = deliveries.indices.map { COLOR_PALETTE[it % COLOR_PALETTE.size] }
        val plot = XYPlot(
            createDataset(deliveries),
            createTimeline(deliveries, year),
            createArtworkAxis(deliveries, colors),
            createRenderer(colors),
        ).apply {
            isRangeGridlinesVisible = false
            noDataMessage = "No completed orders with full dates"
        }
        val title = "Delivery time" + (year?.let { " (${it.value})" } ?: "")
        return JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false)
    }

    private fun createDataset(deliveries: List<DeliveryPeriod>): XYSeriesCollection {
        val dataset = XYSeriesCollection()
        deliveries.forEachIndexed { index, delivery ->
            val series = XYSeries(index)
            series.add(delivery.ordered.toEpochMillis(), index.toDouble())
            series.add(delivery.delivered.toEpochMillis(), index.toDouble())
            dataset.addSeries(series)
        }
        return dataset
    }

    private fun createArtworkAxis(deliveries: List<DeliveryPeriod>, colors: List<Color>): ColoredSymbolAxis {
        val labels = deliveries.map { "${it.art.artist.name} / ${it.art.fileName} (#${it.art.id})" }
        return ColoredSymbolAxis("Artwork", labels.toTypedArray(), colors).apply {
            tickLabelFont = tickLabelFont.deriveFont(14.0f)
            isGridBandsVisible = false
            isInverted = true
            setRange(-0.5, maxOf(deliveries.size - 0.5, 0.5))
        }
    }

    private fun createRenderer(colors: List<Color>): XYLineAndShapeRenderer =
        XYLineAndShapeRenderer(true, true).apply {
            defaultStroke = BasicStroke(2.0f)
            defaultShape = Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0)
            colors.forEachIndexed { index, color -> setSeriesPaint(index, color) }
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

package dev.zooty.artcharts.services.chart

import org.jfree.chart.axis.AxisState
import org.jfree.chart.axis.SymbolAxis
import org.jfree.chart.axis.ValueTick
import org.jfree.chart.text.TextUtils
import org.jfree.chart.ui.RectangleEdge
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.geom.Rectangle2D

/** A symbol axis whose tick labels use the corresponding series colors. */
internal class ColoredSymbolAxis(
    label: String,
    symbols: Array<String>,
    private val colors: List<Paint>,
) : SymbolAxis(label, symbols) {
    init {
        isTickMarksVisible = false
    }

    override fun drawTickMarksAndLabels(
        graphics: Graphics2D,
        cursor: Double,
        plotArea: Rectangle2D,
        dataArea: Rectangle2D,
        edge: RectangleEdge,
    ): AxisState {
        val state = AxisState(cursor)
        if (isAxisLineVisible) {
            drawAxisLine(graphics, cursor, dataArea, edge)
        }
        val ticks = refreshTicks(graphics, state, dataArea, edge).filterIsInstance<ValueTick>()
        state.ticks = ticks
        if (!isTickLabelsVisible) {
            return state
        }
        graphics.font = tickLabelFont
        ticks.forEach { tick ->
            graphics.paint = colors.getOrNull(tick.value.toInt()) ?: tickLabelPaint
            val anchor = calculateAnchorPoint(tick, cursor, dataArea, edge)
            TextUtils.drawRotatedString(
                tick.text, graphics, anchor[0], anchor[1], tick.textAnchor, tick.angle, tick.rotationAnchor,
            )
        }
        val used = if (RectangleEdge.isLeftOrRight(edge)) {
            findMaximumTickLabelWidth(ticks, graphics, plotArea, isVerticalTickLabels)
        } else {
            findMaximumTickLabelHeight(ticks, graphics, plotArea, isVerticalTickLabels)
        }
        state.moveCursor(used, edge)
        return state
    }
}

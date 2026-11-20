package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Density band chart (#219): per time window the share of values per value interval, colour green (rare) → yellow → red
 * (frequent) with growing opacity so the intensity is also readable without hue. Windows without values stay transparent;
 * nothing is interpolated or smoothed. Tap reports the window.
 */
internal class HistoryDensityChartView(
    context: Context,
    private val density: HistoryDensity,
    private val axis: AxisScale,
    private val zone: ZoneId,
    private val onColumn: (Int) -> Unit
) : View(context) {
    private val palette = AppPalette.of(context)
    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * d }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f * d }
    private val rect = RectF()
    private val left = 34 * d; private val right get() = width - 6 * d
    private val top = 8 * d; private val bottom get() = height - 22 * d
    private val fmt = DateTimeFormatter.ofPattern("d.M. HH:mm", Locale.GERMANY)

    /** Umschaltbar: Ampel (rot/gelb/grün) oder farbsichere Skala. */
    var colorSafe: Boolean = false
        set(value) { field = value; invalidate() }

    init {
        tag = "history_density_chart"
        contentDescription = HistoryDensityAccessibility.summary(density, axis)
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), (210 * d).toInt())

    /** Rare = green, mid = yellow, frequent = red; opacity grows with the intensity (secondary, hue-independent cue). */
    internal fun colorFor(intensity: Double): Int {
        val i = intensity.coerceIn(0.0, 1.0)
        if (colorSafe) {
            // Farbsichere Skala (#248): blau (selten) → türkis → gelb (häufig), monoton in der Helligkeit, ohne Rot/Grün-Unterscheidung.
            val stops = intArrayOf(0x3B528B, 0x21A585, 0xFDE725)
            val pos = i * (stops.size - 1); val lo = pos.toInt().coerceAtMost(stops.size - 2); val t = (pos - lo).toFloat()
            fun mix(shift: Int) = (((stops[lo] shr shift) and 0xFF) * (1 - t) + ((stops[lo + 1] shr shift) and 0xFF) * t).toInt()
            return Color.argb((90 + 165 * i).toInt(), mix(16), mix(8), mix(0))
        }
        val hue = (120.0 * (1.0 - intensity.coerceIn(0.0, 1.0))).toFloat()
        val alpha = (70 + 185 * intensity.coerceIn(0.0, 1.0)).toInt()
        return Color.HSVToColor(alpha, floatArrayOf(hue, 0.78f, if (palette.dark) 0.95f else 0.82f))
    }

    private fun y(v: Int): Float = (bottom - (bottom - top) * ((v - axis.minimum).toFloat() / axis.span)).toFloat()

    override fun onDraw(canvas: Canvas) {
        val n = density.columns.size.takeIf { it > 0 } ?: return
        grid.color = palette.track; text.color = palette.muted
        val ticks = listOf(axis.minimum, (axis.minimum + axis.maximum) / 2, axis.maximum)
        text.textAlign = Paint.Align.RIGHT
        ticks.forEach { t -> canvas.drawLine(left, y(t), right, y(t), grid); canvas.drawText("$t", left - 4 * d, y(t) + 4 * d, text) }
        val w = (right - left) / n
        density.columns.forEachIndexed { i, col ->
            if (col.sampleCount == 0) return@forEachIndexed
            col.bins.forEach { bin ->
                if (bin.count == 0) return@forEach
                fill.color = colorFor(bin.intensity)
                rect.set(left + i * w, y(bin.upperBound), left + (i + 1) * w + 0.6f, y(bin.lowerInclusive))
                canvas.drawRect(rect, fill)
            }
        }
        text.textAlign = Paint.Align.LEFT; canvas.drawText(density.columns.first().start.atZone(zone).format(fmt), left, height - 4 * d, text)
        text.textAlign = Paint.Align.RIGHT; canvas.drawText(density.columns.last().end.atZone(zone).format(fmt), right, height - 4 * d, text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP && density.columns.isNotEmpty()) {
            val w = (right - left) / density.columns.size
            val i = ((e.x - left) / w).toInt()
            if (i in density.columns.indices) { performClick(); onColumn(i) }
        }
        return e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_UP
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}

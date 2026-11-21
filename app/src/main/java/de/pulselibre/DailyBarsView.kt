package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.time.LocalDate

/**
 * Trend drilldown (#41): one bar per local day for the last [days] complete days, oldest left. Days without data stay empty
 * (no zero bar). Dashed medians of the 7 and 28 day windows are drawn as horizontal lines.
 */
internal class DailyBarsView(
    context: Context,
    private val values: Map<LocalDate, Double>,
    private val today: LocalDate,
    private val days: Int,
    private val color: Int,
    private val ink: Int,
    private val track: Int,
    private val medians: List<Pair<String, Double?>>,
    /** false: the axis starts below the lowest value (bpm) so day-to-day differences stay visible; labelled "ab …". */
    private val zeroBased: Boolean = true
) : View(context) {
    private val d = resources.displayMetrics.density
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val series: List<Double?> = (days downTo 1).map { values[today.minusDays(it.toLong())]?.takeIf(Double::isFinite) }

    init {
        val present = series.filterNotNull()
        contentDescription = context.getString(R.string.tr_bars_cd, days.toString(), present.size.toString()) +
            (if (present.isNotEmpty()) context.getString(R.string.tr_bars_range, "%.0f".format(present.min()), "%.0f".format(present.max())) else "") + "."
    }

    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(MeasureSpec.getSize(w), (200 * d).toInt()) }

    override fun onDraw(canvas: Canvas) {
        val present = series.filterNotNull()
        val left = 8 * d; val right = width - 8 * d; val top = 14 * d; val bottom = height - 22 * d
        text.color = ink; text.textSize = 10 * d
        if (present.isEmpty()) { text.textAlign = Paint.Align.CENTER; canvas.drawText(context.getString(R.string.tr_bars_none), width / 2f, height / 2f, text); return }
        val hi = maxOf(present.max(), medians.mapNotNull { it.second }.maxOrNull() ?: 0.0)
        val lo = if (zeroBased) minOf(0.0, present.min()) else Math.floor(present.min() * 0.9 / 5.0) * 5.0
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        fun y(v: Double) = bottom - ((v - lo) / span).toFloat() * (bottom - top)
        line.color = track; line.strokeWidth = d; canvas.drawLine(left, bottom, right, bottom, line)
        val slot = (right - left) / days
        bar.color = color; bar.alpha = 200
        series.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed
            val x = left + i * slot
            rect.set(x + slot * 0.15f, y(v), x + slot * 0.85f, bottom)
            canvas.drawRoundRect(rect, slot * 0.2f, slot * 0.2f, bar)
        }
        medians.forEachIndexed { k, (label, m) ->
            if (m == null) return@forEachIndexed
            line.color = ink; line.strokeWidth = 1.2f * d; line.alpha = if (k == 0) 230 else 140
            line.pathEffect = android.graphics.DashPathEffect(floatArrayOf(6 * d, 4 * d), 0f)
            canvas.drawLine(left, y(m), right, y(m), line)
            line.pathEffect = null; line.alpha = 255
            // first label on the right above its line, second on the left below its line: they never overprint
            val txt = "$label ${"%.0f".format(context.displayLocale(), m)}"
            if (k == 0) { text.textAlign = Paint.Align.RIGHT; text.alpha = 230; canvas.drawText(txt, right, y(m) - 3 * d, text) }
            else { text.textAlign = Paint.Align.LEFT; text.alpha = 200; canvas.drawText(txt, left, y(m) + 12 * d, text) }
        }
        text.alpha = 160; text.textAlign = Paint.Align.LEFT
        canvas.drawText(if (zeroBased) context.getString(R.string.tr_bars_ago, days.toString()) else context.getString(R.string.tr_bars_ago_axis, days.toString(), "%.0f".format(lo)), left, height - 6 * d, text)
        text.textAlign = Paint.Align.RIGHT; canvas.drawText(context.getString(R.string.tr_bars_yesterday), right, height - 6 * d, text)
        text.alpha = 255
    }
}

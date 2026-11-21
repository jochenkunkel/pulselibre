package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Chart of the [RestingPulseHero.Model]: daily points; a line only between consecutive valid days (never across a gap),
 * translucent area under each run, dashed line for the previous period's median. No smoothing across missing days.
 */
internal class RestingPulseChartView(context: Context) : View(context) {
    private val palette = AppPalette.of(context)
    private val d = resources.displayMetrics.density
    private var model: RestingPulseHero.Model? = null
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val area = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f * d; pathEffect = DashPathEffect(floatArrayOf(6 * d, 5 * d), 0f) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f * d }
    private val dateFormat = DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(context.displayLocale(), "dM"), context.displayLocale())

    init { tag = "resting_pulse_chart" }

    fun show(value: RestingPulseHero.Model) {
        model = value
        contentDescription = value.description()
        invalidate()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) =
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (150 * d).toInt())

    override fun onDraw(canvas: Canvas) {
        val m = model ?: return
        val left = 34 * d; val right = width - 6 * d; val top = 8 * d; val bottom = height - 22 * d
        label.color = palette.muted
        if (m.points.isEmpty()) {
            label.textAlign = Paint.Align.CENTER
            canvas.drawText(context.getString(R.string.tr_rest_chart_none), width / 2f, height / 2f, label)
            return
        }
        val values = m.points.map { it.bpm } + listOfNotNull(m.baselineBpm)
        var lo = values.min(); var hi = values.max()
        if (hi - lo < 6) { val mid = (hi + lo) / 2; lo = mid - 3; hi = mid + 3 }
        val pad = (hi - lo) * 0.12; lo -= pad; hi += pad
        fun x(i: Int) = left + (right - left) * (i + 0.5f) / m.windowDays
        fun y(v: Double) = (bottom - (bottom - top) * ((v - lo) / (hi - lo))).toFloat()
        // axis labels: value range only (started at the shown minimum, stated in the labels)
        label.textAlign = Paint.Align.RIGHT
        canvas.drawText("${hi.toInt()}", left - 4 * d, top + 8 * d, label)
        canvas.drawText("${lo.toInt()}", left - 4 * d, bottom, label)
        label.textAlign = Paint.Align.LEFT; canvas.drawText(m.firstDay.format(dateFormat), left, height - 4 * d, label)
        label.textAlign = Paint.Align.RIGHT; canvas.drawText(m.lastDay.format(dateFormat), right, height - 4 * d, label)
        m.baselineBpm?.let { b ->
            dash.color = palette.muted
            canvas.drawLine(left, y(b), right, y(b), dash)
        }
        line.color = palette.pulse
        area.color = (palette.pulse and 0x00FFFFFF) or 0x33000000
        dot.color = palette.pulse
        m.segments.forEach { seg ->
            if (seg.size >= 2) {
                val path = Path(); val fill = Path()
                seg.forEachIndexed { i, p -> if (i == 0) { path.moveTo(x(p.dayIndex), y(p.bpm)); fill.moveTo(x(p.dayIndex), bottom); fill.lineTo(x(p.dayIndex), y(p.bpm)) }
                    else { path.lineTo(x(p.dayIndex), y(p.bpm)); fill.lineTo(x(p.dayIndex), y(p.bpm)) } }
                fill.lineTo(x(seg.last().dayIndex), bottom); fill.close()
                canvas.drawPath(fill, area); canvas.drawPath(path, line)
            }
        }
        val r = if (m.windowDays > 30) 1.8f * d else 3f * d
        m.points.forEach { canvas.drawCircle(x(it.dayIndex), y(it.bpm), r, dot) }
    }
}

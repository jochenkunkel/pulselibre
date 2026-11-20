package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Stacked lanes on one shared local-day axis (#263). Gaps are hatched boxes with the label "keine Daten"
 * (information is never only a colour); touching or dragging shows a crosshair and reports time and the value of
 * every lane at that moment, or "keine Daten". No interpolation: lines connect only observations that are close.
 */
internal class DayTimelineView(
    context: Context,
    private val day: DashboardDay,
    private val lanes: List<DayTimeline.Lane>,
    private val zone: ZoneId,
    private val goal: Long?,
    private val ink: Int,
    private val track: Int,
    private val accents: List<Int>,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 11f, resources.displayMetrics)
    }
    private val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = track; style = Paint.Style.STROKE; strokeWidth = density }
    private val margin = 8 * density
    private val left = 40 * density
    private val laneGap = 22 * density
    private val laneHeight = 88 * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    private val tickFractions = day.axisFractions()
    private val tickLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var scrolling = false

    /** Selected instant; null = nothing selected. */
    var selected: Instant? = null
        private set
    var onSelect: (Instant, String) -> Unit = { _, _ -> }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val h = (margin + lanes.size * (laneHeight + laneGap) + margin).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), h)
    }

    private fun x(fraction: Float) = left + (width - left - margin) * fraction
    private fun laneTop(index: Int) = margin + index * (laneHeight + laneGap)

    private fun y(lane: DayTimeline.Lane, index: Int, value: Long): Float {
        val lo = if (lane.cumulative) 0L else lane.min!!
        val hi = maxOf(if (lane.cumulative) maxOf(goal ?: 0L, lane.max!!) else lane.max!!, lo + 1)
        return laneTop(index) + laneHeight * (1f - ((value - lo).toFloat() / (hi - lo)))
    }

    override fun onDraw(canvas: Canvas) {
        lanes.forEachIndexed { index, lane ->
            val top = laneTop(index)
            val bottom = top + laneHeight
            paint.color = track; paint.style = Paint.Style.STROKE; paint.strokeWidth = density
            tickFractions.forEach { canvas.drawLine(x(it), top, x(it), bottom, paint) }
            canvas.drawLine(left, bottom, width - margin, bottom, paint)
            text.textAlign = Paint.Align.LEFT
            canvas.drawText(lane.title, left, top - 4 * density, text)
            // Gaps: hatched box + label, so a gap is readable without colour.
            lane.gaps.forEach { gap ->
                val a = x(day.fraction(gap.start)); val b = x(day.fraction(gap.end))
                var s = a
                while (s < b) { canvas.drawLine(s, bottom, minOf(s + laneHeight * 0.4f, b), top, hatch); s += 8 * density }
                if (b - a > text.measureText("keine Daten") + 4 * density) {
                    text.textAlign = Paint.Align.CENTER
                    canvas.drawText("keine Daten", (a + b) / 2, (top + bottom) / 2, text)
                }
            }
            if (lane.points.isEmpty()) return@forEachIndexed
            // Value axis: low and high of the lane scale.
            text.textAlign = Paint.Align.RIGHT
            val lo = if (lane.cumulative) 0L else lane.min!!
            val hi = maxOf(if (lane.cumulative) maxOf(goal ?: 0L, lane.max!!) else lane.max!!, lo + 1)
            canvas.drawText(lo.toString(), left - 4 * density, bottom, text)
            canvas.drawText(hi.toString(), left - 4 * density, top + text.textSize, text)
            val color = accents[index % accents.size]
            if (lane.cumulative && goal != null && goal > 0) {
                paint.color = color; paint.alpha = 140
                paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(6 * density, 4 * density), 0f)
                canvas.drawLine(left, y(lane, index, goal), width - margin, y(lane, index, goal), paint)
                paint.pathEffect = null; paint.alpha = 255
            }
            paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f * density
            val path = Path()
            lane.points.forEachIndexed { i, p ->
                val px = x(day.fraction(p.time)); val py = y(lane, index, p.value)
                if (i == 0 || !lane.connectsToNext(i - 1)) path.moveTo(px, py)
                else if (lane.cumulative) { path.lineTo(px, y(lane, index, lane.points[i - 1].value)); path.lineTo(px, py) }
                else path.lineTo(px, py)
            }
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            if (!lane.cumulative) lane.points.forEach {
                canvas.drawCircle(x(day.fraction(it.time)), y(lane, index, it.value), 1.6f * density, paint)
            }
        }
        // Axis labels under the last lane.
        text.textAlign = Paint.Align.CENTER; text.color = ink
        val labelY = laneTop(lanes.size - 1) + laneHeight + text.textSize + 2 * density
        val showMiddle = width >= 5 * (text.measureText("00:00") + margin)
        tickFractions.forEachIndexed { i, f ->
            if (showMiddle || i == 0 || i == tickLabels.lastIndex) {
                text.textAlign = when (i) { 0 -> Paint.Align.LEFT; tickLabels.lastIndex -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
                canvas.drawText(tickLabels[i], x(f), labelY, text)
            }
        }
        selected?.let { t ->
            paint.color = ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * density; paint.alpha = 255
            val cx = x(day.fraction(t))
            canvas.drawLine(cx, laneTop(0), cx, laneTop(lanes.size - 1) + laneHeight, paint)
        }
    }

    /** Text for the instant [t]: time and the value of each lane, or "keine Daten". */
    internal fun valuesAt(t: Instant): String = timeFormat.format(t) + " · " + lanes.joinToString(" · ") { lane ->
        val i = lane.nearestIndex(t)
        if (i == null) "${lane.title}: keine Daten"
        else "${lane.title}: ${lane.points[i].value}${if (lane.unit.isEmpty()) "" else " " + lane.unit}"
    }

    fun select(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        val t = day.start.plusMillis((java.time.Duration.between(day.start, day.end).toMillis() * f).toLong())
        selected = t
        invalidate()
        onSelect(t, valuesAt(t))
    }

    private fun fractionAt(px: Float) = ((px - left) / (width - left - margin)).coerceIn(0f, 1f)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; dragging = false; scrolling = false }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && !scrolling) {
                    val h = abs(event.x - downX); val v = abs(event.y - downY)
                    if (h > touchSlop && h > v) { dragging = true; parent.requestDisallowInterceptTouchEvent(true) }
                    else if (v > touchSlop) scrolling = true
                }
                if (dragging) select(fractionAt(event.x))
            }
            MotionEvent.ACTION_UP -> {
                if (!scrolling) { if (!dragging) performClick(); select(fractionAt(event.x)) }
                parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}

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
    private val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = track; alpha = 110; style = Paint.Style.STROKE; strokeWidth = 0.8f * density }
    private val margin = 8 * density
    private val left = 40 * density
    private val laneGap = 22 * density
    private val laneHeight = 88 * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    private val noData: String = context.getString(R.string.tl_no_data)
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
        val h = (topPad + lanes.size * (laneHeight + laneGap) + margin).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), h)
    }

    /** Visible window of the day (#264); changing it only changes the axis, never the data. */
    var viewport = DayViewport()
        set(value) { field = value; invalidate(); onViewport(value) }
    var onViewport: (DayViewport) -> Unit = {}
    private val scale = android.view.ScaleGestureDetector(context, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var lastFocus = 0f
        override fun onScaleBegin(d: android.view.ScaleGestureDetector): Boolean { lastFocus = d.focusX; return true }
        override fun onScale(d: android.view.ScaleGestureDetector): Boolean {
            val focus = viewport.start + fractionAtScreen(d.focusX) * viewport.span
            var v = viewport.zoom(focus, d.scaleFactor)
            v = v.pan(-(d.focusX - lastFocus) / (width - left - margin) * v.span)   // two-finger pan
            lastFocus = d.focusX
            viewport = v
            return true
        }
    })

    fun zoomIn() { viewport = viewport.zoom(centerFraction(), 2f) }
    fun zoomOut() { viewport = viewport.zoom(centerFraction(), 0.5f) }
    fun panBy(direction: Int) { viewport = viewport.pan(direction * viewport.span / 2f) }
    fun resetZoom() { viewport = DayViewport() }
    private fun centerFraction() = (viewport.start + viewport.end) / 2f

    private fun fractionAtScreen(px: Float) = ((px - left) / (width - left - margin)).coerceIn(0f, 1f)
    private fun x(fraction: Float) = left + (width - left - margin) * ((fraction - viewport.start) / viewport.span)
    /** Room above the first lane for its title (S25 check: the title was cut off with only `margin`). */
    private val topPad = 26 * density
    private fun laneTop(index: Int) = topPad + index * (laneHeight + laneGap)

    private fun y(lane: DayTimeline.Lane, index: Int, value: Long): Float {
        val lo = if (lane.cumulative) 0L else lane.min!!
        val hi = maxOf(if (lane.cumulative) maxOf(goal ?: 0L, lane.max!!) else lane.max!!, lo + 1)
        return laneTop(index) + laneHeight * (1f - ((value - lo).toFloat() / (hi - lo)))
    }

    private val goalDash = android.graphics.DashPathEffect(floatArrayOf(6 * density, 4 * density), 0f)

    override fun onDraw(canvas: Canvas) {
        val ticks = visibleTicks()   // once per frame, not once per lane (audit A6-60)
        lanes.forEachIndexed { index, lane ->
            val top = laneTop(index)
            val bottom = top + laneHeight
            paint.color = track; paint.style = Paint.Style.STROKE; paint.strokeWidth = density
            ticks.forEach { canvas.drawLine(x(it.first), top, x(it.first), bottom, paint) }
            canvas.drawLine(left, bottom, width - margin, bottom, paint)
            text.textAlign = Paint.Align.LEFT
            canvas.drawText(lane.title, left, top - 4 * density, text)
            // Value axis first (outside the plot clip): low and high of the lane scale.
            if (lane.points.isNotEmpty()) {
                text.textAlign = Paint.Align.RIGHT
                val lo = if (lane.cumulative) 0L else lane.min!!
                val hi = maxOf(if (lane.cumulative) maxOf(goal ?: 0L, lane.max!!) else lane.max!!, lo + 1)
                canvas.drawText(lo.toString(), left - 4 * density, bottom, text)
                canvas.drawText(hi.toString(), left - 4 * density, top + text.textSize, text)
            }
            // Gaps: a light box with 45-degree hatching, clipped to the gap, plus the label "keine Daten"
            // (a gap is readable without colour).
            canvas.save(); canvas.clipRect(left, top - 2 * density, width - margin, bottom + 2 * density)
            lane.gaps.forEach { gap ->
                val a = x(day.fraction(gap.start)).coerceAtLeast(left); val b = x(day.fraction(gap.end)).coerceAtMost(width - margin)
                if (b <= a) return@forEach
                canvas.save(); canvas.clipRect(a, top, b, bottom)
                var sx = a - laneHeight
                while (sx < b) { canvas.drawLine(sx, bottom, sx + laneHeight, top, hatch); sx += 10 * density }
                canvas.restore()
                if (b - a > text.measureText(noData) + 8 * density) {
                    text.textAlign = Paint.Align.CENTER
                    canvas.drawText(noData, (a + b) / 2, (top + bottom) / 2, text)
                }
            }
            if (lane.points.isEmpty()) { canvas.restore(); return@forEachIndexed }
            val color = accents[index % accents.size]
            if (lane.cumulative && goal != null && goal > 0) {
                paint.color = color; paint.alpha = 140
                paint.pathEffect = goalDash
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
            canvas.restore()
        }
        // Axis labels under the last lane.
        text.textAlign = Paint.Align.CENTER; text.color = ink
        val labelY = laneTop(lanes.size - 1) + laneHeight + text.textSize + 2 * density
        val showMiddle = width >= 5 * (text.measureText("00:00") + margin)
        // Collect label boxes first; on a collision the later label is dropped, except the last one (the end of the
        // axis), which replaces its crowded neighbour.
        class Label(val text: String, val x: Float, val align: Paint.Align, val left: Float, val right: Float)
        val labels = mutableListOf<Label>()
        ticks.forEachIndexed { i, (f, label) ->
            if (!(showMiddle || i == 0 || i == ticks.lastIndex)) return@forEachIndexed
            val align = when (i) { 0 -> Paint.Align.LEFT; ticks.lastIndex -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            val w = text.measureText(label)
            val l = when (align) { Paint.Align.LEFT -> x(f); Paint.Align.RIGHT -> x(f) - w; else -> x(f) - w / 2 }
            val item = Label(label, x(f), align, l, l + w)
            val prev = labels.lastOrNull()
            if (prev == null || item.left >= prev.right + 6 * density) labels += item
            else if (i == ticks.lastIndex) { labels.removeAt(labels.lastIndex); labels += item }
        }
        labels.forEach { text.textAlign = it.align; canvas.drawText(it.text, it.x, labelY, text) }
        selected?.let { t ->
            paint.color = ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * density; paint.alpha = 255
            val cx = x(day.fraction(t))
            if (cx >= left && cx <= width - margin) canvas.drawLine(cx, laneTop(0), cx, laneTop(lanes.size - 1) + laneHeight, paint)
        }
    }

    /** Ticks (day fraction, label) inside the viewport; labels come from real instants, so DST days stay correct. */
    private fun visibleTicks(): List<Pair<Float, String>> {
        val dayMinutes = java.time.Duration.between(day.start, day.end).toMinutes()
        val step = DayViewport.tickMinutes(viewport.span, dayMinutes)
        val out = mutableListOf<Pair<Float, String>>()
        var m = 0L
        while (m <= dayMinutes) {
            val t = day.start.plusSeconds(m * 60)
            val f = day.fraction(t)
            if (f >= viewport.start - 1e-4f && f <= viewport.end + 1e-4f) {
                out += f to (if (m == dayMinutes) "24:00" else timeFormat.format(t))
            }
            m += step
        }
        return out
    }

    /** Text for the instant [t]: time and the value of each lane, or "keine Daten". */
    internal fun valuesAt(t: Instant): String = timeFormat.format(t) + " · " + lanes.joinToString(" · ") { lane ->
        val i = lane.nearestIndex(t)
        if (i == null) context.getString(R.string.tl_lane_value, lane.title, noData)
        else context.getString(R.string.tl_lane_value, lane.title, "${lane.points[i].value}${if (lane.unit.isEmpty()) "" else " " + lane.unit}")
    }

    fun select(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        val t = day.start.plusMillis((java.time.Duration.between(day.start, day.end).toMillis() * f).toLong())
        selected = t
        invalidate()
        onSelect(t, valuesAt(t))
    }

    private fun fractionAt(px: Float) = viewport.start + fractionAtScreen(px) * viewport.span

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scale.onTouchEvent(event)
        if (scale.isInProgress || event.pointerCount > 1) { dragging = false; scrolling = true; parent.requestDisallowInterceptTouchEvent(true); return true }
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

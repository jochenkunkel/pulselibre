package de.pulselibre

import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executor
import kotlin.math.abs

internal fun showDashboardDay(
    activity: MainActivity,
    title: String,
    worker: Executor
) {
    val zone = ZoneId.systemDefault()
    val strings = Strings.of(activity)
    val locale: java.util.Locale = activity.resources.configuration.locales[0]
    val palette = AppPalette.of(activity)
    val today = LocalDate.now(zone)
    val metric = DashboardMeasurement.entries.firstOrNull {
        it.title == title || it == DashboardMeasurement.STEPS && title == "Schritte heute"
    }
    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
    }
    fun label() = TextView(activity).apply {
        setTextColor(palette.ink)
        textSize = 15f
        content.addView(this)
    }
    fun styled(button: Button) = button.apply {
        isAllCaps = false
        setTextColor(palette.accent)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(palette.tinted); cornerRadius = 12 * resources.displayMetrics.density
        }
    }
    fun button(title: String) = styled(Button(activity)).apply {
        text = title
        content.addView(this)
    }
    // Order: date, chart, selected value, point navigation (side by side), then the statistics text.
    val dateButton = button("")
    val chartHolder = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    content.addView(chartHolder)
    val selected = label()
    selected.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    val navRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
    fun navButton(title: String) = styled(Button(activity)).apply { text = title }
    val previous = navButton(activity.getString(R.string.dd_prev))
    val next = navButton(activity.getString(R.string.dd_next))
    previous.contentDescription = activity.getString(R.string.dd_prev_cd)
    next.contentDescription = activity.getString(R.string.dd_next_cd)
    navRow.addView(previous, LinearLayout.LayoutParams(0, -2, 1f))
    navRow.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
    content.addView(navRow)
    val overview = label().apply { textSize = 13f; setTextColor(palette.muted) }
    // Heart rate only: the band's own history (FIT, band measurement time) as a separate chart, never merged with the
    // phone-reception series above (#334, #477). Before, stored band values stayed invisible in this dialog.
    val bandHolder = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; tag = "day_band_section" }
    content.addView(bandHolder)
    val dialog = AlertDialog.Builder(activity).setTitle(MetricText.title(activity, title))
        .setView(ScrollView(activity).apply { addView(content) })
        .setPositiveButton(R.string.action_close, null).create().also { DialogProbe.track(it) }
    var request = 0
    val dateFormat = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale)
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss XXX").withZone(zone)
    fun load(date: LocalDate) {
        val generation = ++request
        dateButton.text = date.format(dateFormat)
        overview.text = activity.getString(R.string.day_reading)
        chartHolder.removeAllViews()
        bandHolder.removeAllViews()
        selected.text = ""
        previous.visibility = View.GONE
        next.visibility = View.GONE
        val now = Instant.now()
        val day = DashboardDay(date, zone)
        if (metric == null) {
            overview.text = activity.getString(R.string.dd_no_sleep)
            return
        }
        // Diagnostic only: if this load is still pending after 15 s, log where the worker thread is stuck.
        overview.postDelayed({
            if (dialog.isShowing && generation == request && overview.text.toString() == activity.getString(R.string.day_reading))
                DashboardWorkerWatch.recordStall(activity.applicationContext, "day-dialog")
        }, DashboardWorkerWatch.STALL_AFTER_MS)
        worker.execute {
            val series = try { DashboardDayReader(activity.applicationContext).read(metric, day, now) }
                catch (_: Exception) { null }
            val bandSeries = BandSeries.of(metric)
            val band = if (bandSeries == null) null else try {
                val window = FitHistoryReader(activity.applicationContext).read(bandSeries, day.start, day.end)
                BandDay(DayBandBuckets.reduce(window.points, day), window.points.size, window.kinds, failed = false, series = bandSeries)
            } catch (_: Exception) { BandDay(emptyList(), 0, emptyMap(), failed = true, series = bandSeries) }
            activity.runOnUiThread {
                if (!dialog.isShowing || generation != request || activity.isDestroyed) return@runOnUiThread
                band?.let { showBandDay(activity, bandHolder, day, it, palette, strings) }
                if (series == null) {
                    overview.text = activity.getString(R.string.day_read_failed)
                    return@runOnUiThread
                }
                val points = series.points
                val suffix = when (metric) {
                    DashboardMeasurement.STEPS -> activity.getString(R.string.dd_unit_steps)
                    DashboardMeasurement.CALORIES_TOTAL, DashboardMeasurement.CALORIES_ACTIVE -> activity.getString(R.string.dd_unit_device)
                    else -> metric.unit
                }
                val context = when (metric) {
                    DashboardMeasurement.OXYGEN -> " · " + activity.getString(R.string.dd_ctx_oxygen)
                    DashboardMeasurement.RESTING_PULSE -> " · " + activity.getString(R.string.dd_ctx_resting)
                    else -> ""
                }
                overview.text = buildString {
                    if (points.size < 2) append(activity.getString(R.string.dd_no_course)).append("\n")
                    append(activity.getString(R.string.dd_basis, zone.id)).append("\n").append(activity.getString(R.string.dd_valid, series.count))
                    append(" · ").append(activity.getString(R.string.dd_invalid, series.invalid))
                    if (series.duplicate > 0) append(" · ").append(activity.getString(R.string.dd_duplicates, series.duplicate))
                    append("\n").append(activity.getString(R.string.dd_minutes, series.minutesObserved, series.availableMinutes))
                    val quality = DayDataQuality.assess(points.map { it.time }, day, now)
                    if (points.isNotEmpty()) append("\n" + quality.summary(strings))
                    append("\n").append(activity.getString(R.string.dd_status, activity.getString(MetricAvailability.of(metric, quality, series.count).labelRes)))
                    if (series.count > points.size) {
                        append("\n").append(activity.getString(R.string.dd_chart_note))
                    }
                    append(context)
                    if (date == today && points.isNotEmpty()) {
                        val age = java.time.Duration.between(points.last().time, now).toMinutes()
                        append("\n").append(activity.getString(R.string.dd_last_today, strings.get(R.string.dur_ago, strings.get(R.string.dur_min, age))))
                    }
                    series.goal?.let { append("\n").append(activity.getString(R.string.dd_goal, it.toString())) }
                }
                if (points.isEmpty()) return@runOnUiThread
                var index = points.lastIndex
                labelLatest(selected, points.last(), suffix, timeFormat)
                if (points.size == 1) return@runOnUiThread
                val fixedScale = metric == DashboardMeasurement.STRESS || metric == DashboardMeasurement.BODY_BATTERY
                val chart = DayChart(activity, day, points, series.goal,
                    fixedScale, palette.indicator, palette.track, palette.ink)
                chartHolder.addView(TextView(activity).apply {
                    text = activity.getString(R.string.dd_scale, chart.min.toString(), chart.max.toString(), suffix)
                    setTextColor(palette.ink)
                })
                fun select(pointIndex: Int) {
                    index = pointIndex
                    chart.selected = index
                    labelLatest(selected, points[index], suffix, timeFormat)
                    previous.isEnabled = index > 0
                    next.isEnabled = index < points.lastIndex
                }
                chart.onSelect = ::select
                chartHolder.addView(chart, LinearLayout.LayoutParams(-1,
                    (160 * activity.resources.displayMetrics.density).toInt()))
                previous.visibility = View.VISIBLE
                next.visibility = View.VISIBLE
                previous.setOnClickListener { select(index - 1) }
                next.setOnClickListener { select(index + 1) }
                select(index)
            }
        }
    }
    var day = today
    dateButton.setOnClickListener {
        DatePickerDialog(activity, { _, year, month, date ->
            day = LocalDate.of(year, month + 1, date)
            load(day)
        }, day.year, day.monthValue - 1, day.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }
    dialog.show()
    load(day)
}

private fun labelLatest(label: TextView, point: DayObservation, unit: String, time: DateTimeFormatter) {
    label.text = "${time.format(point.time)} · ${point.value} $unit"
}

internal class DayChart(
    activity: android.content.Context,
    day: DashboardDay,
    private val points: List<DayObservation>,
    private val goal: Long?,
    fixedScale: Boolean,
    private val ink: Int,
    private val track: Int,
    private val labelColor: Int
) : View(activity) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val margin = 8 * density
    private val left = 36 * density // room for the value axis labels
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
    }
    private val tickFractions = day.axisFractions()
    private val tickLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")
    private val pointFractions = points.map { day.fraction(it.time) }
    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var scrollGesture = false
    val min = if (fixedScale || goal != null) 0L else points.minOf { it.value }
    val max = if (fixedScale) 100L else maxOf(goal ?: min, points.maxOf { it.value }, if (min == Long.MAX_VALUE) min else min + 1)
    var onSelect: (Int) -> Unit = {}
    var selected = points.lastIndex
        set(value) { field = value; invalidate() }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val plotBottom: Float
        get() = (height - 2 * margin - (labelPaint.descent() - labelPaint.ascent())).coerceAtLeast(margin)

    private fun x(fraction: Float) = left + (width - left - margin) * fraction
    internal fun xAt(fraction: Float) = x(fraction)
    private fun y(value: Long) = plotBottom - (plotBottom - margin) *
        ((value.toDouble() - min) / (max.toDouble() - min).coerceAtLeast(1.0)).toFloat()

    override fun onDraw(canvas: Canvas) {
        paint.color = track
        val labelBaseline = height - margin - labelPaint.descent()
        val showIntermediateLabels = width >= 5 * (labelPaint.measureText("00:00") + margin)
        tickFractions.forEachIndexed { index, fraction ->
            val column = x(fraction)
            canvas.drawLine(column, margin, column, plotBottom, paint)
            if (showIntermediateLabels || index == 0 || index == tickLabels.lastIndex) {
                labelPaint.textAlign = when (index) {
                    0 -> Paint.Align.LEFT
                    tickLabels.lastIndex -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(tickLabels[index], column, labelBaseline, labelPaint)
            }
        }
        goal?.let { canvas.drawLine(left, y(it), width - margin, y(it), paint) }
        // Value axis: lowest, middle and highest value of the scale, right-aligned left of the plot.
        labelPaint.textAlign = Paint.Align.RIGHT
        for (value in longArrayOf(min, (min + max) / 2, max).distinct()) {
            canvas.drawText(value.toString(), left - 4 * density, y(value) + labelPaint.textSize * 0.35f, labelPaint)
        }
        paint.color = ink
        points.forEachIndexed { index, point ->
            canvas.drawCircle(x(pointFractions[index]), y(point.value), margin * 0.25f, paint)
        }
        canvas.drawCircle(x(pointFractions[selected]), y(points[selected].value), margin * 0.75f, paint)
    }

    private fun selectNearest(event: MotionEvent) {
        val index = points.indices.minBy {
            abs(event.x - x(pointFractions[it])) + abs(event.y - y(points[it].value)) * 0.1f
        }
        if (index != selected) onSelect(index)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragging = false
                scrollGesture = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && !scrollGesture) {
                    val horizontal = abs(event.x - downX)
                    val vertical = abs(event.y - downY)
                    if (horizontal > touchSlop && horizontal > vertical) {
                        dragging = true
                        parent.requestDisallowInterceptTouchEvent(true)
                    } else if (vertical > touchSlop) {
                        scrollGesture = true
                    }
                }
                if (dragging) selectNearest(event)
            }
            MotionEvent.ACTION_UP -> {
                if (!scrollGesture) {
                    if (!dragging) performClick()
                    selectNearest(event)
                }
                parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> parent.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}

/** Band heart rate (FIT) of one local day: reduced chart points, the stored value count and the FIT message kinds. */
internal data class BandDay(val points: List<DayObservation>, val count: Int, val kinds: Map<BandRecordKind, Int>, val failed: Boolean,
                             val series: BandSeries = BandSeries.HEART_RATE)

/** Same reduction as the live day series: per observed minute the minimum, the maximum and the latest value. */
internal object DayBandBuckets {
    fun reduce(points: List<HistoryPoint>, day: DashboardDay): List<DayObservation> {
        val buckets = java.util.TreeMap<Long, List<DayObservation>>()
        for (p in points) {
            if (p.receivedAt < day.start || p.receivedAt >= day.end) continue
            val minute = java.time.Duration.between(day.start, p.receivedAt).toMinutes()
            val point = DayObservation(p.receivedAt, p.value.toLong())
            val candidates = buckets[minute].orEmpty() + point
            buckets[minute] = listOf(candidates.minBy { it.value }, candidates.maxBy { it.value }, candidates.maxBy { it.time })
                .distinct().sortedBy { it.time }
        }
        return buckets.values.flatten()
    }
}

private fun showBandDay(activity: MainActivity, holder: LinearLayout, day: DashboardDay, band: BandDay, palette: AppPalette, strings: Strings) {
    fun text(value: String, size: Float, color: Int) = TextView(activity).apply { text = value; textSize = size; setTextColor(color) }
    val density = activity.resources.displayMetrics.density
    holder.addView(text(activity.getString(R.string.dd_band_title), 15f, palette.ink).apply { setPadding(0, (16 * density).toInt(), 0, 0) })
    when {
        band.failed -> holder.addView(text(activity.getString(R.string.dd_band_failed), 13f, palette.muted))
        band.count == 0 -> holder.addView(text(activity.getString(
            if (band.series == BandSeries.HEART_RATE) R.string.dd_band_none else R.string.dd_band_none_metric), 13f, palette.muted))
        else -> {
            // Monitoring/record split exists only for heart rate; stress, SpO2 and respiration have their own FIT messages.
            val summary = if (band.series == BandSeries.HEART_RATE) activity.getString(R.string.dd_band_summary, band.count,
                band.kinds[BandRecordKind.MONITORING] ?: 0, band.kinds[BandRecordKind.ACTIVITY] ?: 0)
                else activity.getString(R.string.dd_band_summary_plain, band.count)
            holder.addView(text(summary, 13f, palette.muted).apply { tag = "day_band_summary" })
            // An activity recording alone is no normal all-day history: say so instead of implying a full day.
            if (band.series == BandSeries.HEART_RATE &&
                (band.kinds[BandRecordKind.MONITORING] ?: 0) == 0 && (band.kinds[BandRecordKind.ACTIVITY] ?: 0) > 0)
                holder.addView(text(strings.get(R.string.hv_fit_activity_only), 12f, palette.ink).apply { tag = "day_band_activity_only" })
            if (band.points.size >= 2) {
                val color = when (band.series) { BandSeries.HEART_RATE -> palette.pulse; BandSeries.SPO2 -> palette.oxygen; else -> palette.accent }
                val unit = when (band.series) {
                    BandSeries.HEART_RATE -> activity.getString(R.string.dd_unit_bpm); BandSeries.SPO2 -> "%"
                    BandSeries.STRESS -> activity.getString(R.string.dd_unit_device_scale)
                    BandSeries.RESPIRATION -> activity.getString(R.string.dd_unit_breaths) }
                val chart = DayChart(activity, day, band.points, null, false, color, palette.track, palette.ink)
                holder.addView(text(activity.getString(R.string.dd_scale, chart.min.toString(), chart.max.toString(), unit), 13f, palette.ink))
                holder.addView(chart, LinearLayout.LayoutParams(-1, (160 * density).toInt()))
            }
        }
    }
}

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
    val previous = navButton("‹ Vorheriger")
    val next = navButton("Nächster ›")
    previous.contentDescription = "Vorheriger Datenpunkt"
    next.contentDescription = "Nächster Datenpunkt"
    navRow.addView(previous, LinearLayout.LayoutParams(0, -2, 1f))
    navRow.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
    content.addView(navRow)
    val overview = label().apply { textSize = 13f; setTextColor(palette.muted) }
    val dialog = AlertDialog.Builder(activity).setTitle(title)
        .setView(ScrollView(activity).apply { addView(content) })
        .setPositiveButton("Schließen", null).create()
    var request = 0
    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss XXX").withZone(zone)
    fun load(date: LocalDate) {
        val generation = ++request
        dateButton.text = date.format(dateFormat)
        overview.text = "Daten werden gelesen …"
        chartHolder.removeAllViews()
        selected.text = ""
        previous.visibility = View.GONE
        next.visibility = View.GONE
        val now = Instant.now()
        val day = DashboardDay(date, zone)
        if (metric == null) {
            overview.text = "Noch kein Tagesverlauf verfügbar. Validierte Schlafdaten fehlen."
            return
        }
        worker.execute {
            val series = try { DashboardDayReader(activity.applicationContext).read(metric, day, now) }
                catch (_: android.database.sqlite.SQLiteException) { null }
            activity.runOnUiThread {
                if (!dialog.isShowing || generation != request || activity.isDestroyed) return@runOnUiThread
                if (series == null) {
                    overview.text = "Lokale Messdaten konnten nicht gelesen werden."
                    return@runOnUiThread
                }
                val points = series.points
                val suffix = when (metric) {
                    DashboardMeasurement.STEPS -> "Schritte"
                    DashboardMeasurement.CALORIES_TOTAL, DashboardMeasurement.CALORIES_ACTIVE -> "Geräteeinheit ungeprüft"
                    else -> metric.unit
                }
                val context = when (metric) {
                    DashboardMeasurement.OXYGEN -> " · CIRQA-Sensor nicht validiert"
                    DashboardMeasurement.RESTING_PULSE -> " · Gerätefeld, Messbedingungen ungeprüft"
                    else -> ""
                }
                overview.text = buildString {
                    if (points.size < 2) append("Noch kein Tagesverlauf verfügbar.\n")
                    append("Empfangszeit · ${zone.id}\n${series.count} gültige Messwerte")
                    append(" · ${series.invalid} ohne gültigen Wert")
                    if (series.duplicate > 0) append(" · ${series.duplicate} Duplikate")
                    append("\nMinuten mit gültigem Empfang: ${series.minutesObserved} von ${series.availableMinutes}")
                    val quality = DayDataQuality.assess(points.map { it.time }, day, now)
                    if (points.isNotEmpty()) append("\n" + quality.summary())
                    append("\nDatenstatus: " + MetricAvailability.of(metric, quality, series.count).label)
                    if (series.count > points.size) {
                        append("\nDiagramm: Minimum, Maximum und letzter Wert je Empfangsminute.")
                    }
                    append(context)
                    if (date == today && points.isNotEmpty()) {
                        val age = java.time.Duration.between(points.last().time, now).toMinutes()
                        append("\nLetzter Empfang heute vor $age min")
                    }
                    series.goal?.let { append("\nLetztes Geräte-Schrittziel: $it") }
                }
                if (points.isEmpty()) return@runOnUiThread
                var index = points.lastIndex
                labelLatest(selected, points.last(), suffix, timeFormat)
                if (points.size == 1) return@runOnUiThread
                val fixedScale = metric == DashboardMeasurement.STRESS || metric == DashboardMeasurement.BODY_BATTERY
                val chart = DayChart(activity, day, points, series.goal,
                    fixedScale, palette.indicator, palette.track, palette.ink)
                chartHolder.addView(TextView(activity).apply {
                    text = "Skala · ${chart.min}–${chart.max} $suffix"
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

package de.pulselibre

import android.app.DatePickerDialog
import android.view.View
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

private data class Day(val pulse: DashboardDaySeries, val steps: DashboardDaySeries, val band: List<DayObservation>)

/** #263: heart rate and the step counter of one local day on one shared time axis, read from local data only. */
internal fun showDayTimeline(activity: MainActivity, worker: Executor) {
    val zone = ZoneId.systemDefault()
    val strings = Strings.of(activity)
    val locale: java.util.Locale = activity.resources.configuration.locales[0]
    val bandLaneTitle = activity.getString(R.string.tl_lane_band)
    val palette = AppPalette.of(activity)
    val density = activity.resources.displayMetrics.density
    fun text(size: Float, color: Int) = TextView(activity).apply { textSize = size; setTextColor(color) }
    val content = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        val p = (16 * density).toInt(); setPadding(p, p, p, p)
    }
    val dateButton = Button(activity).apply {
        isAllCaps = false; setTextColor(palette.accent); tag = "day_timeline_date"
        background = android.graphics.drawable.GradientDrawable().apply { setColor(palette.tinted); cornerRadius = 12 * density }
    }
    content.addView(dateButton)
    val chartHolder = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    content.addView(chartHolder)
    val selected = text(14f, palette.ink).apply {
        tag = "day_timeline_selected"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    content.addView(selected)
    val summary = text(13f, palette.muted).apply { tag = "day_timeline_summary" }
    content.addView(summary)
    val dialog = AlertDialog.Builder(activity).setTitle(R.string.main_day_link_title)
        .setView(ScrollView(activity).apply { addView(content) })
        .setPositiveButton(R.string.action_close, null).create().also { DialogProbe.track(it) }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale)
    var request = 0
    var date = LocalDate.now(zone)
    fun load() {
        val generation = ++request
        val day = DashboardDay(date, zone)
        dateButton.text = date.format(dateFormat)
        summary.text = activity.getString(R.string.day_reading); selected.text = ""; chartHolder.removeAllViews()
        val now = Instant.now()
        summary.postDelayed({
            if (dialog.isShowing && generation == request && summary.text.toString() == activity.getString(R.string.day_reading))
                DashboardWorkerWatch.recordStall(activity.applicationContext, "day-timeline")
        }, DashboardWorkerWatch.STALL_AFTER_MS)
        worker.execute {
            val read = try {
                val reader = DashboardDayReader(activity.applicationContext)
                // Band history (FIT) follows the band's measurement time and is its own lane, never merged with phone-time readings (#334).
                val band = try {
                    FitHistoryReader(activity.applicationContext).read(day.start, maxOf(day.start.plusSeconds(1), minOf(day.end, now.plusSeconds(60))))
                        .points.map { DayObservation(it.receivedAt, it.value.toLong()) }
                } catch (_: Exception) { emptyList() }
                Day(reader.read(DashboardMeasurement.PULSE, day, now), reader.read(DashboardMeasurement.STEPS, day, now), band)
            } catch (_: Exception) { null }
            activity.runOnUiThread {
                if (!dialog.isShowing || generation != request || activity.isDestroyed) return@runOnUiThread
                if (read == null) { summary.text = activity.getString(R.string.day_read_failed); return@runOnUiThread }
                val (pulse, steps, band) = read
                val allLanes = listOf(
                    // #763: one heart-rate lane, live and band values together
                    DayTimeline.lane(DashboardMeasurement.PULSE, MetricText.title(activity, "Herzfrequenz"), "bpm", TileInsightCalc.merge(pulse.points, band), day, now),
                    DayTimeline.lane(DashboardMeasurement.STEPS, MetricText.title(activity, "Schritte (Tageszähler)"), "", steps.points, day, now),
                )
                val lanes = allLanes.filter { it.points.isNotEmpty() }
                val description = DayTimeline.describe(strings, allLanes, day, now, zone)
                summary.text = description + "\n" + activity.getString(R.string.tl_note_phone) + " " +
                    (if (band.isNotEmpty()) activity.getString(R.string.tl_note_band, bandLaneTitle) + " " else "") +
                    activity.getString(R.string.tl_note_gaps, zone.id)
                if (lanes.isEmpty()) return@runOnUiThread
                val chart = DayTimelineView(activity, day, lanes, zone,
                    effectiveStepGoal(activity.getSharedPreferences(SpO2DisplaySettings.STORE, android.content.Context.MODE_PRIVATE).getString("step_goal", null), steps.goal), palette.ink, palette.track,
                    listOf(palette.indicator, palette.accent, palette.muted))
                chart.contentDescription = description
                chart.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                chart.onSelect = { _, line -> selected.text = line }
                chartHolder.addView(chart, LinearLayout.LayoutParams(-1, -2))
                val zoomInfo = text(12f, palette.muted).apply { tag = "day_timeline_zoom_info" }
                fun describeViewport(v: DayViewport) {
                    zoomInfo.text = if (v.isFull) activity.getString(R.string.tl_zoom_full)
                    else activity.getString(R.string.tl_zoom_part, "%.1f".format(locale, v.span * 24))
                }
                chart.onViewport = ::describeViewport
                describeViewport(chart.viewport)
                val tools = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                fun tool(label: String, description: String, action: () -> Unit) = Button(activity).apply {
                    text = label; contentDescription = description; isAllCaps = false; minWidth = 0; minimumWidth = 0
                    setTextColor(palette.accent)
                    background = android.graphics.drawable.GradientDrawable().apply { setColor(palette.tinted); cornerRadius = 12 * density }
                    setOnClickListener { action() }
                    tools.addView(this, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = (4 * density).toInt() })
                }
                tool("‹", activity.getString(R.string.tl_pan_back)) { chart.panBy(-1) }
                tool("−", activity.getString(R.string.tl_zoom_out)) { chart.zoomOut() }
                tool("+", activity.getString(R.string.tl_zoom_in)) { chart.zoomIn() }
                tool("›", activity.getString(R.string.tl_pan_forward)) { chart.panBy(1) }
                tool(activity.getString(R.string.tl_day_button), activity.getString(R.string.tl_day_button_cd)) { chart.resetZoom() }
                chartHolder.addView(tools)
                chartHolder.addView(zoomInfo)
                selected.text = activity.getString(R.string.tl_hint)
            }
        }
    }
    dateButton.setOnClickListener {
        DatePickerDialog(activity, { _, y, m, d -> date = LocalDate.of(y, m + 1, d); load() },
            date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }
    dialog.show()
    load()
}

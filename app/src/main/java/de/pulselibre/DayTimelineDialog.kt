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

/** #263: heart rate and the step counter of one local day on one shared time axis, read from local data only. */
internal fun showDayTimeline(activity: MainActivity, worker: Executor) {
    val zone = ZoneId.systemDefault()
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
    val dialog = AlertDialog.Builder(activity).setTitle("Tag im Überblick")
        .setView(ScrollView(activity).apply { addView(content) })
        .setPositiveButton("Schließen", null).create()
    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    var request = 0
    var date = LocalDate.now(zone)
    fun load() {
        val generation = ++request
        val day = DashboardDay(date, zone)
        dateButton.text = date.format(dateFormat)
        summary.text = "Daten werden gelesen …"; selected.text = ""; chartHolder.removeAllViews()
        val now = Instant.now()
        worker.execute {
            val read = try {
                val reader = DashboardDayReader(activity.applicationContext)
                Triple(reader.read(DashboardMeasurement.PULSE, day, now), reader.read(DashboardMeasurement.STEPS, day, now), true)
            } catch (_: android.database.sqlite.SQLiteException) { null }
            activity.runOnUiThread {
                if (!dialog.isShowing || generation != request || activity.isDestroyed) return@runOnUiThread
                if (read == null) { summary.text = "Lokale Messdaten konnten nicht gelesen werden."; return@runOnUiThread }
                val (pulse, steps, _) = read
                val allLanes = listOf(
                    DayTimeline.lane(DashboardMeasurement.PULSE, "Herzfrequenz", "bpm", pulse.points, day, now),
                    DayTimeline.lane(DashboardMeasurement.STEPS, "Schritte (Tageszähler)", "", steps.points, day, now),
                )
                val lanes = allLanes.filter { it.points.isNotEmpty() }
                val description = DayTimeline.describe(allLanes, day, now, zone)
                summary.text = description + "\nEmpfangszeit · ${zone.id}. Lücken bleiben leer und werden nicht überbrückt."
                if (lanes.isEmpty()) return@runOnUiThread
                val chart = DayTimelineView(activity, day, lanes, zone,
                    effectiveStepGoal(activity.getSharedPreferences(SpO2DisplaySettings.STORE, android.content.Context.MODE_PRIVATE).getString("step_goal", null), steps.goal), palette.ink, palette.track,
                    listOf(palette.indicator, palette.accent))
                chart.contentDescription = description
                chart.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                chart.onSelect = { _, line -> selected.text = line }
                chartHolder.addView(chart, LinearLayout.LayoutParams(-1, -2))
                val zoomInfo = text(12f, palette.muted).apply { tag = "day_timeline_zoom_info" }
                fun describeViewport(v: DayViewport) {
                    zoomInfo.text = if (v.isFull) "Ganzer Tag. Zwei Finger zum Zoomen, oder Tasten."
                    else "Ausschnitt ${"%.1f".format(java.util.Locale.GERMANY, v.span * 24)} h von 24 h (Rohpunkte, keine Mittelwerte)."
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
                tool("‹", "Ausschnitt zurück") { chart.panBy(-1) }
                tool("−", "Herauszoomen") { chart.zoomOut() }
                tool("+", "Hineinzoomen") { chart.zoomIn() }
                tool("›", "Ausschnitt vor") { chart.panBy(1) }
                tool("Tag", "Ganzen Tag anzeigen") { chart.resetZoom() }
                chartHolder.addView(tools)
                chartHolder.addView(zoomInfo)
                selected.text = "Tippen oder ziehen, um Werte zu einer Uhrzeit zu sehen."
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

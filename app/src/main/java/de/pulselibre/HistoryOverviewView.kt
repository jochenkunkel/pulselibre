package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Local-only summaries of stored observations; no missing windows are filled or connected. */
class HistoryOverviewView(
    context: Context,
    private val load: (HistoryRange, Int) -> HistorySnapshot
) : LinearLayout(context) {
    /** Loader without window navigation (always the newest window). */
    constructor(context: Context, load: (HistoryRange) -> HistorySnapshot) : this(context, { range, _ -> load(range) })

    private val palette = AppPalette.of(context)
    private var selectedRange = HistoryRange.DAY
    /** 0 = newest window; n = n windows (of the selected range) back (#110). */
    private var selectedOffset = 0

    init { orientation = VERTICAL }

    /** Which clock a series' time axis follows; the two are never merged into one series (#334). */
    internal enum class TimeBasis(val legend: String, val info: String, val emptyText: String) {
        PHONE_RECEIVED(
            "Zeitachse: Telefonzeit · Punkt: Median · Ring: Mittelwert (bei mehreren Werten) · Linie: Min–Max · Lücken: keine Messdaten",
            "Zeitstempel = Empfang auf dem Telefon.",
            "Keine Messdaten in diesem Zeitraum"),
        DEVICE_MEASURED(
            "Zeitachse: Messzeit des Bandes (aus FIT-Dateien) · Punkt: Median · Ring: Mittelwert (bei mehreren Werten) · Linie: Min–Max · Lücken: keine Messdaten",
            "Zeitstempel = vom Band gemessen (FIT-Datei), nicht der Empfang auf dem Telefon. Bei einem Teil der Werte ist die Zeit aus der Kurzform der Datei zurückgerechnet.",
            "Keine Band-Verlaufsdaten in diesem Zeitraum")
    }

    private companion object {
        const val FIT_HEART_RATE_TITLE = "Herzfrequenz · Band-Verlauf (FIT)"
        val worker: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor()
    }

    private var generation = 0
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var attached = true

    /** Loads off the UI thread; only the newest request may render (#79). */
    fun refresh() {
        val token = ++generation
        val range = selectedRange
        val offset = selectedOffset
        worker.execute {
            val snapshot = try { load(range, offset) } catch (_: Exception) { null }
            mainHandler.post {
                if (token != generation || !attached) return@post
                if (snapshot == null) showFailure() else render(snapshot)
            }
        }
    }

    /** Same result as [refresh] but on the caller's thread; for deterministic tests only. */
    internal fun refreshSynchronously() {
        generation++
        val snapshot = try { load(selectedRange, selectedOffset) } catch (_: Exception) { null }
        if (snapshot == null) showFailure() else render(snapshot)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); attached = true }
    override fun onDetachedFromWindow() { attached = false; super.onDetachedFromWindow() }

    /** Previous/next window of the selected range; limited to the locally cached 30 days. */
    private fun addWindowNavigation(snapshot: HistorySnapshot) {
        val maxOffset = selectedRange.maxOffset()
        if (maxOffset == 0 && selectedOffset == 0) return // 30 days: nothing to navigate
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ofPattern(context.shortDatePattern(selectedRange == HistoryRange.DAY), context.displayLocale())
        fun step(label: String, description: String, enabled: Boolean, target: Int, tagName: String) =
            label(label, 20f, if (enabled) palette.accent else palette.muted, true).apply {
                gravity = Gravity.CENTER
                tag = tagName
                contentDescription = description
                isEnabled = enabled
                alpha = if (enabled) 1f else 0.4f
                isClickable = enabled
                isFocusable = enabled
                background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(12).toFloat() }
                if (enabled) setOnClickListener {
                    selectedOffset = target
                    showLoading()
                    refresh()
                }
            }
        val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(step("‹", context.getString(R.string.hv_window_previous), selectedOffset < maxOffset, selectedOffset + 1, "history_window_previous"),
            LayoutParams(dp(48), dp(48)))
        val title = if (selectedOffset == 0) context.getString(R.string.hv_window_current, fmt.format(snapshot.end.atZone(zone)))
            else "${fmt.format(snapshot.start.atZone(zone))} – ${fmt.format(snapshot.end.atZone(zone))}"
        row.addView(label(title, 13f, palette.ink, true).apply {
            gravity = Gravity.CENTER
            tag = "history_window_title"
            contentDescription = context.getString(R.string.hv_window_cd, title)
        }, LayoutParams(0, -2, 1f))
        row.addView(step("›", context.getString(R.string.hv_window_next), selectedOffset > 0, selectedOffset - 1, "history_window_next"),
            LayoutParams(dp(48), dp(48)))
        // free date choice inside the cached 30 days
        row.addView(android.widget.ImageView(context).apply {
            setImageDrawable(PlIcon.drawable(context, PlIcon.CALENDAR, 24, palette.accent))
            scaleType = android.widget.ImageView.ScaleType.CENTER; tag = "history_window_date"
            contentDescription = context.getString(R.string.hv_pick_date)
            isClickable = true; isFocusable = true
            background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(12).toFloat() }
            setOnClickListener {
                val today = java.time.LocalDate.now(zone)
                val current = snapshot.end.atZone(zone).toLocalDate()
                android.app.DatePickerDialog(context, { _, y, m, d ->
                    selectedOffset = selectedRange.offsetForDate(java.time.LocalDate.of(y, m + 1, d), today)
                    showLoading(); refresh()
                }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                    datePicker.minDate = System.currentTimeMillis() - java.time.Duration.ofDays(29).toMillis()
                }.show()
            }
        }, LayoutParams(dp(48), dp(48)).apply { marginStart = dp(6) })
        addView(row, LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }

    private fun showLoading() {
        // Keep the previous charts visible; only mark the selection and the pending state immediately.
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v is TextView && v.tag == "history_loading") return
        }
        addView(label(context.getString(R.string.hv_loading, DomainText.tr(context, selectedRange.label)), 12f, palette.muted).apply { tag = "history_loading" },
            0, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun render(snapshot: HistorySnapshot) {
        removeAllViews()
        addView(label(context.getString(R.string.hv_header), 12f, palette.muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val rangeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        HistoryRange.entries.forEach { range ->
            val selected = range == selectedRange
            val button = label(DomainText.tr(context, range.label), 12f, if (selected) palette.card else palette.accent, true).apply {
                gravity = Gravity.CENTER
                minHeight = dp(48)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                isFocusable = true
                isSelected = selected
                contentDescription = DomainText.tr(context, range.label).let { if (selected) context.getString(R.string.hv_range_selected_cd, it) else it }
                background = GradientDrawable().apply {
                    setColor(if (selected) palette.primary else palette.tinted)
                    cornerRadius = dp(12).toFloat()
                }
                setOnClickListener {
                    if (selectedRange != range) {
                        selectedRange = range
                        selectedOffset = 0 // a new range starts at the newest window
                        showLoading()
                        refresh()
                    }
                }
            }
            rangeRow.addView(button, LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        }
        addView(rangeRow, LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        addWindowNavigation(snapshot)
        val resolution = HistoryResolution.forRange(snapshot.range)
        allDataCard(snapshot)
        // #334: FIT-Historie des Bands in eigener Karte mit eigener Zeitbasis (Messzeit), nie mit Empfangszeit vermischt.
        // Zuerst, weil sie die meisten Werte enthaelt; feinere Aufloesung, weil die Banddaten dicht sind.
        if (snapshot.fitAvailable || snapshot.fitHeartRate.isNotEmpty() || snapshot.fitReadFailed) {
            metricCard(FIT_HEART_RATE_TITLE, "bpm", snapshot.fitHeartRate, snapshot, 240, palette.pulse,
                HistoryResolution.forFitRange(snapshot.range), TimeBasis.DEVICE_MEASURED, fitEmptyNote(snapshot),
                FitKindText.note(Strings.of(context), snapshot.fitKinds, snapshot.fitDuplicates))
        }
        metricCard("Herzfrequenz", "bpm", snapshot.heartRate, snapshot, 240, palette.pulse, resolution)
        metricCard("Sauerstoffsättigung · SpO₂", "%", snapshot.oxygen, snapshot, 100, palette.oxygen, resolution)
        // The scientific trend block now lives in the main tab "Analysen" (#227).
    }

    /**
     * One table for live (phone) and imported (band) data: a row per day, source and measure, newest day first.
     * Sources keep their own clock and are never merged; the caption says which time each source uses.
     */
    private fun allDataCard(snapshot: HistorySnapshot) {
        val rows = snapshot.allData
        val locale = context.resources.configuration.locales[0]
        val dayFormat = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT).withLocale(locale)
        val number = java.text.NumberFormat.getInstance(locale).apply { maximumFractionDigits = 1 }
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(18).toFloat() }
            tag = "history_all_data"
        }
        card.addView(label(context.getString(R.string.ht_title), 15f, palette.ink, true))
        fun cell(text: String, weight: Float, bold: Boolean = false, color: Int = palette.ink, align: Int = Gravity.START) =
            label(text, 11f, color, bold).apply { gravity = align }.also { it.layoutParams = LinearLayout.LayoutParams(0, -2, weight) }
        fun line(cells: List<TextView>, description: String? = null) = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(5), 0, dp(5))
            minimumHeight = dp(28)
            cells.forEach { addView(it) }
            if (description != null) { contentDescription = description; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                cells.forEach { it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO } }
        }
        if (rows.isEmpty()) {
            card.addView(label(context.getString(R.string.ht_empty), 14f, palette.muted),
                LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(6) })
        } else {
            card.addView(line(listOf(
                cell(context.getString(R.string.ht_col_date), 1.0f, true, palette.muted),
                cell(context.getString(R.string.ht_col_source) + " · " + context.getString(R.string.ht_col_metric), 1.9f, true, palette.muted),
                cell(context.getString(R.string.ht_col_count), 0.9f, true, palette.muted, Gravity.END),
                cell(context.getString(R.string.ht_col_range), 1.3f, true, palette.muted, Gravity.END),
                cell(context.getString(R.string.ht_col_mean), 0.9f, true, palette.muted, Gravity.END))),
                LayoutParams(-1, -2).apply { topMargin = dp(8) })
            val offsetMs = ZoneId.systemDefault().rules.getOffset(snapshot.end).totalSeconds * 1000L
            rows.forEach { row ->
                val source = context.getString(when (row.origin) {
                    MeasurementOrigin.BAND -> R.string.ht_src_band
                    MeasurementOrigin.LIVE -> R.string.ht_src_phone
                    MeasurementOrigin.IMPORT -> R.string.ht_src_import
                })
                val metric = metricLabel(row.metric)?.let { context.getString(it) } ?: "${row.metric} (${row.unit})"
                val day = dayFormat.format(java.time.LocalDate.ofEpochDay(row.dayIndex))
                val noValid = row.count == 0
                val range = if (noValid) "–" else "${number.format(row.minimum)}–${number.format(row.maximum)}"
                // Profile codes (sleep level, SpO2 mode/confidence, activity type) are categories: a mean is meaningless.
                val mean = if (noValid || row.unit == "profile_code") "–" else number.format(row.mean)
                val invalidNote = if (row.invalid > 0) context.getString(R.string.ht_invalid, row.invalid) else null
                val label = if (invalidNote == null) "$source · $metric" else "$source · $metric\n$invalidNote"
                val description = context.getString(R.string.ht_row_cd, day, source, metric, row.count, range, mean) +
                    (invalidNote?.let { ", $it" } ?: "")
                card.addView(line(listOf(
                    cell(day, 1.0f),
                    cell(label, 1.9f),
                    cell(row.count.toString(), 0.9f, align = Gravity.END),
                    cell(range, 1.3f, align = Gravity.END),
                    cell(mean, 0.9f, align = Gravity.END)),
                    description),
                    LayoutParams(-1, -2))
            }
        }
        card.addView(label(context.getString(R.string.ht_note), 11f, palette.muted).apply { tag = "history_all_data_note" },
            LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addView(card, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    /** Readable name of a stored metric key; null for keys this version does not know (shown as stored). */
    private fun metricLabel(metric: String): Int? = when (metric) {
        "heart_rate", "bpm" -> R.string.ht_metric_pulse
        "resting_bpm" -> R.string.ht_m_resting
        "spo2_percent" -> R.string.ht_metric_spo2
        "steps" -> R.string.ht_m_steps
        "goal" -> R.string.ht_m_goal
        "total" -> R.string.ht_m_kcal_total
        "active" -> R.string.ht_m_kcal_active
        "moderate" -> R.string.ht_m_min_moderate
        "vigorous" -> R.string.ht_m_min_vigorous
        "rr_ms" -> R.string.ht_m_rr
        "stress" -> R.string.ht_m_stress
        "body_battery" -> R.string.ht_m_battery
        "breaths_per_min", "respiration" -> R.string.ht_m_breath
        // #696: band metrics from FIT (Garmin profile 21.222.0)
        "spo2" -> R.string.ht_metric_spo2
        "spo2_confidence" -> R.string.ht_m_spo2_confidence
        "spo2_measurement_mode" -> R.string.ht_m_spo2_mode
        "sleep_level" -> R.string.ht_m_sleep_level
        "skin_temperature_deviation" -> R.string.ht_m_skin_dev
        "skin_temperature_7_day_deviation" -> R.string.ht_m_skin_dev7
        "monitoring_cycles" -> R.string.ht_m_cycles
        "monitoring_activity_type", "monitoring_current_activity_type" -> R.string.ht_m_activity_type
        "monitoring_intensity_code" -> R.string.ht_m_intensity_code
        // #150
        "hrv" -> R.string.ht_m_hrv
        "hrv_weekly_average" -> R.string.ht_m_hrv_week
        "hrv_last_night_average" -> R.string.ht_m_hrv_night
        "hrv_last_night_5_min_high" -> R.string.ht_m_hrv_night_high
        "resting_heart_rate" -> R.string.ht_m_resting
        "current_day_resting_heart_rate" -> R.string.ht_m_resting_today
        "active_calories" -> R.string.ht_m_kcal_active
        "active_time" -> R.string.ht_m_active_time
        "moderate_activity_minutes" -> R.string.ht_m_min_moderate
        "vigorous_activity_minutes" -> R.string.ht_m_min_vigorous
        "distance" -> R.string.ht_m_distance
        "speed" -> R.string.ht_m_speed
        "cadence" -> R.string.ht_m_cadence
        "power" -> R.string.ht_m_power
        else -> null
    }

    /** Why the band card is empty: read error (A6-29) or the date of the newest band measurement outside the window (#477). */
    private fun fitEmptyNote(snapshot: HistorySnapshot): String? = when {
        snapshot.fitReadFailed -> context.getString(R.string.hv_fit_read_failed)
        snapshot.fitHeartRate.isEmpty() && snapshot.fitNewest != null -> context.getString(R.string.hv_fit_newest,
            java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
                .withLocale(context.resources.configuration.locales[0]).withZone(java.time.ZoneId.systemDefault()).format(snapshot.fitNewest))
        else -> null
    }

    /** Cards switched to "Verteilung" (#219). The view re-renders on every new reading; the mode must survive that. */
    private val densityTitles = mutableSetOf<String>()
    private val safeTitles = mutableSetOf<String>()

    private fun metricCard(
        title: String,
        unit: String,
        points: List<HistoryPoint>,
        snapshot: HistorySnapshot,
        hardMaximum: Int,
        accent: Int,
        resolution: HistoryResolution,
        basis: TimeBasis = TimeBasis.PHONE_RECEIVED,
        emptyNote: String? = null,
        /** Where the shown values come from (e.g. normal band history vs. activity recordings); shown with data only. */
        sourceNote: String? = null
    ) {
        val buckets = aggregateHistoryPoints(points, snapshot.start, snapshot.end, resolution)
        // Fit the vertical range to the observed data (e.g. SpO₂ 89–100 % no longer squeezed into the top of 0–100).
        val scale = if (buckets.isEmpty()) AxisScale(0, hardMaximum)
            else AxisScale.forData(buckets.minOf { it.minimum }, buckets.maxOf { it.maximum }, 0, hardMaximum,
                if (hardMaximum <= 100) 10 else 40)
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(palette.card)
                cornerRadius = dp(18).toFloat()
            }
            tag = "history_series_$title"
        }
        val heading = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(PlIcon.view(context, DashboardMetricIconView.iconFor(title), 20, accent), LayoutParams(dp(20), dp(20)).apply {
            marginEnd = dp(7)
        })
        heading.addView(label(DomainText.tr(context, title), 15f, palette.ink, true).apply { maxLines = 2 }, LayoutParams(0, -2, 1f))
        val infoButton = android.widget.FrameLayout(context).apply {
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.hv_info_cd, DomainText.tr(context, title))
            tag = "history_info_$title"
            setOnClickListener { showMetricInfo(title, unit, basis) }
            addView(PlIcon.view(context, PlIcon.INFO_SOURCE, 20, palette.indicator),
                android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        }
        heading.addView(infoButton, LayoutParams(dp(48), dp(48)))
        card.addView(heading)
        card.addView(label(context.getString(R.string.hv_card_summary, points.size, buckets.size, DomainText.tr(context, resolution.label), scale.minimum.toString(), scale.maximum.toString(), unit),
            12f, palette.muted), LayoutParams(-1, -2).apply { topMargin = dp(4) })
        if (points.isNotEmpty()) sourceNote?.let { note ->
            card.addView(label(note, 12f, palette.ink).apply { tag = "history_source_note" }, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        if (points.isEmpty()) {
            card.addView(label(DomainText.tr(context, basis.emptyText), 14f, palette.muted),
                LayoutParams(-1, -2).apply { topMargin = dp(18); bottomMargin = dp(18) })
            emptyNote?.let { note ->
                card.addView(label(note, 12f, palette.ink).apply { tag = "history_empty_note" },
                    LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
            // The explanatory caption stays even without data (consistent per card, see #182).
            card.addView(label(
                DomainText.tr(context, basis.legend), 11f, palette.muted
            ).apply { tag = "history_chart_legend"; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES },
                LayoutParams(-1, -2).apply { topMargin = dp(8) })
        } else {
            // #219: switch between the existing points view and the density ("Verteilung") view.
            val scatter = HistoryScatterChartView(context, buckets, points.size, snapshot.start, snapshot.end,
                scale, accent, snapshot.range, resolution)
            val scatterLegend = label(
                DomainText.tr(context, basis.legend), 11f, palette.muted
            ).apply { tag = "history_chart_legend"; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES }
            val densityLegend = label(HistoryDensityDetail.legend(Strings.of(context), false), 11f, palette.muted).apply {
                tag = "history_density_legend"; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES; visibility = View.GONE
            }
            val zone = ZoneId.systemDefault()
            var densityView: HistoryDensityChartView? = null
            val modes = LinearLayout(context).apply { orientation = HORIZONTAL; tag = "history_mode_$title" }
            fun styleMode(t: TextView, on: Boolean) { t.setTextColor(if (on) palette.accent else palette.muted); t.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL) }
            val pointsMode = TextView(context); val densityMode = TextView(context); val safeMode = TextView(context)
            listOf(pointsMode to "Punkte", densityMode to "Verteilung", safeMode to "Farbsicher").forEach { (t, name) ->
                val shown = DomainText.tr(context, name)
                t.text = shown; t.textSize = 12f; t.gravity = Gravity.CENTER; t.minimumHeight = dp(48); t.isClickable = true; t.isFocusable = true
                t.contentDescription = context.getString(R.string.hv_mode_cd, shown); t.tag = "history_mode_${name}_$title"
                modes.addView(t, LayoutParams(0, -2, 1f))
            }
            styleMode(pointsMode, true); styleMode(densityMode, false); styleMode(safeMode, false)
            val chartHost = android.widget.FrameLayout(context)
            chartHost.addView(scatter, android.widget.FrameLayout.LayoutParams(-1, dp(210)))
            pointsMode.setOnClickListener {
                densityTitles.remove(title); safeTitles.remove(title)
                styleMode(pointsMode, true); styleMode(densityMode, false); styleMode(safeMode, false)
                scatter.visibility = View.VISIBLE; densityView?.visibility = View.GONE
                scatterLegend.visibility = View.VISIBLE; densityLegend.visibility = View.GONE
            }
            fun showDensity(safe: Boolean) {
                densityTitles.add(title); if (safe) safeTitles.add(title) else safeTitles.remove(title)
                styleMode(pointsMode, false); styleMode(densityMode, !safe); styleMode(safeMode, safe)
                if (densityView == null) {
                    val binWidth = (scale.span / 20).coerceAtLeast(1)
                    val density = aggregateHistoryDensity(points, snapshot.start, snapshot.end, resolution, scale, binWidth)
                    densityView = HistoryDensityChartView(context, density, scale, zone) { index ->
                        val column = density.columns[index]
                        androidx.appcompat.app.AlertDialog.Builder(context).setTitle(context.getString(R.string.hv_density_title, DomainText.tr(context, title)))
                            .setMessage(HistoryDensityDetail.describe(Strings.of(context), column, points, unit, zone, index == density.columns.lastIndex, context.resources.configuration.locales[0]))
                            .setPositiveButton(context.getString(R.string.action_close), null).show()
                    }
                    chartHost.addView(densityView, android.widget.FrameLayout.LayoutParams(-1, dp(210)))
                }
                densityView?.colorSafe = safe
                densityLegend.text = HistoryDensityDetail.legend(Strings.of(context), safe)
                scatter.visibility = View.GONE; densityView?.visibility = View.VISIBLE
                scatterLegend.visibility = View.GONE; densityLegend.visibility = View.VISIBLE
            }
            densityMode.setOnClickListener { showDensity(false) }
            safeMode.setOnClickListener { showDensity(true) }
            card.addView(modes, LayoutParams(-1, -2).apply { topMargin = dp(4) })
            card.addView(chartHost, LayoutParams(-1, -2))
            card.addView(scatterLegend, LayoutParams(-1, -2).apply { topMargin = dp(8) })
            card.addView(densityLegend, LayoutParams(-1, -2).apply { topMargin = dp(8) })
            if (title in safeTitles) safeMode.performClick() else if (title in densityTitles) densityMode.performClick()
        }
        addView(card, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun showFailure() {
        removeAllViews()
        addView(label(context.getString(R.string.hv_failed), 14f, palette.muted))
    }

    private fun showMetricInfo(title: String, unit: String, basis: TimeBasis = TimeBasis.PHONE_RECEIVED) {
        val profile = MetricReferenceProfile.forMetric(DashboardMetric(title, "Keine Messdaten", ""))
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(8))
        }
        fun row(symbol: String, text: String, url: String? = null) {
            val icon = MetricInfoIconMapping.forSymbol(symbol)
            val line = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            line.addView(PlIcon.view(context, icon, 18, if (url == null) palette.muted else palette.accent),
                LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(10) })
            line.addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(if (url == null) palette.ink else palette.accent)
                if (url != null) {
                    isFocusable = true
                    isClickable = true
                    setOnClickListener {
                        try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url))) } catch (_: Exception) { }
                    }
                }
            }, LayoutParams(0, -2, 1f))
            content.addView(line)
        }
        row("▬", context.getString(R.string.ov_scale, (profile?.minimum ?: 0).toString(), (profile?.maximum ?: if (unit == "%") 100 else 240).toString(), unit))
        profile?.referenceMinimum?.let { low ->
            row("▰", "$low–${profile.referenceMaximum} $unit · ${DomainText.tr(context, profile.referenceLabel ?: "")}")
        }
        profile?.sourceName?.let { row("↗", context.getString(R.string.ov_source, it), profile.sourceUrl) }
        row("⚠", profile?.caveat?.let { DomainText.tr(context, it) } ?: context.getString(R.string.hv_no_norm))
        row("◷", DomainText.tr(context, basis.info))
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("ⓘ  ${DomainText.tr(context, title)}")
            .setView(content)
            .setPositiveButton(context.getString(R.string.action_close), null)
            .show()
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        includeFontPadding = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

private class HistoryScatterChartView(
    context: Context,
    private val buckets: List<HistoryBucket>,
    private val storedPointCount: Int,
    private val start: java.time.Instant,
    private val end: java.time.Instant,
    private val axis: AxisScale,
    private val accent: Int,
    range: HistoryRange,
    private val resolution: HistoryResolution
) : View(context) {
    private val palette = AppPalette.of(context)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plot = RectF()
    private val timeFormatter = DateTimeFormatter.ofPattern(if (range == HistoryRange.DAY) "HH:mm" else "dd.MM")
        .withZone(ZoneId.systemDefault())

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.hv_chart_cd, storedPointCount, buckets.size, DomainText.tr(context, resolution.label),
            axis.minimum, axis.maximum, HistoryAxisSpeech.span(Strings.of(context), start, end, range, ZoneId.systemDefault(), context.shortDatePattern(true), context.shortDatePattern(false)))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val left = 42f * density
        val right = w - 18f * density
        val top = 12f * density
        val bottom = h - 31f * density
        if (right <= left || bottom <= top) return
        plot.set(left, top, right, bottom)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = palette.track
        for (i in 0..4) {
            val fraction = i / 4f
            val y = plot.bottom - fraction * plot.height()
            canvas.drawLine(plot.left, y, plot.right, y, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 10f * density
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = palette.muted
            canvas.drawText((axis.minimum + axis.span * fraction).toInt().toString(), plot.left - 5f * density,
                y + 3.5f * density, paint)
            paint.style = Paint.Style.STROKE
            paint.color = palette.track
        }
        val zone = ZoneId.systemDefault()
        val span = java.time.Duration.between(start, end).toMillis().coerceAtLeast(1L)
        val bucketMillis = resolution.duration.toMillis()
        val tickCount = ((span + bucketMillis - 1L) / bucketMillis).toInt().coerceAtLeast(1)
        paint.style = Paint.Style.STROKE
        for (index in 0..tickCount) {
            val fraction = index.toFloat() / tickCount
            val x = plot.left + fraction * plot.width()
            val major = index % resolution.majorTickEvery == 0 || index == tickCount
            if (index > 0) {
                paint.color = palette.track
                paint.alpha = if (major) 150 else 65
                paint.strokeWidth = if (major) 1f * density else 0.7f * density
                canvas.drawLine(x, plot.top, x, plot.bottom, paint)
            }
            paint.alpha = 255
            paint.strokeWidth = 1f * density
            canvas.drawLine(x, plot.bottom, x, plot.bottom + (if (major) 4f else 2.5f) * density, paint)
            if (major) {
                val time = start.plusMillis(span * index / tickCount).atZone(zone)
                paint.style = Paint.Style.FILL
                paint.textSize = 10f * density
                paint.color = palette.muted
                paint.textAlign = Paint.Align.CENTER // all labels centred on their tick: uniform spacing
                canvas.drawText(timeFormatter.format(time), x, h - 7f * density, paint)
                paint.style = Paint.Style.STROKE
            }
        }

        buckets.forEach { bucket ->
            val center = bucket.start.plusMillis(java.time.Duration.between(bucket.start, bucket.end).toMillis() / 2L)
            val xFraction = (java.time.Duration.between(start, center).toMillis().toDouble() / span)
                .toFloat().coerceIn(0f, 1f)
            val x = plot.left + xFraction * plot.width()
            val minY = plot.bottom - ((bucket.minimum.toFloat() - axis.minimum) / axis.span).coerceIn(0f, 1f) * plot.height()
            val medianY = plot.bottom - ((bucket.median.toFloat() - axis.minimum) / axis.span).coerceIn(0f, 1f) * plot.height()
            val maxY = plot.bottom - ((bucket.maximum.toFloat() - axis.minimum) / axis.span).coerceIn(0f, 1f) * plot.height()
            paint.color = accent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.1f * density
            canvas.drawLine(x, maxY, x, minY, paint)
            canvas.drawLine(x - 1.5f * density, maxY, x + 1.5f * density, maxY, paint)
            canvas.drawLine(x - 1.5f * density, minY, x + 1.5f * density, minY, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, medianY, 2f * density, paint)
            // arithmetic mean as a small ring: where it differs from the median the distribution is skewed (#79)
            if (bucket.count > 1) {
                val meanY = plot.bottom - ((bucket.mean.toFloat() - axis.minimum) / axis.span).coerceIn(0f, 1f) * plot.height()
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * density
                canvas.drawCircle(x, meanY, 3.4f * density, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }
}

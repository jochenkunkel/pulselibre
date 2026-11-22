package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.Executors

/** 7/28/90-day trends of device resting pulse and daily steps (#41). Not a diagnosis. */
internal class TrendCardView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val content = LinearLayout(context).apply { orientation = VERTICAL }
    private var generation = 0
    private var data: Data? = null
    private val nf = NumberFormat.getNumberInstance(context.displayLocale()).apply { maximumFractionDigits = 1 }

    /** Everything the card shows, computed off the UI thread. */
    class Data(
        val overview: FitnessOverview.Result,
        val rest: List<TrendAnalysis.Result>,
        val steps: List<TrendAnalysis.Result>,
        val correlation: PairedCorrelation.Result,
        val scatter: List<Pair<Double, Double>>,
        val restDaily: Map<LocalDate, Double> = emptyMap(),
        val stepsDaily: Map<LocalDate, Double> = emptyMap(),
        val today: LocalDate = LocalDate.now(),
        val sensitivity: PairedCorrelation.Sensitivity? = null,
        /** Daily steps INCLUDING today's running counter (atlas only); empty = use [stepsDaily]. */
        val stepsWithToday: Map<LocalDate, Double> = emptyMap()
    )

    init {
        orientation = VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(16).toFloat(); UnvalidatedFeatures.mark(this, resources.displayMetrics.density, palette.dark) }
        contentDescription = context.getString(R.string.tr_cd) + DomainText.tr(context, UnvalidatedFeatures.ACCESSIBILITY_SUFFIX)
        tag = "trend_card"
        addView(content, LayoutParams(-1, -2))
        showMessage(context.getString(R.string.tr_computing))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val cached = TrendCache.fresh()
        if (cached != null) { data = cached; render(cached); return }
        load()
    }

    override fun onDetachedFromWindow() { generation++; super.onDetachedFromWindow() }

    private fun load() {
        val token = ++generation
        val app = context.applicationContext
        worker.execute {
            val result = try { compute(app) } catch (_: Exception) { null }
            Handler(Looper.getMainLooper()).post {
                if (token != generation) return@post
                if (result == null) { showMessage(context.getString(R.string.tr_failed)); return@post }
                TrendCache.store(result); data = result
                render(result)
            }
        }
    }

    private fun compute(app: Context): Data {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val first = today.minusDays(180)
        val reader = DailyValueReader(app)
        val rest = reader.read(DashboardMeasurement.RESTING_PULSE, first, today, zone)
        val steps = reader.read(DashboardMeasurement.STEPS, first, today, zone)
        val windows = listOf(7, 28, 90)
        val restTrends = windows.map { TrendAnalysis.analyze(rest, today, it, 1.0) }
        val stepTrends = windows.map { TrendAnalysis.analyze(steps, today, it, 500.0) }
        return Data(FitnessOverview.assess(stepTrends[1], restTrends[1]), restTrends, stepTrends,
            PairedCorrelation.analyze(steps, rest, 1),
            PairedCorrelation.pair(steps, rest, 1).map { it.second to it.third }, rest, steps, today,
            PairedCorrelation.sensitivity(steps, rest, 1),
            reader.read(DashboardMeasurement.STEPS, first, today.plusDays(1), zone))
    }

    private fun showMessage(text: String) {
        content.removeAllViews()
        content.addView(titleRow(null))
        content.addView(TextView(context).apply { this.text = text; textSize = 13f; setTextColor(palette.muted) })
    }

    private fun titleRow(info: (() -> Unit)?): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(PlIcon.view(context, PlIcon.TRENDS, 24), LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
        addView(TextView(context).apply {
            text = context.getString(R.string.tr_title); textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink)
        }, LayoutParams(0, -2, 1f))
        if (info != null) addView(FrameLayout(context).apply {
            contentDescription = context.getString(R.string.tr_method_cd)
            isClickable = true; isFocusable = true
            setOnClickListener { info() }
            addView(PlIcon.view(context, PlIcon.INFO, 22), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        }, LayoutParams(dp(48), dp(48)))
    }

    /** Dimension icons identify *what* is assessed; the state is written text only (no status via icon). */
    private fun dimensionIcon(name: String) = when (name) {
        "Fitnessentwicklung" -> PlIcon.FITNESS
        "Regeneration" -> PlIcon.RECOVERY
        else -> PlIcon.CONSISTENCY
    }

    private fun render(d: Data) {
        content.removeAllViews()
        windowListeners.clear()
        content.addView(titleRow { showDetails(d) })
        addWindowSegments()
        addRestingHero(d)
        addWeekSummary(d)
        // 28-day overview: icon + name + state; sentences live in the ⓘ dialog and as accessibility text.
        d.overview.dimensions.forEach { dim ->
            content.addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(32)
                contentDescription = context.getString(R.string.tr_dim_cd, DomainText.tr(context, dim.name), DomainText.tr(context, dim.state.label), DomainText.tr(context, dim.sentence))
                isFocusable = true
                addView(PlIcon.view(context, dimensionIcon(dim.name), 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
                addView(TextView(context).apply { text = DomainText.tr(context, dim.name); textSize = 13f; setTextColor(palette.ink)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(0, -2, 1f))
                addView(TextView(context).apply { text = DomainText.tr(context, dim.state.label); textSize = 12f; setTextColor(palette.muted)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
            })
        }
        // Trend table: rows = metric, columns = 7 / 28 / 90 days.
        content.addView(tableRow(null, listOf(context.getString(R.string.tr_col_7), context.getString(R.string.tr_col_28), context.getString(R.string.tr_col_90))), LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content.addView(tableRow(context.getString(R.string.tr_rest), d.rest.map { cell(it) }, PlIcon.HEART).apply {
            tag = "trend_row_rest"; isClickable = true; isFocusable = true; minimumHeight = dp(48)
            setOnClickListener { showDaily(d, context.getString(R.string.tr_rest_daily), d.restDaily, d.rest, palette.pulse, zeroBased = false) }
        })
        content.addView(tableRow(context.getString(R.string.tr_steps), d.steps.map { cell(it) }, PlIcon.STEPS).apply {
            tag = "trend_row_steps"; isClickable = true; isFocusable = true; minimumHeight = dp(48)
            setOnClickListener { showDaily(d, context.getString(R.string.tr_steps_daily), d.stepsDaily, d.steps, palette.indicator) }
        })
        // Pre-specified hypothesis, one compact line; tap opens the scatter plot.
        val c = d.correlation
        val corr = if (c.rho == null) context.getString(R.string.tr_pairs_days, c.pairs.toString(), PairedCorrelation.MIN_PAIRS.toString())
            else "${nf.format(c.rho)} ρ · n=${c.pairs}"
        content.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            isClickable = true; isFocusable = true
            contentDescription = context.getString(R.string.tr_corr_cd, DomainText.tr(context, c.verdict.label), DomainText.tr(context, c.note))
            setOnClickListener { showScatter() }
            addView(PlIcon.view(context, PlIcon.TREND_LINK, 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(PlIcon.view(context, PlIcon.STEPS, 18), LayoutParams(dp(18), dp(18)))
                addView(TextView(context).apply { text = " → "; textSize = 13f; setTextColor(palette.ink) })
                addView(PlIcon.view(context, PlIcon.HEART, 18), LayoutParams(dp(18), dp(18)))
                addView(TextView(context).apply { text = context.getString(R.string.tr_next_day); textSize = 13f; setTextColor(palette.ink) })
            }, LayoutParams(0, -2, 1f))
            if (c.rho == null) {
                addView(PlIcon.view(context, PlIcon.HOURGLASS, 18), LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(4) })
                addView(TextView(context).apply { text = context.getString(R.string.tr_pairs_days, c.pairs.toString(), PairedCorrelation.MIN_PAIRS.toString()); textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, dp(8), 0) })
            } else addView(TextView(context).apply { text = corr; textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, dp(8), 0) })
            if (d.scatter.size >= 2) addView(PlIcon.view(context, PlIcon.TRENDS, 22), LayoutParams(dp(22), dp(22)))
        }, LayoutParams(-1, -2).apply { topMargin = dp(4) })
    
        addAtlas(d)
        addCoverage(d)
    }

    /** One shared window (7 / 28 / 90 days, default 28) for hero and atlas (#41). */
    private var atlasWindow = 28
    private val windowListeners = mutableListOf<() -> Unit>()

    private fun addWindowSegments() {
        val segments = LinearLayout(context).apply { orientation = HORIZONTAL; tag = "window_segments" }
        listOf(7, 28, 90).forEach { days ->
            segments.addView(TextView(context).apply {
                text = context.getString(when (days) { 7 -> R.string.tr_col_7; 28 -> R.string.tr_col_28; else -> R.string.tr_col_90 }); textSize = 12f; gravity = Gravity.CENTER; minimumHeight = dp(48); tag = "atlas_window_$days"
                isClickable = true; isFocusable = true; contentDescription = context.getString(R.string.tr_window_cd, days.toString())
                setOnClickListener {
                    atlasWindow = days
                    for (i in 0 until segments.childCount) (segments.getChildAt(i) as TextView).let { t -> styleSegment(t, t.tag == "atlas_window_$atlasWindow") }
                    windowListeners.forEach { it() }
                }
                styleSegment(this, atlasWindow == days)
            }, LayoutParams(0, -2, 1f))
        }
        content.addView(segments, LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun styleSegment(t: TextView, on: Boolean) {
        t.setTextColor(if (on) palette.accent else palette.muted); t.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Last 7 complete days (#265): per-row coverage, averages only over days with data, no score. */
    private fun addWeekSummary(d: Data) {
        val goal = context.getSharedPreferences(SpO2DisplaySettings.STORE, Context.MODE_PRIVATE)
            .getString("step_goal", null)?.trim()?.toIntOrNull()?.takeIf { it in 100..100_000 }
        val week = WeekSummary.build(d.stepsDaily, d.restDaily, d.today, goal) { v ->
            NumberFormat.getIntegerInstance(context.displayLocale()).format(Math.round(v))
        }
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "week_summary"; setPadding(0, dp(8), 0, dp(4)) }
        box.addView(TextView(context).apply {
            text = context.getString(R.string.tr_last7); textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink)
        })
        week.rows.forEach { row ->
            box.addView(LinearLayout(context).apply {
                orientation = VERTICAL; setPadding(0, dp(6), 0, 0); isFocusable = true
                tag = "week_row_${row.label}"; contentDescription = DomainText.weekRowDescription(context, row)
                addView(LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    addView(TextView(context).apply { text = DomainText.tr(context, row.label); textSize = 12f; setTextColor(palette.muted); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(0, -2, 1f))
                    addView(TextView(context).apply { text = row.value?.let { DomainText.tr(context, it) } ?: context.getString(R.string.tr_no_data); textSize = 13f; setTextColor(palette.ink); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
                })
                addView(TextView(context).apply { text = (if (row.value != null) DomainText.weekRowCoverage(context, row) else "") + (row.detail?.let { " · ${DomainText.tr(context, it)}" } ?: ""); textSize = 11f; setTextColor(palette.muted); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
            })
        }
        box.addView(TextView(context).apply { text = DomainText.tr(context, week.unavailable); textSize = 11f; setTextColor(palette.muted); setPadding(0, dp(6), 0, 0) })
        content.addView(box, LayoutParams(-1, -2))
    }

    /** Resting-pulse hero (#41 "Pulse Flow"): median, neutral delta, daily points with gaps, previous-period baseline. */
    private fun addRestingHero(d: Data) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "resting_hero"; setPadding(0, dp(8), 0, dp(4)) }
        box.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.RESTING_PULSE, 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(TextView(context).apply { text = context.getString(R.string.tr_rest_dev); textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
        })
        val value = TextView(context).apply { textSize = 28f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink); tag = "resting_hero_value" }
        val delta = TextView(context).apply { textSize = 12f; setTextColor(palette.muted); tag = "resting_hero_delta" }
        val message = TextView(context).apply { textSize = 11f; setTextColor(palette.muted); tag = "resting_hero_message" }
        val chart = RestingPulseChartView(context)
        box.addView(value, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        box.addView(delta)
        box.addView(chart, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        box.addView(message, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        box.isClickable = true; box.isFocusable = true; box.minimumHeight = dp(48)
        box.setOnClickListener { showDaily(d, context.getString(R.string.tr_rest_daily), d.restDaily, d.rest, palette.pulse, zeroBased = false) }
        fun refresh() {
            val trend = TrendAnalysis.analyze(d.restDaily, d.today, atlasWindow, 1.0)
            val m = RestingPulseHero.build(d.restDaily, d.today, atlasWindow, trend)
            value.text = m.valueText ?: "–"
            delta.text = m.deltaText?.let { DomainText.tr(context, it) }.orEmpty(); delta.visibility = if (m.deltaText == null) GONE else VISIBLE
            message.text = DomainText.tr(context, m.message)
            chart.show(m)
            box.contentDescription = DomainText.heroDescription(context, m) + context.getString(R.string.tr_tap_daily)
        }
        windowListeners.add { refresh() }
        refresh()
        content.addView(box)
    }

    /** Slim coverage block (#41): resting pulse and steps separately, never one shared percentage. */
    private fun addCoverage(d: Data) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "data_coverage_box" }
        box.addView(View(context).apply { setBackgroundColor(palette.track) }, LayoutParams(-1, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(10) })
        box.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.COVERAGE, 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(TextView(context).apply { text = context.getString(R.string.tr_coverage); textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
        })
        val rows = LinearLayout(context).apply { orientation = VERTICAL }
        box.addView(rows)
        fun refresh() {
            rows.removeAllViews()
            listOf(DataCoverage.line("Ruhepuls", d.restDaily, d.today, atlasWindow), DataCoverage.line("Schritte", d.stepsDaily, d.today, atlasWindow)).forEach { line ->
                rows.addView(LinearLayout(context).apply {
                    orientation = VERTICAL; setPadding(0, dp(6), 0, 0); contentDescription = DomainText.coverageDescription(context, line); isFocusable = true; tag = "coverage_${line.name}"
                    addView(TextView(context).apply { text = DomainText.tr(context, line.text()); textSize = 12f; setTextColor(palette.ink); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
                    addView(DeviceCoverageBar(context, line.fraction, if (line.name == "Ruhepuls") palette.pulse else palette.indicator, palette.track),
                        LayoutParams(-1, dp(6)).apply { topMargin = dp(4) })
                    line.hint()?.let { addView(TextView(context).apply { text = DomainText.tr(context, it); textSize = 11f; setTextColor(palette.muted); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }) }
                })
            }
        }
        windowListeners.add { refresh() }
        refresh()
        content.addView(box)
    }

    /** Thin proportional bar; the fill is exactly the share of covered days. */
    private class DeviceCoverageBar(context: Context, private val fraction: Float, private val fill: Int, private val track: Int) : View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val rect = android.graphics.RectF()
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun onDraw(canvas: android.graphics.Canvas) {
            val r = height / 2f
            paint.color = track; rect.set(0f, 0f, width.toFloat(), height.toFloat()); canvas.drawRoundRect(rect, r, r, paint)
            if (fraction > 0f) { paint.color = fill; rect.set(0f, 0f, maxOf(height.toFloat(), width * fraction.coerceIn(0f, 1f)), height.toFloat()); canvas.drawRoundRect(rect, r, r, paint) }
        }
    }

    /** Activity atlas (#41): calendar of daily step totals for the shared window. */
    private fun addAtlas(d: Data) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "activity_atlas_box" }
        box.addView(View(context).apply { setBackgroundColor(palette.track) }, LayoutParams(-1, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(10) })
        box.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.ACTIVITY_CALENDAR, 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(TextView(context).apply { text = context.getString(R.string.tr_atlas); textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
        })
        val summary = TextView(context).apply { textSize = 11f; setTextColor(palette.muted); setPadding(0, dp(4), 0, dp(6)); tag = "activity_atlas_summary" }
        val atlas = ActivityAtlasView(context) { cell ->
            androidx.appcompat.app.AlertDialog.Builder(context).setTitle(context.getString(R.string.tr_day_detail))
                .setMessage(DomainText.tr(context, ActivityAtlas.describe(cell))).setPositiveButton(context.getString(R.string.action_close), null).show()
        }
        val ownGoal = localStepGoal(context.getSharedPreferences(SpO2DisplaySettings.STORE, Context.MODE_PRIVATE).getString("step_goal", null))
        val regularity = TextView(context).apply { textSize = 11f; setTextColor(palette.ink); setPadding(0, 0, 0, dp(6)); tag = "activity_atlas_goal" }
        fun refresh() {
            val m = ActivityAtlas.build(d.stepsWithToday.ifEmpty { d.stepsDaily }, d.today, atlasWindow)
            atlas.show(m); summary.text = DomainText.tr(context, m.summary())
            val line = ownGoal?.let { GoalRegularity.build(d.stepsDaily, d.today, atlasWindow, it).text() }
            regularity.text = line?.let { DomainText.tr(context, it) } ?: ""; regularity.visibility = if (line == null) View.GONE else View.VISIBLE
        }
        box.addView(summary)
        box.addView(regularity)
        box.addView(atlas, LayoutParams(-1, -2))
        box.addView(TextView(context).apply {
            text = context.getString(R.string.tr_atlas_legend)
            textSize = 11f; setTextColor(palette.muted); setPadding(0, dp(8), 0, 0)
        })
        windowListeners.add { refresh() }
        refresh()
        content.addView(box)
    }

    private fun cell(r: TrendAnalysis.Result): String =
        if (r.direction == TrendAnalysis.Direction.UNKNOWN) "–\n${r.current.validDays}/${r.windowDays}"
        else "${compact(r.current.median!!)} ${r.direction.arrow}\n${r.current.validDays}/${r.windowDays}"

    /** Whole numbers from 100 up (7.762 steps), one decimal below (62,5 bpm) – keeps the 70 dp column single-line. */
    private fun compact(v: Double): String =
        if (Math.abs(v) >= 100) NumberFormat.getIntegerInstance(context.displayLocale()).format(Math.round(v)) else nf.format(v)

    private fun tableRow(label: String?, cells: List<String>, icon: PlIcon? = null): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            if (icon != null) addView(PlIcon.view(context, icon, 20), LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
            addView(TextView(context).apply { text = label.orEmpty(); textSize = 13f; setTextColor(palette.ink) })
        }, LayoutParams(0, -2, 1.3f))
        cells.forEach { c ->
            addView(TextView(context).apply {
                text = c; textSize = if (label == null) 11f else 12f
                setTextColor(if (label == null || c.startsWith("–")) palette.muted else palette.ink)
                gravity = Gravity.CENTER
                if (label == null) setTypeface(null, Typeface.BOLD)
                contentDescription = if (label == null) c else
                    "$label ${c.replace("\n", context.getString(R.string.tr_cell_days))}"
            }, LayoutParams(0, -2, 1f))
        }
    }

    /** Drilldown: daily values of the last 90 days with the 7/28-day medians (#41). */
    private fun showDaily(d: Data, title: String, values: Map<LocalDate, Double>, trends: List<TrendAnalysis.Result>, color: Int, zeroBased: Boolean = true) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(16), dp(8), dp(16), 0) }
        val present = (1..90).count { values.containsKey(d.today.minusDays(it.toLong())) }
        box.addView(DailyBarsView(context, values, d.today, 90, color, palette.ink, palette.track,
            listOf(context.getString(R.string.tr_median7) to trends[0].current.median, context.getString(R.string.tr_median28) to trends[1].current.median), zeroBased),
            LayoutParams(-1, -2))
        box.addView(TextView(context).apply {
            setTextColor(palette.muted); textSize = 12f; setPadding(0, dp(8), 0, 0)
            text = context.getString(R.string.tr_daily_note, present.toString())
        })
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle(title).setView(box).setPositiveButton(context.getString(R.string.action_close), null).show()
    }

    /** Full explanation moved out of the card body. */
    private fun showDetails(d: Data) {
        val text = buildString {
            append(context.getString(R.string.tr_details_head))
            d.overview.dimensions.forEach { append(context.getString(R.string.tr_details_dim, DomainText.tr(context, it.name), DomainText.tr(context, it.state.label), DomainText.tr(context, it.sentence))) }
            append(context.getString(R.string.tr_details_sizes))
            listOf(context.getString(R.string.tr_rest_daily) to d.rest, context.getString(R.string.tr_steps_daily) to d.steps).forEach { (title, list) ->
                append("\n\n$title")
                list.forEach { r ->
                    append(context.getString(R.string.tr_details_days, r.windowDays.toString()))
                    if (r.direction == TrendAnalysis.Direction.UNKNOWN) append(context.getString(R.string.tr_details_unrated, DomainText.tr(context, r.reason)))
                    else append(context.getString(R.string.tr_details_row, nf.format(r.current.median), r.direction.arrow, if (r.delta!! > 0) "+" else "", nf.format(r.delta), nf.format(r.baseline.median), r.current.validDays.toString(), r.windowDays.toString()))
                }
            }
            val c = d.correlation
            append(context.getString(R.string.tr_details_corr_head))
            if (c.rho == null) append(context.getString(R.string.tr_details_corr_nr, DomainText.tr(context, c.verdict.label), DomainText.tr(context, c.note)))
            else append(context.getString(R.string.tr_details_corr, DomainText.tr(context, c.verdict.label), nf.format(c.rho), nf.format(c.ciLow), nf.format(c.ciHigh), c.pairs.toString(), DomainText.tr(context, c.note)))
            append(context.getString(R.string.tr_details_foot))
        }
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle(context.getString(R.string.tr_methods_title))
            .setMessage(text).setPositiveButton(context.getString(R.string.action_ok), null).show()
    }

    private fun showScatter() {
        val pairs = data?.scatter.orEmpty()
        if (pairs.size < 2) return
        val view = ScatterView(context, pairs, context.getString(R.string.tr_scatter_x), context.getString(R.string.tr_scatter_y), palette.ink, palette.accent, palette.muted)
        val note = TextView(context).apply {
            setTextColor(palette.ink); textSize = 12f
            text = context.getString(R.string.tr_scatter_note, pairs.size.toString())
            setPadding(0, dp(8), 0, 0)
        }
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(16), dp(12), dp(16), 0); addView(view); addView(note) }
        data?.sensitivity?.let { s ->
            box.addView(TextView(context).apply {
                tag = "scatter_sensitivity"; textSize = 12f; setTextColor(palette.ink); setPadding(0, dp(8), 0, 0)
                // The model sentence is German with German number formatting (DomainText recognises it by that pattern); only the display is translated.
                text = DomainText.tr(context, s.text(NumberFormat.getNumberInstance(java.util.Locale.GERMANY).apply { maximumFractionDigits = 1 }))
            })
        }
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle(context.getString(R.string.tr_scatter_title))
            .setView(box).setPositiveButton(context.getString(R.string.action_close), null).show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        val worker = Executors.newSingleThreadExecutor()
    }
}

/**
 * Process-wide cache of the last trend computation (audit #6 A6-33). Valid for ten minutes, only on the calendar day it
 * was computed for, and dropped by [invalidate] whenever stored data changes (FIT import, backup restore).
 */
internal object TrendCache {
    const val MAX_AGE_MS = 10 * 60_000L
    @Volatile private var data: TrendCardView.Data? = null
    @Volatile private var storedAt = 0L

    fun store(result: TrendCardView.Data) { data = result; storedAt = System.currentTimeMillis() }
    fun invalidate() { data = null; storedAt = 0L }

    fun fresh(now: Long = System.currentTimeMillis(), today: LocalDate = LocalDate.now()): TrendCardView.Data? {
        val cached = data ?: return null
        return cached.takeIf { now - storedAt < MAX_AGE_MS && it.today == today }
    }
}

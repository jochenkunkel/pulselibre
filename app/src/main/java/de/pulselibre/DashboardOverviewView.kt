package de.pulselibre

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import java.text.NumberFormat
import java.util.Locale
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Read-only cards for locally received observations. No transport or persistence ownership. */
class DashboardOverviewView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val ink = palette.ink
    private val muted = palette.muted
    private val violet = palette.accent
    internal var openDay: ((String) -> Unit)? = null
    private var lastPulse: HeartRateGaugeModel? = null
    private var stepGoal: Int? = null

    init {
        orientation = VERTICAL
        // Drop a legacy manual mode left by older app versions; mode is now step-derived.
        context.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE).edit()
            .remove("pulse_context").apply()
    }

    private var lastMetrics: List<DashboardMetric> = emptyList()
    private var lastActivity: StepActivityEstimate = StepActivityEstimate.UNKNOWN
    private val displayPrefs get() = context.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE)

    fun render(allMetrics: List<DashboardMetric>, activity: StepActivityEstimate = StepActivityEstimate.UNKNOWN) {
        lastMetrics = allMetrics; lastActivity = activity
        removeAllViews()
        val metrics = applyLocalStepGoal(allMetrics,
            context.getSharedPreferences(SpO2DisplaySettings.STORE, Context.MODE_PRIVATE).getString("step_goal", null))
        stepGoal = metrics.firstOrNull { it.title == "Schrittziel" }?.value?.toIntOrNull()
        val overview = dashboardOverview(metrics)
        val keys = DashboardLayout.defaultKeys(overview.details.map { it.title })
        val entries = DashboardLayout.resolve(DashboardLayout.decode(displayPrefs.getString(LAYOUT_KEY, null)), keys)
        if (DashboardLayout.isDefault(entries, keys)) renderDefault(overview, activity) else renderCustom(overview, activity, entries)
        addCustomizeButton(entries)
    }

    private fun renderDefault(overview: DashboardOverview, activity: StepActivityEstimate) {
        addCardPair(overview.vitalSigns) { metric ->
            if (metric.title == "Herzfrequenz") pulseCard(metric, activity)
            else metricCard(metric, palette.oxygen)
        }
        addView(metricCard(overview.sleep, violet),
            LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        addNightOxygenCard()
        heading(context.getString(R.string.ov_today_glance))
        addStepContextChip()
        addView(stepCard(overview.steps, overview.stepGoal),
            LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        heading(context.getString(R.string.ov_more_health))
        overview.details.chunked(2).forEach { pair ->
            addCardPair(pair) { metric -> metricCard(metric, ink) }
        }
    }

    /** User layout (#267): favourites first under "Im Fokus", hidden tiles omitted, half-width tiles pair up. */
    private fun renderCustom(overview: DashboardOverview, activity: StepActivityEstimate, entries: List<DashboardLayout.Entry>) {
        val byKey = (overview.vitalSigns + overview.details).associateBy { it.title }
        val order = DashboardLayout.displayOrder(entries)
        if (order.isEmpty()) {
            addView(label(context.getString(R.string.ov_all_hidden), 13f, muted).apply {
                tag = "dashboard_all_hidden"
            }, LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(8) })
            return
        }
        fun renderRows(part: List<DashboardLayout.Entry>) {
            DashboardLayout.rows(part).forEach { row ->
                val key = row.first()
                when {
                    key == DashboardLayout.SLEEP -> addView(metricCard(overview.sleep, violet), LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                    key == DashboardLayout.NIGHT -> addNightOxygenCard()
                    key == DashboardLayout.STEPS -> {
                        addStepContextChip()
                        addView(stepCard(overview.steps, overview.stepGoal), LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                    }
                    else -> addCardPair(row.mapNotNull { byKey[it] }) { metric ->
                        when (metric.title) {
                            DashboardLayout.HEART -> pulseCard(metric, activity)
                            DashboardLayout.SPO2 -> metricCard(metric, palette.oxygen)
                            else -> metricCard(metric, ink)
                        }
                    }
                }
            }
        }
        val favorites = order.filter { it.favorite }
        if (favorites.isNotEmpty()) {
            heading(context.getString(R.string.ov_in_focus))
            renderRows(favorites)
            val rest = order.filterNot { it.favorite }
            if (rest.isNotEmpty()) { heading(context.getString(R.string.ov_more_tiles)); renderRows(rest) }
        } else renderRows(order)
    }

    private fun addCustomizeButton(entries: List<DashboardLayout.Entry>) {
        addView(label(context.getString(R.string.ov_customize), 13f, palette.accent, true).apply {
            tag = "dashboard_customize"
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            isClickable = true; isFocusable = true
            contentDescription = context.getString(R.string.ov_customize_cd)
            setOnClickListener {
                showDashboardCustomize(context, entries,
                    onSave = { saved ->
                        displayPrefs.edit().putString(LAYOUT_KEY, DashboardLayout.encode(saved)).apply()
                        render(lastMetrics, lastActivity)
                    },
                    onReset = {
                        displayPrefs.edit().remove(LAYOUT_KEY).apply()
                        render(lastMetrics, lastActivity)
                    })
            }
        }, LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(8) })
    }

    private fun stepCard(steps: DashboardMetric, goal: DashboardMetric): View {
        val integers = NumberFormat.getIntegerInstance(Locale.GERMANY)
        val count = steps.value.toLongOrNull()?.takeIf { it >= 0 }
        val target = goal.value.toLongOrNull()?.takeIf { it > 0 }
        val progress = stepGoalProgress(steps, goal)
        val metric = steps.copy(title = "Schritte heute",
            value = count?.let { context.getString(R.string.ov_steps_count, integers.format(it)) } ?: MetricText.NO_DATA)
        return card(metric).apply {
            // Background layer: faint 15-minute bars behind all texts, clipped to the rounded tile (#122).
            val bars = StepBarsView(context, palette.accent)
            addView(bars, 0, android.widget.FrameLayout.LayoutParams(-1, -1))
            clipToOutline = true
            val trendNote = label("", 11f, muted).apply { tag = "dashboard_step_trend_note"; visibility = View.GONE }
            var baseDescription = ""   // the tile's own description without the trend sentence; set once the tile is built
            fun showSteps(series: StepIntervals.Series?) {
                bars.show(series)
                if (series == null) return
                if (series.observed > 0) {
                    contentDescription = context.getString(R.string.ov_steps_trend_partial_cd, baseDescription, series.observed, series.values.size)
                    trendNote.visibility = View.GONE
                } else {
                    contentDescription = context.getString(R.string.ov_steps_trend_none_cd, baseDescription)
                    trendNote.text = context.getString(R.string.ov_steps_trend_none)
                    trendNote.visibility = View.VISIBLE
                }
            }
            stepBarsTarget = ::showSteps
            if (cachedStepSeries != null) showSteps(cachedStepSeries)
            if (cachedStepSeries == null || System.currentTimeMillis() - cachedStepAt > STEP_CACHE_MS) loadStepSeries()
            // Layout after design/step-tile-background-bars-mockup.svg: title row with a mint percent pill,
            // big number with a smaller unit, thin goal bar, goal line.
            val titleRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            titleRow.addView(header(metric), LayoutParams(0, -2, 1f))
            val percentText = progress?.let {
                "≈ " + NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 0 }.format(it) + " %"
            }
            if (percentText != null) titleRow.addView(label(percentText, 13f, if (palette.dark) 0xFF0E3B33.toInt() else 0xFF0E5A49.toInt(), true).apply {
                tag = "dashboard_step_percent"
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = GradientDrawable().apply { setColor(DesignTokens.MINT); cornerRadius = dp(16).toFloat() }
                contentDescription = null
            }, LayoutParams(-2, -2).apply { marginEnd = dp(34) })
            content.addView(titleRow)
            val big = android.text.SpannableString(count?.let { context.getString(R.string.ov_steps_count, integers.format(it)) } ?: context.getString(R.string.metric_no_data))
            if (count != null) {
                val split = integers.format(count).length
                big.setSpan(android.text.style.AbsoluteSizeSpan(38, true), 0, split, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                big.setSpan(android.text.style.AbsoluteSizeSpan(16, true), split, big.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                big.setSpan(android.text.style.ForegroundColorSpan(muted), split, big.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            content.addView(label("", 22f, ink, true).apply { text = big; tag = "dashboard_step_value" },
                LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val targetSource = context.getString(if (goal.note == LOCAL_STEP_GOAL_NOTE) R.string.ov_step_goal_own else R.string.ov_step_goal_device)
            val reached = stepGoalStatus(steps, goal)?.let { DomainText.tr(context, it) }
            val targetText = target?.let { context.getString(R.string.ov_step_target, targetSource, integers.format(it)) + (reached?.let { r -> " · $r" } ?: "") }
                ?: context.getString(R.string.ov_step_target_none)
            contentDescription = "${MetricText.title(context, metric.title)}. ${MetricText.value(context, metric.value)}. $targetText."
            baseDescription = contentDescription.toString()
            if (progress != null) {
                val percentValue = progress
                val percent = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
                    maximumFractionDigits = 1
                }.format(progress)
                contentDescription = context.getString(R.string.ov_step_percent_cd, contentDescription, percent)
                baseDescription = contentDescription.toString()
                content.addView(android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    tag = "dashboard_step_progress"
                    max = 1000
                    this.progress = (percentValue.coerceIn(0.0, 100.0) * 10).toInt()
                    progressTintList = android.content.res.ColorStateList.valueOf(palette.indicator)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LayoutParams(-1, dp(10)).apply { topMargin = dp(10) })
            }
            content.addView(label(targetText, 13f, muted), LayoutParams(-1, -2).apply { topMargin = dp(6) })
            content.addView(trendNote, LayoutParams(-1, -2).apply { topMargin = dp(4) })
            if (cachedStepSeries != null) showSteps(cachedStepSeries)
        }
    }

    private var stepBarsTarget: ((StepIntervals.Series?) -> Unit)? = null

    /** Reads today's step counter observations once (cached) and fills whichever step tile is on screen. */
    private fun loadStepSeries() {
        if (stepLoading) return
        stepLoading = true
        val app = context.applicationContext
        Thread {
            val series = try {
                val zone = java.time.ZoneId.systemDefault()
                val now = java.time.Instant.now()
                val day = DashboardDay(java.time.LocalDate.now(zone), zone)
                val read = DashboardDayReader(app).read(DashboardMeasurement.STEPS, day, now)
                StepIntervals.compute(read.points, day)
            } catch (_: Exception) { null }
            cachedStepSeries = series
            cachedStepAt = System.currentTimeMillis()
            stepLoading = false
            post { if (isAttachedToWindow) stepBarsTarget?.invoke(series) }
        }.start()
    }

    private fun pulseCard(metric: DashboardMetric, activity: StepActivityEstimate): MetricCard {
        val model = HeartRateGaugeModel.from(metric)
        val previous = lastPulse
        lastPulse = model
        val card = card(metric)
        card.content.addView(header(metric))
        val gauge = HeartRateGaugeView(context)
        gauge.show(model, activity)
        card.content.addView(gauge, LayoutParams(-1, -2))
        card.content.addView(label(DomainText.activityCaption(context, activity), 11f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(2) })
        attachInsights(card, metric.copy(title = DashboardMeasurement.PULSE.title), card.content.let { palette.pulse })
        if (previous?.bpm != null && model.bpm != null && previous.bpm != model.bpm) {
            gauge.post { if (gauge.isAttachedToWindow) gauge.show(model, activity, previous.fraction) }
        }
        return card
    }

    private fun addNightOxygenCard() {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(palette.card)
                cornerRadius = dp(18).toFloat()
                UnvalidatedFeatures.mark(this, resources.displayMetrics.density, palette.dark)
            }
            tag = "dashboard_night_oxygen_insights"
            contentDescription = context.getString(R.string.ov_night_cd)
        }
        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.INFO_TIME, 24, palette.accent),
                LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
            addView(label(context.getString(R.string.ov_night_title), 15f, ink, true), LayoutParams(0, -2, 1f))
            addView(android.widget.ImageView(context).apply {
                setImageDrawable(PlIcon.drawable(context, PlIcon.HOURGLASS, 22, palette.muted))
                contentDescription = context.getString(R.string.ov_night_loading_cd)
                tag = "night_status_icon"
            }, LayoutParams(dp(22), dp(22)))
        }
        card.addView(titleRow)
        card.addView(View(context).apply { setBackgroundColor(palette.track) },
            LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(10) })
        val insightRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val nightReason = label("", 10f, muted)
        fun fill(night: NocturnalOxygen.Summary?) {
            insightRow.removeAllViews()
            if (night == null) {
                addNightIndicator(insightRow, PlIcon.OXYGEN, "SpO₂", context.getString(R.string.ov_night_loading), palette.muted)
                return
            }
            val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY).apply { maximumFractionDigits = 1 }
            val notComputable = context.getString(R.string.ov_not_computable)
            addNightIndicator(insightRow, PlIcon.RECOVERY, context.getString(R.string.ov_night_hours),
                java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(night.samples), palette.muted,
                description = context.getString(R.string.ov_night_samples_cd, night.samples.toString()))
            addNightIndicator(insightRow, PlIcon.HOURGLASS, context.getString(R.string.ov_night_coverage),
                night.coveragePercent?.let { "${nf.format(it)} %" } ?: notComputable, palette.muted)
            addNightIndicator(insightRow, PlIcon.SIGNAL, "T90",
                if (!night.computable) notComputable else context.getString(R.string.ov_night_minutes, nf.format(night.minutesBelow90)), palette.muted)
            addNightIndicator(insightRow, PlIcon.BREATHING, "ODI3 / ODI4",
                if (!night.computable) notComputable
                else night.odi3?.let { context.getString(R.string.ov_night_odi_value, nf.format(it), nf.format(night.odi4)) } ?: notComputable, palette.muted)
            // The hourglass only belongs to the loading state.
            findViewWithTag<View>("night_status_icon")?.apply { visibility = View.GONE; contentDescription = null }
            if (night.reason != "ok") nightReason.text = DomainText.nightReason(context, night.reason)
        }
        // The overview re-renders whenever readings arrive; show the cached result at once and let the
        // single background load update whichever card is currently on screen.
        fill(cachedNight)
        card.addView(insightRow)
        card.addView(nightReason, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        nightFill = ::fill
        if (cachedNight == null || System.currentTimeMillis() - cachedNightAt > NIGHT_CACHE_MS) loadNight()
        card.addView(label(context.getString(R.string.ov_night_footer), 10f, muted).apply {
            gravity = Gravity.END
            contentDescription = context.getString(R.string.ov_night_footer_cd)
        }, LayoutParams(-1, -2).apply { topMargin = dp(8) })
        card.isFocusable = true
        card.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        addView(card, LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
    }

    private var nightFill: ((NocturnalOxygen.Summary?) -> Unit)? = null

    private fun loadNight() {
        if (nightLoading) return
        nightLoading = true
        val app = context.applicationContext
        Thread {
            val summary = try {
                val night = NightOxygenReader(app).read(java.time.Instant.now(), java.time.ZoneId.systemDefault())
                NocturnalOxygen.summarize(night.samples, night.start, night.end)
            } catch (_: Exception) { NocturnalOxygen.Summary(false, "Nachtserie nicht lesbar.", 0, null, null, null, null) }
            cachedNight = summary
            cachedNightAt = System.currentTimeMillis()
            nightLoading = false
            post { if (isAttachedToWindow) nightFill?.invoke(summary) }
        }.start()
    }

    private companion object {
        private const val LAYOUT_KEY = "layout_v1"
        const val INSIGHT_CACHE_MS = 60_000L
        val insightCache = java.util.concurrent.ConcurrentHashMap<String, Pair<TileInsight, Long>>()
        const val STEP_CACHE_MS = 60_000L
        @Volatile var cachedStepSeries: StepIntervals.Series? = null
        @Volatile var cachedStepAt = 0L
        @Volatile var stepLoading = false
        const val NIGHT_CACHE_MS = 5 * 60_000L
        @Volatile var cachedNight: NocturnalOxygen.Summary? = null
        @Volatile var cachedNightAt = 0L
        @Volatile var nightLoading = false
    }

    private fun addNightIndicator(parent: LinearLayout, icon: PlIcon, title: String, value: String, color: Int, description: String? = null) {
        val cell = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(3), dp(4), dp(3))
            contentDescription = description ?: "$title: $value"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            addView(android.widget.ImageView(context).apply {
                setImageDrawable(PlIcon.drawable(context, icon, 22, color))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(22), dp(22)))
            addView(label(title, 11f, muted, true).apply { gravity = Gravity.CENTER }, LayoutParams(-1, -2))
            addView(label(value, 12f, ink).apply { gravity = Gravity.CENTER }, LayoutParams(-1, -2).apply { topMargin = dp(2) })
        }
        parent.addView(cell, LayoutParams(0, -2, 1f))
    }

    private fun addStepContextChip() {
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                setColor(palette.tinted)
                cornerRadius = dp(12).toFloat()
            }
            isFocusable = true
            contentDescription = context.getString(R.string.ov_step_day_state_cd)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            val stepIconParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(5) }
            addView(PlIcon.view(context, PlIcon.STEPS, 18, palette.indicator), stepIconParams)
            addView(label(context.getString(R.string.ov_step_day_state), 11f, muted, true), LinearLayout.LayoutParams(-2, -2))
        }
        chip.getChildAt(0).importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        chip.getChildAt(1).importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(chip, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(12) })
    }

    private fun addCardPair(metrics: List<DashboardMetric>, create: (DashboardMetric) -> View) {
        val stacked = resources.configuration.fontScale >= 1.3f
        val row = LinearLayout(context).apply { orientation = if (stacked) VERTICAL else HORIZONTAL }
        metrics.forEachIndexed { index, metric ->
            val view = create(metric)
            row.addView(view,
                if (stacked || metrics.size == 1) LayoutParams(-1, -2).apply {
                    if (index != metrics.lastIndex) bottomMargin = dp(8)
                } else LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    // Tiles of one row share the row height (tallest wins) instead of ragged heights.
                    if (index != metrics.lastIndex) marginEnd = dp(8)
                })
        }
        addView(row, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun metricCard(metric: DashboardMetric, accent: Int) = card(metric).apply {
        content.addView(header(metric))
        if (metric.title == "Sauerstoffsättigung · SpO₂") {
            val percent = Regex("^[0-9]+(?= %$)").find(metric.value)?.value?.toIntOrNull()
            val valueLayer = android.widget.FrameLayout(context).apply {
                contentDescription = metric.value
            }
            val displaySettings = SpO2DisplaySettings.read(context)
            valueLayer.addView(SpO2LungView(context, percent, displaySettings.colorFor(percent, palette), palette.track),
                android.widget.FrameLayout.LayoutParams(-1, dp(76), Gravity.CENTER))
            valueLayer.addView(label(MetricText.value(context, metric.value), 22f, accent, true).apply {
                gravity = Gravity.CENTER
                tag = "spo2_value"
            }, android.widget.FrameLayout.LayoutParams(-1, -1))
            content.addView(valueLayer, LayoutParams(-1, dp(76)).apply { topMargin = dp(4) })
        } else {
            content.addView(label(MetricText.value(context, metric.value), 22f, accent, true),
                LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
        val profile = MetricReferenceProfile.forMetric(metric, stepGoal)
        val value = Regex("-?[0-9]+") .find(metric.value)?.value?.toIntOrNull()
        if (profile != null && value != null) {
            content.addView(MetricReferenceScaleView(context, profile, value, accent, ink, muted,
                palette.track, palette.muted), LayoutParams(-1, dp(40)).apply { topMargin = dp(8) })
        }
        attachInsights(this, metric, accent)
    }

    /** Extra information under the value: today's range, 24 h sparkline, 7-day same-time average (loaded in the background). */
    private fun attachInsights(card: MetricCard, metric: DashboardMetric, accent: Int) {
        val measurement = DashboardMeasurement.entries.firstOrNull { it.title == metric.title } ?: return
        val box = LinearLayout(context).apply { orientation = VERTICAL; visibility = View.GONE; tag = "tile_insights_${metric.title}" }
        card.content.addView(box, LayoutParams(-1, -2).apply { topMargin = dp(8) })
        fun render(i: TileInsight) {
            box.removeAllViews()
            if (i.count == 0 && i.fit == null) { box.visibility = View.GONE; return }
            val unit = if (measurement.unit.startsWith("Geräteskala")) "" else measurement.unit
            val nf = TileInsightCalc
            val range = when {
                i.cumulative -> context.getString(R.string.ov_today_value, nf.format(i.current ?: 0.0, unit))
                i.min == i.max -> context.getString(R.string.ov_today_single, nf.format(i.average ?: 0.0, unit), valuesText(i.count))
                else -> context.getString(R.string.ov_today_range, nf.format(i.average ?: 0.0, ""), nf.format(i.min ?: 0.0, ""), nf.format(i.max ?: 0.0, "")) +
                    if (unit.isBlank()) "" else " $unit"
            }
            if (i.count > 0 && !i.cumulative) box.addView(label(range, 11f, muted), LayoutParams(-1, -2))   // counters: the big value already says it
            // a line needs at least two hours with data; otherwise it would only be an empty axis
            if (i.count > 0 && i.hourly.count { it != null } >= 2) box.addView(SparkView(context, accent, palette.track).apply {
                show(i.hourly, java.time.LocalTime.now().hour)
            }, LayoutParams(-1, dp(34)).apply { topMargin = dp(4) })
            // #334: band history (FIT) has its own line and sparkline; its clock is the band's measurement time.
            i.fit?.let { fit ->
                val dayLabel = if (fit.date == java.time.LocalDate.now()) context.getString(R.string.ov_today_word) else fit.date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM."))
                val text = context.getString(R.string.ov_band_history, dayLabel, nf.format(fit.average, ""), nf.format(fit.min, ""), nf.format(fit.max, ""), unit, valuesText(fit.count))
                box.addView(label(text, 11f, muted).apply { tag = "tile_fit_${metric.title}" },
                    LayoutParams(-1, -2).apply { topMargin = dp(if (i.count > 0) 8 else 0) })
                if (fit.hourly.count { it != null } >= 2) box.addView(SparkView(context, accent, palette.track).apply {
                    alpha = 0.7f; tag = "tile_fit_spark_${metric.title}"; show(fit.hourly, java.time.LocalTime.now().hour)
                }, LayoutParams(-1, dp(34)).apply { topMargin = dp(4) })
            }
            i.bands?.let { (hi, mid, lo) ->
                box.addView(object : View(context) {
                    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
                    private val r = android.graphics.RectF()
                    override fun onDraw(c: Canvas) {
                        val w = width.toFloat(); var x = 0f
                        listOf(hi to palette.indicator, mid to palette.oxygen, lo to Color.rgb(205, 57, 65)).forEach { (share, col) ->
                            if (share <= 0.0) return@forEach
                            val seg = (share / 100.0).toFloat() * w
                            p.color = col; p.alpha = 200; r.set(x, 0f, x + seg, height.toFloat()); c.drawRoundRect(r, height / 2f, height / 2f, p); x += seg
                        }
                    }
                }.apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LayoutParams(-1, dp(7)).apply { topMargin = dp(8) })
                box.addView(label(context.getString(R.string.ov_spo2_bands, hi.toInt(), mid.toInt(), lo.toInt()), 10f, muted),
                    LayoutParams(-1, -2).apply { topMargin = dp(3) })
            }
            if (i.count == 0) { box.visibility = View.VISIBLE; return }   // only band history today: no live week comparison
            val week = i.weekAtThisTime
            val delta = i.deltaToWeek
            val deltaText = delta?.let { d -> "  ${if (d > 0.05) "▲" else if (d < -0.05) "▼" else "●"} ${if (d > 0) "+" else ""}${nf.format(d, "")}" } ?: ""
            val weekText = if (week == null) context.getString(R.string.ov_week_pending, i.weekDays, TileInsightCalc.MIN_WEEK_DAYS)
                else context.getString(R.string.ov_week_now, nf.format(week, unit)) + deltaText
            box.addView(label(weekText, 11f, if (week == null) muted else ink, week != null).apply { if (week != null) contentDescription = context.getString(R.string.ov_week_now_cd, nf.format(week, unit) + deltaText) },
                LayoutParams(-1, -2).apply { topMargin = dp(6) })
            box.visibility = View.VISIBLE
        }
        val key = metric.title
        insightCache[key]?.takeIf { System.currentTimeMillis() - it.second < INSIGHT_CACHE_MS }?.let { render(it.first); return }
        Thread {
            val insight = try { TileInsightReader(context.applicationContext).read(measurement, java.time.ZoneId.systemDefault(), java.time.Instant.now()) }
                catch (_: Exception) { null }
            if (insight != null) insightCache[key] = insight to System.currentTimeMillis()
            if (insight != null) post { if (isAttachedToWindow) render(insight) }
        }.start()
    }

    private fun valuesText(count: Int) = context.getString(if (count == 1) R.string.ov_values_one else R.string.ov_values_many, count)

    private fun card(metric: DashboardMetric) = MetricCard(metric)

    private fun receptionLabel(f: ReceptionFreshness) = when (f) {
        ReceptionFreshness.UNKNOWN -> context.getString(R.string.reception_unknown)
        ReceptionFreshness.FRESH -> context.getString(R.string.reception_fresh)
        ReceptionFreshness.RECENT -> context.getString(R.string.reception_recent)
        ReceptionFreshness.DELAYED -> context.getString(R.string.reception_delayed)
        ReceptionFreshness.OLD -> context.getString(R.string.reception_old)
    }

    private inner class MetricCard(metric: DashboardMetric) : android.widget.FrameLayout(context) {
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        init {
            // Thin frame instead of a dot: its colour is the age of the last reception; tiles whose source is
            // not validated get a second, violet frame inside it.
            val freshness = ReceptionFreshness.of(metric.receivedAt, Instant.now())
            val frameColor = when (freshness) {
                ReceptionFreshness.FRESH -> Color.rgb(31, 166, 113)
                ReceptionFreshness.RECENT -> Color.rgb(207, 166, 19)
                ReceptionFreshness.DELAYED -> Color.rgb(228, 112, 30)
                else -> palette.track
            }
            val base = GradientDrawable().apply {
                setColor(palette.card); cornerRadius = dp(18).toFloat(); setStroke(dp(1) + 1, frameColor)
            }
            val unvalidated = UnvalidatedFeatures.isUnvalidated(metric.title)
            background = if (unvalidated) {
                val inner = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT); cornerRadius = dp(15).toFloat()
                    setStroke(dp(1) + 1, UnvalidatedFeatures.color(palette.dark))
                }
                android.graphics.drawable.LayerDrawable(arrayOf(base, inner)).apply { setLayerInset(1, dp(4), dp(4), dp(4), dp(4)) }
            } else base
            tag = "dashboard_metric_${metric.title}"
            contentDescription = context.getString(R.string.metric_last_reception_cd, MetricText.title(context, metric.title), MetricText.value(context, metric.value), receptionLabel(freshness))
            if (unvalidated) contentDescription = contentDescription.toString() + DomainText.tr(context, UnvalidatedFeatures.ACCESSIBILITY_SUFFIX)
            ViewCompat.setScreenReaderFocusable(this, false)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val show = openDay
            if (show != null && metric.title != "Ground Rules") {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                isFocusable = true
                setOnClickListener { show(metric.title) }
            }
            addView(content, android.widget.FrameLayout.LayoutParams(-1, -2))
            val indicator = ReceptionIndicatorView(context, metric, stepGoal).apply {
                tag = "reception_indicator_${metric.title}"
            }
            addView(indicator, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(4)
                marginEnd = dp(4)
            })
        }
    }

    private fun header(metric: DashboardMetric) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPaddingRelative(0, 0, dp(18), 0)
        addView(PlIcon.view(context, DashboardMetricIconView.iconFor(metric.title), 20, iconColor(metric.title)).apply {
            tag = "dashboard_metric_icon_${metric.title}"
        }, LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
        // The long SpO₂ title broke mid-word in a half-width card; show the short name, keep the full one for accessibility.
        addView(label(MetricText.shortTitle(context, metric.title), 12f, muted, true).apply {
            if (metric.title != "Schritte heute") {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            contentDescription = MetricText.title(context, metric.title)
        }, LayoutParams(0, -2, 1f))
    }

    /** Short visible names; long words broke mid-word in half-width cards. The full title stays the accessibility name. */
    private fun displayTitle(title: String) = when (title) {
        "Sauerstoffsättigung · SpO₂" -> "SpO₂"
        "Moderate Intensitätsminuten" -> "Intensität moderat"
        "Intensive Intensitätsminuten" -> "Intensität hoch"
        "Kalorien gesamt (Gerätewert)" -> "Kalorien gesamt"
        "Kalorien aktiv (Gerätewert)" -> "Kalorien aktiv"
        else -> title
    }

    private fun iconColor(title: String) = when {
        title.contains("Herzfrequenz", true) || title.contains("Ruhepuls", true) -> palette.pulse
        title.contains("SpO₂", true) || title.contains("Sauerstoff", true) -> palette.oxygen
        title.contains("Schritt", true) -> palette.indicator
        title.contains("Schlaf", true) -> palette.accent
        else -> palette.accent
    }

    private fun heading(title: String) {
        val view = label(title, 17f, ink, true)
        ViewCompat.setAccessibilityHeading(view, true)
        addView(view, LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })
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

/** Subdued oxygen-fill silhouette. It conveys only the received percent, never signal quality or a diagnosis.
 * Blueprint O01 defines finger-oximeter context, not clinical bands for CIRQA wrist readings.
 */
@SuppressLint("ViewConstructor")
internal class SpO2LungView(
    context: Context,
    percent: Int?,
    private val neutralColor: Int,
    private val trackColor: Int
) : View(context) {
    val fillFraction: Float? = percent?.takeIf { it in 1..100 }?.div(100f)
    val fillColor: Int = neutralColor
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lung = android.graphics.Path()

    init {
        contentDescription = if (fillFraction != null) context.getString(R.string.ov_lung_cd, percent) else context.getString(R.string.ov_lung_nodata_cd)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val scale = minOf(w / 64f, h / 76f)
        val left = (w - 64f * scale) / 2f
        val top = (h - 76f * scale) / 2f
        val save = canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        lung.reset()
        // Trachea and paired lung lobes, drawn as one continuous silhouette.
        lung.moveTo(28f, 5f); lung.lineTo(36f, 5f); lung.lineTo(36f, 29f)
        lung.cubicTo(40f, 28f, 46f, 18f, 53f, 21f)
        lung.cubicTo(61f, 24f, 63f, 40f, 60f, 57f)
        lung.cubicTo(58f, 69f, 50f, 74f, 43f, 70f)
        lung.cubicTo(37f, 67f, 37f, 56f, 36f, 45f)
        lung.lineTo(32f, 40f); lung.lineTo(28f, 45f)
        lung.cubicTo(27f, 56f, 27f, 67f, 21f, 70f)
        lung.cubicTo(14f, 74f, 6f, 69f, 4f, 57f)
        lung.cubicTo(1f, 40f, 3f, 24f, 11f, 21f)
        lung.cubicTo(18f, 18f, 24f, 28f, 28f, 29f)
        lung.close()
        paint.style = Paint.Style.FILL
        paint.color = trackColor
        paint.alpha = 38
        canvas.drawPath(lung, paint)
        paint.alpha = 255
        fillFraction?.let { fraction ->
            val fillSave = canvas.save()
            canvas.clipPath(lung)
            canvas.clipRect(0f, 76f * (1f - fraction), 64f, 76f)
            paint.color = fillColor
            paint.alpha = 74
            canvas.drawRect(0f, 0f, 64f, 76f, paint)
            paint.alpha = 255
            canvas.restoreToCount(fillSave)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = fillColor
        paint.alpha = 88
        canvas.drawPath(lung, paint)
        canvas.restoreToCount(save)
    }
}

/** Small, individually accessible tap target; only the drawn dot changes with reception age. */
@SuppressLint("ViewConstructor")
private class ReceptionIndicatorView(
    context: Context,
    private val metric: DashboardMetric,
    private val stepGoal: Int?
) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val palette = AppPalette.of(context)
    private val state = ReceptionFreshness.of(metric.receivedAt, Instant.now())

    init {
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.ov_reception_cd, MetricText.title(context, metric.title), state.label())
        setOnClickListener { showDetails() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Quiet info icon only (PulsLibre icon system); reception age is shown by the tile frame colour.
        val d = icon ?: androidx.core.content.ContextCompat.getDrawable(context, PlIcon.INFO.res)!!.mutate().also {
            androidx.core.graphics.drawable.DrawableCompat.setTint(it, palette.muted); icon = it
        }
        val size = (16f * density).toInt()
        val left = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) (6f * density).toInt() else width - size - (6f * density).toInt()
        val top = (6f * density).toInt()
        d.setBounds(left, top, left + size, top + size)
        d.alpha = 190
        d.draw(canvas)
    }

    private var icon: android.graphics.drawable.Drawable? = null

    private fun showDetails() {
        val now = Instant.now()
        val currentState = ReceptionFreshness.of(metric.receivedAt, now)
        val timestamp = metric.receivedAt
        val received = timestamp?.let {
            val local = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss z")
                .withZone(ZoneId.systemDefault()).format(it)
            val seconds = Duration.between(it, now).seconds.coerceAtLeast(0)
            val age = when {
                seconds < 60 -> context.getString(R.string.ov_recv_seconds, seconds.toInt())
                seconds < 3600 -> context.getString(R.string.ov_recv_minutes, (seconds / 60).toInt())
                else -> context.getString(R.string.ov_recv_hours, (seconds / 3600).toInt())
            }
            context.getString(R.string.ov_recv_at, local, age)
        } ?: context.getString(R.string.ov_recv_none)
        val details = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(8))
        }
        fun row(symbol: String, text: String, color: Int = palette.ink, url: String? = null) {
            val icon = MetricInfoIconMapping.forSymbol(symbol)
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            line.addView(PlIcon.view(context, icon, 18, if (url == null) palette.muted else palette.accent),
                LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(10) })
            line.addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(if (url == null) color else palette.accent)
                if (url != null) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                        catch (_: Exception) { /* Keep the source readable if no browser is available. */ }
                    }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            details.addView(line)
        }
        val profile = MetricReferenceProfile.forMetric(metric, stepGoal)
        row("■", "${MetricText.title(context, metric.title)}  ·  ${MetricText.value(context, metric.value)}")
        if (profile != null) {
            row("▬", context.getString(R.string.ov_scale, profile.minimum.toString(), profile.maximum.toString(), DomainText.tr(context, profile.unit)), palette.muted)
            if (profile.referenceMinimum != null && profile.referenceMaximum != null) {
                row("▰", "${profile.referenceMinimum}–${profile.referenceMaximum} ${DomainText.tr(context, profile.unit)}  ·  ${DomainText.tr(context, profile.referenceLabel ?: "")}", palette.muted)
            }
            profile.sourceName?.let { row("↗", context.getString(R.string.ov_source, it), palette.accent, profile.sourceUrl) }
            if (metric.title == "Sauerstoffsättigung · SpO₂") {
                val displayBands = SpO2DisplaySettings.read(context)
                row("▰", context.getString(R.string.ov_preview, displayBands.bandLabel(context)), palette.muted)
                row("⏳", context.getString(if (displayBands.colorBandsEnabled) R.string.ov_spo2_scale_on else R.string.ov_spo2_scale_off), palette.muted)
            }
            row("⚠", DomainText.tr(context, profile.caveat), palette.muted)
        } else if (metric.title == "Moderate Intensitätsminuten" || metric.title == "Intensive Intensitätsminuten") {
            row("▱", context.getString(R.string.ov_who), palette.muted)
            row("↗", context.getString(R.string.ov_source, context.getString(R.string.ov_who_name)), palette.accent, "https://www.who.int/initiatives/behealthy/physical-activity")
            row("⚠", context.getString(R.string.ov_cirqa_period), palette.muted)
        } else {
            row("▱", DomainText.tr(context, metric.note), palette.muted)
            row("⚠", context.getString(R.string.ov_no_norm), palette.muted)
        }
        row("◷", received, palette.muted)
        row("●", context.getString(R.string.ov_reception_state, currentState.label()), palette.muted)
        row("i", context.getString(R.string.ov_dot_hint), palette.muted)
        AlertDialog.Builder(context).setTitle("ⓘ  ${MetricText.title(context, metric.title)}").setView(details)
            .setPositiveButton(context.getString(R.string.action_close), null).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    fun ReceptionFreshness.label() = when (this) {
        ReceptionFreshness.UNKNOWN -> context.getString(R.string.reception_unknown)
        ReceptionFreshness.FRESH -> context.getString(R.string.reception_fresh)
        ReceptionFreshness.RECENT -> context.getString(R.string.reception_recent)
        ReceptionFreshness.DELAYED -> context.getString(R.string.reception_delayed)
        ReceptionFreshness.OLD -> context.getString(R.string.reception_old)
    }
}

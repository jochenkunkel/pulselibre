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

    fun render(metrics: List<DashboardMetric>, activity: StepActivityEstimate = StepActivityEstimate.UNKNOWN) {
        removeAllViews()
        stepGoal = metrics.firstOrNull { it.title == "Schrittziel" }?.value?.toIntOrNull()
        val overview = dashboardOverview(metrics)
        addView(label("Deine Messwerte · lokal gespeichert", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addView(label(UnvalidatedFeatures.LEGEND, 11f, UnvalidatedFeatures.color(palette.dark)).apply {
            tag = "dashboard_unvalidated_legend"
        }, LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        addCardPair(overview.vitalSigns) { metric ->
            if (metric.title == "Herzfrequenz") pulseCard(metric, activity)
            else metricCard(metric, palette.oxygen)
        }
        addView(metricCard(overview.sleep, violet),
            LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        addNightOxygenCard()
        heading("Heute auf einen Blick")
        addStepContextChip()
        addView(stepCard(overview.steps, overview.stepGoal),
            LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        heading("Weitere Gesundheitswerte")
        overview.details.chunked(2).forEach { pair ->
            addCardPair(pair) { metric -> metricCard(metric, ink) }
        }
    }

    private fun stepCard(steps: DashboardMetric, goal: DashboardMetric): View {
        val integers = NumberFormat.getIntegerInstance(Locale.GERMANY)
        val count = steps.value.toLongOrNull()?.takeIf { it >= 0 }
        val target = goal.value.toLongOrNull()?.takeIf { it > 0 }
        val progress = stepGoalProgress(steps, goal)
        val metric = steps.copy(title = "Schritte heute",
            value = count?.let { "${integers.format(it)} Schritte" } ?: "Keine Messdaten")
        return card(metric).apply {
            content.addView(header(metric))
            content.addView(label(metric.value, 22f, ink, true))
            val targetText = target?.let { "Tagesziel · ${integers.format(it)} Schritte" }
                ?: "Kein gültiges Geräte-Schrittziel verfügbar"
            content.addView(label(targetText, 14f, muted))
            contentDescription = "${metric.title}. ${metric.value}. $targetText."
            if (progress != null) {
                val percentValue = progress
                val percent = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
                    maximumFractionDigits = 1
                }.format(progress)
                contentDescription = "$contentDescription $percent % des Tagesziels."
                content.addView(label("$percent % des Tagesziels", 14f, ink, true))
                content.addView(android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    tag = "dashboard_step_progress"
                    max = 1000
                    this.progress = (percentValue.coerceIn(0.0, 100.0) * 10).toInt()
                    progressTintList = android.content.res.ColorStateList.valueOf(palette.indicator)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LayoutParams(-1, dp(12)))
            }
        }
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
        card.content.addView(label(activity.caption, 11f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(2) })
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
            contentDescription = "Nacht-Insights. SpO₂-Auswertung noch nicht berechenbar: Sampling und Signalqualität für CIRQA sind nicht validiert. Keine Diagnose."
        }
        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(DashboardInfoIconView(context, DashboardInfoIconView.Kind.TIME, palette.accent),
                LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
            addView(label("Nacht-Insights", 15f, ink, true), LayoutParams(0, -2, 1f))
            addView(label("⏳", 17f, palette.muted, true).apply {
                contentDescription = "Nachtserie wird gelesen"
                tag = "night_status_icon"
            }, LayoutParams(-2, -2))
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
                addNightIndicator(insightRow, "◉", "SpO₂", "Nachtserie wird gelesen …", palette.muted)
                return
            }
            val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY).apply { maximumFractionDigits = 1 }
            addNightIndicator(insightRow, "◉", "22–6 Uhr", "${night.samples} Werte", palette.muted)
            addNightIndicator(insightRow, "◷", "Abdeckung",
                night.coveragePercent?.let { "${nf.format(it)} %" } ?: "nicht berechenbar", palette.muted)
            addNightIndicator(insightRow, "⌁", "T90",
                if (!night.computable) "nicht berechenbar" else "${nf.format(night.minutesBelow90)} min", palette.muted)
            addNightIndicator(insightRow, "↧", "ODI3 / ODI4",
                if (!night.computable) "nicht berechenbar"
                else night.odi3?.let { "${nf.format(it)} / ${nf.format(night.odi4)} je h" } ?: "nicht berechenbar", palette.muted)
            // The hourglass only belongs to the loading state.
            findViewWithTag<TextView>("night_status_icon")?.apply { text = ""; contentDescription = null }
            if (night.reason != "ok") nightReason.text = night.reason
        }
        // The overview re-renders whenever readings arrive; show the cached result at once and let the
        // single background load update whichever card is currently on screen.
        fill(cachedNight)
        card.addView(insightRow)
        card.addView(nightReason, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        nightFill = ::fill
        if (cachedNight == null || System.currentTimeMillis() - cachedNightAt > NIGHT_CACHE_MS) loadNight()
        card.addView(label("Nur Originalpunkte · keine Diagnose", 10f, muted).apply {
            gravity = Gravity.END
            contentDescription = "Nur gespeicherte Originalmessungen; keine Diagnose"
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
        const val NIGHT_CACHE_MS = 5 * 60_000L
        @Volatile var cachedNight: NocturnalOxygen.Summary? = null
        @Volatile var cachedNightAt = 0L
        @Volatile var nightLoading = false
    }

    private fun addNightIndicator(parent: LinearLayout, icon: String, title: String, value: String, color: Int) {
        val cell = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(3), dp(4), dp(3))
            contentDescription = "$title: $value"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            addView(label(icon, 18f, color, true), LayoutParams(-2, -2))
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
            contentDescription = "Tagesstand. Kumulativer Gerätezähler; Messpunkte werden nicht addiert."
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            val stepIconParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(5) }
            addView(DashboardMetricIconView(context, "Schritte", palette.indicator), stepIconParams)
            addView(label("Tagesstand", 11f, muted, true), LinearLayout.LayoutParams(-2, -2))
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
            valueLayer.addView(label(metric.value, 22f, accent, true).apply {
                gravity = Gravity.CENTER
                tag = "spo2_value"
            }, android.widget.FrameLayout.LayoutParams(-1, -1))
            content.addView(valueLayer, LayoutParams(-1, dp(76)).apply { topMargin = dp(4) })
        } else {
            content.addView(label(metric.value, 22f, accent, true),
                LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
        val profile = MetricReferenceProfile.forMetric(metric, stepGoal)
        val value = Regex("-?[0-9]+") .find(metric.value)?.value?.toIntOrNull()
        if (profile != null && value != null) {
            content.addView(MetricReferenceScaleView(context, profile, value, accent, ink, muted,
                palette.track, palette.muted), LayoutParams(-1, dp(40)).apply { topMargin = dp(8) })
        }
    }

    private fun card(metric: DashboardMetric) = MetricCard(metric)

    private inner class MetricCard(metric: DashboardMetric) : android.widget.FrameLayout(context) {
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        init {
            background = GradientDrawable().apply {
                setColor(palette.card)
                cornerRadius = dp(18).toFloat()
            }
            tag = "dashboard_metric_${metric.title}"
            contentDescription = "${metric.title}: ${metric.value}"
            if (UnvalidatedFeatures.isUnvalidated(metric.title)) {
                (background as GradientDrawable).let { UnvalidatedFeatures.mark(it, resources.displayMetrics.density, palette.dark) }
                contentDescription = contentDescription.toString() + UnvalidatedFeatures.ACCESSIBILITY_SUFFIX
            }
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
        addView(DashboardMetricIconView(context, metric.title, iconColor(metric.title)).apply {
            tag = "dashboard_metric_icon_${metric.title}"
        }, LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
        // The long SpO₂ title broke mid-word in a half-width card; show the short name, keep the full one for accessibility.
        addView(label(displayTitle(metric.title), 12f, muted, true).apply {
            if (metric.title != "Schritte heute") {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            contentDescription = metric.title
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
        contentDescription = "Dezente Lungengrafik${fillFraction?.let { ", Füllstand ${percent}%" } ?: ", keine Messdaten"}; Farbe nach konfigurierbarer SpO₂-Skala, keine Signalqualitätsanzeige"
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
        contentDescription = "Quelle, Skala und Empfangsinformationen zu ${metric.title}: ${state.label()}. Antippen für Details."
        setOnClickListener { showDetails() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) 8f * density else width - 8f * density
        val y = 8f * density
        val color = when (state) {
            ReceptionFreshness.FRESH -> Color.rgb(31, 166, 113)
            ReceptionFreshness.RECENT -> Color.rgb(207, 166, 19)
            ReceptionFreshness.DELAYED -> Color.rgb(228, 112, 30)
            ReceptionFreshness.OLD -> palette.muted // no alarm red: this only says how old the last reception is
            ReceptionFreshness.UNKNOWN -> palette.muted
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = color
        canvas.drawCircle(x, y, 6f * density, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 3.1f * density, paint)
    }

    private fun showDetails() {
        val now = Instant.now()
        val currentState = ReceptionFreshness.of(metric.receivedAt, now)
        val timestamp = metric.receivedAt
        val received = timestamp?.let {
            val local = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss z")
                .withZone(ZoneId.systemDefault()).format(it)
            val seconds = Duration.between(it, now).seconds.coerceAtLeast(0)
            val age = when {
                seconds < 60 -> "vor $seconds Sekunden"
                seconds < 3600 -> "vor ${seconds / 60} Minuten"
                else -> "vor ${seconds / 3600} Stunden"
            }
            "Empfangen: $local ($age)"
        } ?: "Für diesen Wert liegt kein Empfangszeitpunkt vor."
        val details = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(8))
        }
        fun row(symbol: String, text: String, color: Int = palette.ink, url: String? = null) {
            val kind = when (symbol) {
                "▬" -> DashboardInfoIconView.Kind.SCALE
                "▰" -> DashboardInfoIconView.Kind.RANGE
                "↗" -> DashboardInfoIconView.Kind.SOURCE
                "◷" -> DashboardInfoIconView.Kind.TIME
                "⚠" -> DashboardInfoIconView.Kind.LIMIT
                else -> DashboardInfoIconView.Kind.WINDOW
            }
            val line = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            line.addView(DashboardInfoIconView(context, kind, if (url == null) palette.muted else palette.accent),
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
        row("■", "${metric.title}  ·  ${metric.value}")
        if (profile != null) {
            row("▬", "Skala  ${profile.minimum}–${profile.maximum} ${profile.unit}", palette.muted)
            if (profile.referenceMinimum != null && profile.referenceMaximum != null) {
                row("▰", "${profile.referenceMinimum}–${profile.referenceMaximum} ${profile.unit}  ·  ${profile.referenceLabel}", palette.muted)
            }
            profile.sourceName?.let { row("↗", "Quelle  $it", palette.accent, profile.sourceUrl) }
            if (metric.title == "Sauerstoffsättigung · SpO₂") {
                val displayBands = SpO2DisplaySettings.read(context)
                row("▰", "Vorschau  ${displayBands.bandLabel()}", palette.muted)
                row("⏳", if (displayBands.colorBandsEnabled) "Farbskala aktiv (vom Nutzer akzeptiert) · Sensor NICHT validiert, keine Signalqualität, keine Diagnose oder Alarmfunktion." else "Orientierung deaktiviert · Sensor nicht validiert.", palette.muted)
            }
            row("⚠", profile.caveat, palette.muted)
        } else if (metric.title == "Moderate Intensitätsminuten" || metric.title == "Intensive Intensitätsminuten") {
            row("▱", "WHO: 150–300 min moderat oder 75–150 min intensiv/Woche", palette.muted)
            row("↗", "Quelle  WHO", palette.accent, "https://www.who.int/initiatives/behealthy/physical-activity")
            row("⚠", "CIRQA-Zeitraum unbestätigt; kein Wochenvergleich.", palette.muted)
        } else {
            row("▱", metric.note, palette.muted)
            row("⚠", "Keine validierte Normskala für diesen Gerätewert.", palette.muted)
        }
        row("◷", received, palette.muted)
        row("●", "Empfangsstatus  ${currentState.label()}", palette.muted)
        row("i", "Punktfarbe oben zeigt nur Empfangsalter, keine Gesundheit.", palette.muted)
        AlertDialog.Builder(context).setTitle("ⓘ  ${metric.title}").setView(details)
            .setPositiveButton("Schließen", null).show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun ReceptionFreshness.label() = when (this) {
        ReceptionFreshness.UNKNOWN -> "kein Empfangszeitpunkt"
        ReceptionFreshness.FRESH -> "frisch"
        ReceptionFreshness.RECENT -> "wenige Minuten alt"
        ReceptionFreshness.DELAYED -> "verzögert"
        ReceptionFreshness.OLD -> "lange ohne neuen Empfang"
    }
}

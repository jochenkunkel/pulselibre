package de.pulselibre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat

/** Read-only cards for locally received observations. No transport or persistence ownership. */
class DashboardOverviewView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val ink = palette.ink
    private val muted = palette.muted
    private val violet = palette.accent
    private val preferences = context.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE)
    private var selectedContext = PulseContext.from(preferences.getString("pulse_context", null))

    private var lastPulse: HeartRateGaugeModel? = null

    init { orientation = VERTICAL }

    fun render(metrics: List<DashboardMetric>) {
        removeAllViews()
        val overview = dashboardOverview(metrics)
        addView(label("Deine Messwerte · lokal gespeichert", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addView(pulseCard(overview.vitalSigns.first()),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addView(metricCard(overview.vitalSigns[1], palette.oxygen),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addView(metricCard(overview.sleep, violet),
            LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        heading("Heute auf einen Blick")
        addView(label("Schritte: letzter empfangener Tageszähler, keine Summe", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addPair(overview.today)
        heading("Weitere Gesundheitswerte")
        overview.details.forEach { metric ->
            addView(metricCard(metric, ink),
                LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun pulseCard(metric: DashboardMetric): LinearLayout {
        val model = HeartRateGaugeModel.from(metric)
        val previous = lastPulse
        lastPulse = model
        val card = metricCard(metric, palette.pulse)
        // Replace the ordinary value card with the approved instrument layout.
        card.removeAllViews()
        val gauge = HeartRateGaugeView(context)
        fun updateContext() {
            gauge.show(model, selectedContext)
            card.contentDescription = "${metric.title}: ${metric.value}. ${metric.note}. " +
                "${selectedContext.label}. ${if (selectedContext == PulseContext.UNKNOWN) "Aktivität nicht erkannt" else "Modus manuell gewählt"}. ${model.scaleNote}"
        }
        card.addView(label("Herzfrequenz", 16f, ink, true))
        card.addView(gauge, LayoutParams(-1, -2))
        val controls = LinearLayout(context).apply {
            orientation = if (resources.configuration.fontScale >= 1.3f ||
                resources.configuration.screenWidthDp < 360) VERTICAL else HORIZONTAL
        }
        val buttons = mutableListOf<TextView>()
        fun refreshButtons() {
            buttons.forEach { button ->
                val selected = button.tag == selectedContext
                button.isSelected = selected
                button.setTextColor(if (selected) Color.WHITE else violet)
                button.background = GradientDrawable().apply {
                    setColor(if (selected) palette.primary else palette.tinted)
                    cornerRadius = dp(20).toFloat()
                }
            }
        }
        listOf(PulseContext.REST, PulseContext.MOVEMENT, PulseContext.SPORT).forEach { mode ->
            val button = label(if (mode == PulseContext.MOVEMENT) "Bewegung" else mode.label, 13f, violet, true).apply {
                tag = mode
                gravity = android.view.Gravity.CENTER
                minHeight = dp(48)
                setPadding(dp(4), dp(6), dp(4), dp(6))
                isClickable = true
                isFocusable = true
                contentDescription = "${mode.label} manuell wählen; erneut tippen hebt die Auswahl auf"
                setOnClickListener {
                    selectedContext = if (selectedContext == mode) PulseContext.UNKNOWN else mode
                    preferences.edit().putString("pulse_context", selectedContext.name).apply()
                    updateContext()
                    refreshButtons()
                }
            }
            buttons.add(button)
            controls.addView(button, if (controls.orientation == VERTICAL)
                LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
            else LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        }
        card.addView(controls)
        card.addView(label("Modus manuell wählen · erneut tippen zum Aufheben", 12f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(10) })
        card.addView(label(model.scaleNote, 12f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(6) })
        card.addView(label(metric.note, 12f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(6) })
        // The context buttons remain individually accessible, unlike the read-only value cards.
        ViewCompat.setScreenReaderFocusable(card, false)
        updateContext()
        refreshButtons()
        if (previous?.bpm != null && model.bpm != null && previous.bpm != model.bpm) {
            gauge.post { if (gauge.isAttachedToWindow) gauge.show(model, selectedContext, previous.fraction) }
        }
        return card
    }

    private fun addPair(metrics: List<DashboardMetric>) {
        // Preserve readable text at large accessibility font sizes and narrow widths.
        val stacked = resources.configuration.fontScale >= 1.3f ||
            resources.configuration.screenWidthDp < 360
        val row = LinearLayout(context).apply { orientation = if (stacked) VERTICAL else HORIZONTAL }
        metrics.forEachIndexed { index, metric ->
            // Accent colors identify metrics, never normal/abnormal health states.
            val accent = when (metric.title) {
                "Herzfrequenz" -> palette.pulse
                "Sauerstoffsättigung · SpO₂" -> palette.oxygen
                else -> ink
            }
            row.addView(metricCard(metric, accent),
                if (stacked) LayoutParams(-1, -2).apply {
                    if (index != metrics.lastIndex) bottomMargin = dp(8)
                } else LayoutParams(0, -2, 1f).apply {
                    if (index != metrics.lastIndex) marginEnd = dp(8)
                })
        }
        addView(row, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun metricCard(metric: DashboardMetric, accent: Int) = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply {
            setColor(palette.card)
            cornerRadius = dp(20).toFloat()
        }
        tag = "dashboard_metric_${metric.title}"
        contentDescription = "${metric.title}: ${metric.value}. ${metric.note}"
        ViewCompat.setScreenReaderFocusable(this, true)
        addView(label(metric.title, 13f, muted, true))
        addView(label(metric.value, 22f, accent, true),
            LayoutParams(-1, -2).apply { topMargin = dp(10) })
        addView(label(metric.note, 12f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(8) })
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
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

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
    private val ink = Color.rgb(35, 29, 59)
    private val muted = Color.rgb(109, 104, 127)
    private val violet = Color.rgb(81, 39, 201)

    init { orientation = VERTICAL }

    fun render(metrics: List<DashboardMetric>) {
        removeAllViews()
        val overview = dashboardOverview(metrics)
        addView(label("Lokal empfangene Werte · keine medizinische Bewertung", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addPair(overview.vitalSigns)
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

    private fun addPair(metrics: List<DashboardMetric>) {
        // Preserve readable text at large accessibility font sizes and narrow widths.
        val stacked = resources.configuration.fontScale >= 1.3f ||
            resources.configuration.screenWidthDp < 360
        val row = LinearLayout(context).apply { orientation = if (stacked) VERTICAL else HORIZONTAL }
        metrics.forEachIndexed { index, metric ->
            // Accent colors identify metrics, never normal/abnormal health states.
            val accent = when (metric.title) {
                "Herzfrequenz" -> Color.rgb(166, 38, 79)
                "Sauerstoffsättigung · SpO₂" -> Color.rgb(27, 91, 156)
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
            setColor(Color.WHITE)
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

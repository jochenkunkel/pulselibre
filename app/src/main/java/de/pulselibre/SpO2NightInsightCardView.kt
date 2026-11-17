package de.pulselibre

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Compact night-analysis placeholder. Raw samples lack validated night boundaries, cadence,
 * and signal-quality metadata, so ODI/T90 are deliberately not calculated.
 */
internal class SpO2NightInsightCardView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)

    init {
        orientation = VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            setColor(palette.card)
            cornerRadius = dp(18).toFloat()
        }
        tag = "dashboard_night_oxygen_insights"
        contentDescription = "Nacht-Insights. SpO₂-Auswertung nicht berechenbar: Nachtfenster, Sampling und Signalqualität sind nicht validiert. Keine Diagnose."
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(DashboardInfoIconView(context, DashboardInfoIconView.Kind.TIME, palette.accent),
                LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
            addView(text("Nacht-Insights", 15f, palette.ink, true), LayoutParams(0, -2, 1f))
            addView(text("⏳", 17f, palette.muted, true).apply {
                contentDescription = "Analyse nicht verfügbar"
            }, LayoutParams(-2, -2))
        }
        addView(titleRow)
        addView(View(context).apply { setBackgroundColor(palette.track) },
            LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(10) })

        val values = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        indicator(values, "◉", "SpO₂", "Nachtserie offen")
        indicator(values, "◷", "Abdeckung", "Sampling offen")
        indicator(values, "⌁", "ODI / T90", "Nicht berechenbar")
        addView(values)
        addView(text("Nur Originalpunkte · keine Diagnose", 10f, palette.muted, false).apply {
            gravity = Gravity.END
            contentDescription = "Nur gespeicherte Originalmesspunkte; keine Diagnose"
        }, LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun indicator(parent: LinearLayout, icon: String, title: String, value: String) {
        val cell = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(3), dp(4), dp(3))
            contentDescription = "$title: $value"
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            addView(text(icon, 18f, palette.muted, true), LayoutParams(-2, -2))
            addView(text(title, 9f, palette.muted, true), LayoutParams(-2, -2))
            addView(text(value, 9f, palette.muted, false), LayoutParams(-1, -2))
        }
        parent.addView(cell, LayoutParams(0, -2, 1f))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        includeFontPadding = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

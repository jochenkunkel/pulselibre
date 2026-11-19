package de.pulselibre

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

/** Compatibility view for dashboard metrics. All artwork comes from the PulsLibre SVG icon set. */
class DashboardMetricIconView(context: Context, title: String, color: Int) : AppCompatImageView(context) {
    init {
        setImageDrawable(PlIcon.drawable(context, iconFor(title), 24, color))
        scaleType = ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    companion object {
        /** Match the former Canvas icon semantics, including special cases before generic steps. */
        internal fun iconFor(title: String): PlIcon = when {
            title.contains("Herzfrequenz", true) || title.contains("Ruhepuls", true) -> PlIcon.HEART
            title.contains("SpO₂", true) || title.contains("Sauerstoff", true) -> PlIcon.OXYGEN
            title.contains("Schlaf", true) -> PlIcon.SLEEP
            title.contains("Schrittziel", true) || title.contains("Tagesziel", true) -> PlIcon.GOAL
            title.contains("Schritte", true) -> PlIcon.STEPS
            title.contains("Kalorien", true) || title.contains("Energie", true) -> PlIcon.CALORIES
            title.contains("Intensitätsminuten", true) || title.contains("Aktivität", true) -> PlIcon.INTENSITY
            title.contains("RR-Intervall", true) || title.contains("HRV", true) -> PlIcon.RR_INTERVAL
            title.contains("Stress", true) -> PlIcon.STRESS
            title.contains("Body Battery", true) -> PlIcon.BODY_BATTERY
            title.contains("Atem", true) -> PlIcon.BREATHING
            else -> PlIcon.GENERIC_METRIC
        }
    }
}

package de.pulselibre

import android.graphics.drawable.GradientDrawable

/**
 * Product decision of the owner (2026-11-18): validation gates are accepted so gated features are
 * shown, but every tile or card relying on an unvalidated source carries a DARK-PURPLE BORDER so it is
 * always visible what has not been validated. This does not validate anything and is not a medical
 * statement; the explanatory texts on the tiles stay unchanged.
 */
internal object UnvalidatedFeatures {
    /** Deep purple from the PulsLibre palette family (primary #5127C9); lighter in dark mode for contrast. */
    fun color(dark: Boolean): Int = if (dark) 0xFFA27CFF.toInt() else 0xFF3A1778.toInt()
    const val STROKE_DP = 2
    const val LEGEND = "Violetter Rand = Messwert oder Auswertung nicht validiert (Sensor/Gerätefeld ungeprüft, keine Diagnose)."
    const val ACCESSIBILITY_SUFFIX = " Nicht validiert, violetter Rand."

    private val titles: Set<String> =
        MetricAvailability.EXPERIMENTAL_METRICS.map { it.title }.toSet() + "Schlaf"

    fun isUnvalidated(title: String): Boolean = title in titles || title.startsWith("Kalorien")

    fun mark(background: GradientDrawable, density: Float, dark: Boolean) {
        background.setStroke((STROKE_DP * density + 0.5f).toInt(), color(dark))
    }
}

package de.pulselibre

import android.content.Context

/**
 * Display names for dashboard metrics. The German titles in the data model are stable identifiers (layout storage, view
 * tags, tests) and must not be shown directly any more; this maps them to the language of the app (#i18n).
 * Unknown ids are shown unchanged.
 */
internal object MetricText {
    private val full = mapOf(
        "Herzfrequenz" to R.string.metric_pulse,
        "Ruhepuls" to R.string.metric_resting_pulse,
        "Schritte (Tageszähler)" to R.string.metric_steps_counter,
        "Schrittziel" to R.string.metric_step_goal,
        "Sauerstoffsättigung · SpO₂" to R.string.metric_spo2,
        "RR-Intervall (kein RMSSD)" to R.string.metric_rr,
        "Stress (Geräteskala)" to R.string.metric_stress,
        "Body Battery (Geräteskala)" to R.string.metric_body_battery,
        "Atemfrequenz" to R.string.metric_respiration,
        "Kalorien gesamt (Gerätewert)" to R.string.metric_calories_total,
        "Kalorien aktiv (Gerätewert)" to R.string.metric_calories_active,
        "Moderate Intensitätsminuten" to R.string.metric_moderate,
        "Intensive Intensitätsminuten" to R.string.metric_vigorous,
        "Schlaf" to R.string.metric_sleep,
        "Schritte" to R.string.metric_steps,
        "Schritte heute" to R.string.metric_steps_today,
    )
    private val short = mapOf(
        "Sauerstoffsättigung · SpO₂" to R.string.metric_spo2_short,
        "Moderate Intensitätsminuten" to R.string.metric_moderate_short,
        "Intensive Intensitätsminuten" to R.string.metric_vigorous_short,
        "Kalorien gesamt (Gerätewert)" to R.string.metric_calories_total_short,
        "Kalorien aktiv (Gerätewert)" to R.string.metric_calories_active_short,
    )

    fun title(context: Context, id: String): String = full[id]?.let(context::getString) ?: id

    /** Short visible name for half-width cards; falls back to the full name. */
    fun shortTitle(context: Context, id: String): String = short[id]?.let(context::getString) ?: title(context, id)

    /** Replaces the stored placeholder "Keine Messdaten"; real values (numbers with units) are language independent. */
    fun value(context: Context, raw: String): String = if (raw == NO_DATA) context.getString(R.string.metric_no_data) else raw

    const val NO_DATA = "Keine Messdaten"
}

package de.pulselibre

import org.json.JSONObject

/** Fields supported by the local dashboard, with display plausibility bounds. */
internal enum class DashboardMeasurement(
    val service: Int,
    val title: String,
    val key: String,
    val unit: String,
    private val plausible: (Long) -> Boolean
) {
    PULSE(6, "Herzfrequenz", "bpm", "bpm", { it in 1..240 }),
    RESTING_PULSE(6, "Ruhepuls", "resting_bpm", "bpm", { it in 1..240 }),
    STEPS(7, "Schritte (Tageszähler)", "steps", "", { it >= 0 }),
    GOAL(7, "Schrittziel", "goal", "", { it > 0 }),
    OXYGEN(19, "Sauerstoffsättigung · SpO₂", "spo2_percent", "%", { it in 1..100 }),
    RR(12, "RR-Intervall (kein RMSSD)", "rr_ms", "ms", { it > 0 }),
    STRESS(13, "Stress (Geräteskala)", "stress", "Geräteskala 0–100", { it in 0..100 }),
    BODY_BATTERY(20, "Body Battery (Geräteskala)", "body_battery", "Geräteskala 0–100", { it in 0..100 }),
    RESPIRATION(21, "Atemfrequenz", "breaths_per_min", "Atemzüge/min", { it in 1..100 }),
    CALORIES_TOTAL(8, "Kalorien gesamt (Gerätewert)", "total", "", { it >= 0 }),
    CALORIES_ACTIVE(8, "Kalorien aktiv (Gerätewert)", "active", "", { it >= 0 }),
    MODERATE(10, "Moderate Intensitätsminuten", "moderate", "min", { it >= 0 }),
    VIGOROUS(10, "Intensive Intensitätsminuten", "vigorous", "min", { it >= 0 });

    fun value(event: JSONObject): Long? =
        event.optLong(key, Long.MIN_VALUE).takeIf(plausible)
}

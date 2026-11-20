package de.pulselibre

import java.time.LocalDate

/**
 * "Letzte 7 Tage" (#265): the seven complete days before today, each row with its own coverage ("x von 7 Tagen").
 * Averages are taken only over days that have a value; missing days are never counted as 0 and resting pulse is
 * only what the device reported (no minimum-of-day estimate). No score, no rating.
 */
internal object WeekSummary {
    const val DAYS = 7

    data class Row(
        val label: String,
        /** Formatted value, or null when no day of the week has data. */
        val value: String?,
        val validDays: Int,
        /** Optional second line, e.g. the goal statement. */
        val detail: String? = null,
    ) {
        fun text() = "$label: ${value ?: "keine Daten"}"
        fun coverage() = "$validDays von $DAYS Tagen"
        fun description() = text() + (if (value != null) ", ${coverage()}" else "") + (detail?.let { ". $it" } ?: "")
    }

    data class Result(val rows: List<Row>, val unavailable: String) {
        val days: Int get() = DAYS
    }

    /** Sources that are not backed by validated daily values yet (see GARMIN_CONNECT_BENCHMARK_261). */
    const val UNAVAILABLE = "Nicht verfügbar (keine belegte Quelle): Schlafdauer, Kalorien, Intensitätsminuten, Stress."

    fun build(stepsDaily: Map<LocalDate, Double>, restDaily: Map<LocalDate, Double>, today: LocalDate,
              stepGoal: Int?, format: (Double) -> String): Result {
        val days = (1..DAYS).map { today.minusDays(it.toLong()) }
        fun valid(m: Map<LocalDate, Double>) = days.mapNotNull { d -> m[d]?.takeIf { it.isFinite() && it >= 0.0 } }
        val steps = valid(stepsDaily)
        val rest = valid(restDaily).filter { it > 0.0 }
        val goalDetail = stepGoal?.takeIf { it > 0 && steps.isNotEmpty() }?.let { goal ->
            "Ziel ($goal) erreicht an ${steps.count { it >= goal }} von ${steps.size} Tagen mit Daten."
        }
        return Result(listOf(
            Row("Ø Schritte pro Tag", steps.takeIf { it.isNotEmpty() }?.let { format(it.average()) }, steps.size, goalDetail),
            Row("Schritte gesamt", steps.takeIf { it.isNotEmpty() }?.let { format(it.sum()) + " (nur Tage mit Daten)" }, steps.size),
            Row("Ø Geräte-Ruhepuls", rest.takeIf { it.isNotEmpty() }?.let { format(it.average()) + " bpm" }, rest.size),
        ), UNAVAILABLE)
    }
}

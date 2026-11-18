package de.pulselibre

import de.pulselibre.TrendAnalysis.Direction

/**
 * Three separate dimensions, deliberately without a combined score (#42).
 * Regeneration stays "nicht bewertbar" until verified sleep/HRV intervals exist (#43).
 * A falling resting pulse alone never yields a positive statement.
 */
internal object FitnessOverview {
    enum class State(val label: String) {
        POSITIVE("positiv"), NEUTRAL("neutral"), MIXED("uneinheitlich"), NOT_ASSESSABLE("nicht bewertbar")
    }

    data class Dimension(val name: String, val state: State, val sentence: String)

    data class Result(val dimensions: List<Dimension>, val windowDays: Int)

    fun assess(activity: TrendAnalysis.Result, restingPulse: TrendAnalysis.Result): Result {
        val n = activity.windowDays
        val a = activity.direction
        val r = restingPulse.direction
        val activityDim = when (a) {
            Direction.UNKNOWN -> Dimension("Aktivitätskonstanz", State.NOT_ASSESSABLE,
                "Zu wenige Tage mit Schrittdaten für einen Vergleich über $n Tage.")
            Direction.UP -> Dimension("Aktivitätskonstanz", State.NEUTRAL, "Schritte pro Tag sind gegenüber dem Vorzeitraum gestiegen.")
            Direction.DOWN -> Dimension("Aktivitätskonstanz", State.NEUTRAL, "Schritte pro Tag sind gegenüber dem Vorzeitraum gesunken.")
            Direction.STABLE -> Dimension("Aktivitätskonstanz", State.NEUTRAL, "Schritte pro Tag sind gegenüber dem Vorzeitraum unverändert.")
        }
        val fitnessDim = when {
            a == Direction.UNKNOWN && r == Direction.UNKNOWN -> Dimension("Fitnessentwicklung", State.NOT_ASSESSABLE,
                "Weder Aktivität noch Ruhepuls haben genug Daten über $n Tage.")
            a == Direction.UNKNOWN || r == Direction.UNKNOWN -> Dimension("Fitnessentwicklung", State.NEUTRAL,
                "Nur ein Datenkanal verfügbar (${if (a == Direction.UNKNOWN) "Ruhepuls" else "Schritte"}); keine Aussage zur Fitness.")
            a == Direction.UP && r == Direction.DOWN -> Dimension("Fitnessentwicklung", State.POSITIVE,
                "Aktivität gestiegen; Geräte-Ruhepuls im selben Zeitraum gesunken. Hinweis, keine Diagnose.")
            (a == Direction.DOWN && r == Direction.UP) -> Dimension("Fitnessentwicklung", State.MIXED,
                "Aktivität gesunken und Geräte-Ruhepuls gestiegen. Mögliche Ursachen wurden nicht geprüft.")
            (a == Direction.UP && r == Direction.UP) || (a == Direction.DOWN && r == Direction.DOWN) -> Dimension("Fitnessentwicklung", State.MIXED,
                "Aktivität und Geräte-Ruhepuls entwickeln sich nicht gleichgerichtet im Sinne eines Trainingseffekts.")
            else -> Dimension("Fitnessentwicklung", State.NEUTRAL, "Keine eindeutige Veränderung über $n Tage.")
        }
        val recovery = Dimension("Regeneration", State.NOT_ASSESSABLE,
            "Kein verifiziertes Schlaf- oder HRV-Signal; es wird nichts geschätzt.")
        return Result(listOf(fitnessDim, recovery, activityDim), n)
    }
}

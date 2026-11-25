package de.pulselibre

import java.time.LocalDate

/**
 * The pre-specified relationship hypotheses of the "Zusammenhänge" analysis (#45). Pure logic, no UI.
 *
 * Fixed in advance (no search for the strongest pair): each hypothesis has one primary lag; lag 0 is reported only as a
 * sensitivity check. Hypotheses without the data they need stay "not computable" and are named, not hidden. The
 * computed primary tests are corrected together with Benjamini–Hochberg; an association is only reported as
 * exploratory when the interval excludes 0, |rho| reaches [PairedCorrelation.MIN_EFFECT] AND the adjusted p is below
 * [ALPHA]. Correlation is not causation.
 */
internal object RelationshipHypotheses {
    const val ALPHA = 0.05

    enum class Id { STEPS_REST_NEXT_DAY, SLEEP_REST_NEXT_DAY, SLEEP_HRV }

    /** Why a hypothesis could not be computed when its data are missing. */
    enum class Missing { NONE, SLEEP_NOT_DECODED, HRV_NOT_DECODED }

    data class Outcome(
        val id: Id,
        val primaryLag: Int,
        val primary: PairedCorrelation.Result?,
        val lagZero: PairedCorrelation.Result?,
        /** Benjamini–Hochberg adjusted p over all computed primary tests; null when not computed. */
        val adjustedP: Double?,
        val verdict: PairedCorrelation.Verdict,
        val missing: Missing
    )

    /**
     * [sleepMinutes] and [hrv] are null while the band data are not decoded (FIT); then H2/H3 stay not computable.
     */
    fun evaluate(
        steps: Map<LocalDate, Double>,
        restingPulse: Map<LocalDate, Double>,
        sleepMinutes: Map<LocalDate, Double>? = null,
        hrv: Map<LocalDate, Double>? = null
    ): List<Outcome> {
        data class Spec(val id: Id, val x: Map<LocalDate, Double>?, val y: Map<LocalDate, Double>?, val lag: Int, val missing: Missing)
        val specs = listOf(
            Spec(Id.STEPS_REST_NEXT_DAY, steps, restingPulse, 1, Missing.NONE),
            Spec(Id.SLEEP_REST_NEXT_DAY, sleepMinutes, restingPulse, 1, Missing.SLEEP_NOT_DECODED),
            Spec(Id.SLEEP_HRV, sleepMinutes, hrv, 0, if (sleepMinutes == null) Missing.SLEEP_NOT_DECODED else Missing.HRV_NOT_DECODED)
        )
        val computed = specs.map { s ->
            if (s.x == null || s.y == null) Triple(s, null, null)
            else Triple(s, PairedCorrelation.analyze(s.x, s.y, s.lag), if (s.lag != 0) PairedCorrelation.analyze(s.x, s.y, 0) else null)
        }
        val tested = computed.withIndex().filter { it.value.second?.pValue != null }
        val adjusted = PairedCorrelation.benjaminiHochberg(tested.map { it.value.second!!.pValue!! })
        val adjustedByIndex = tested.map { it.index }.zip(adjusted).toMap()
        return computed.mapIndexed { i, (s, primary, lagZero) ->
            val adj = adjustedByIndex[i]
            val verdict = when {
                primary == null -> PairedCorrelation.Verdict.NOT_COMPUTABLE
                primary.verdict == PairedCorrelation.Verdict.EXPLORATORY_ASSOCIATION && adj != null && adj >= ALPHA ->
                    PairedCorrelation.Verdict.NO_RELIABLE_ASSOCIATION
                else -> primary.verdict
            }
            Outcome(s.id, s.lag, primary, lagZero, adj, verdict, if (primary == null) s.missing else Missing.NONE)
        }
    }
}

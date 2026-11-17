package de.pulselibre

/**
 * Fail-closed counter rule for new negative GFDI status responses.
 * The baseline is captured when the 5050 host response is queued.
 * This does not infer which command caused the negative response.
 */
object Cirqa5050NegativeResponseGuard {
    fun shouldAbort(baselineErrors: Int, currentErrors: Int): Boolean =
        baselineErrors < 0 || currentErrors < baselineErrors ||
            currentErrors > baselineErrors
}

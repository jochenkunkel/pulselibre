package de.pulselibre

/**
 * What a rejected FileAccess pull response does to the run (#637, 26.11.2026).
 *
 * The S10 runs `49d2005da20a` and `3dada620b0a5` got `result=3` for the very first pull and stopped at once, so 37 listed
 * files were never requested. A rejection now skips that file only; [MAX_CONSECUTIVE] rejections in a row still end the
 * run, so a band that refuses everything is not hammered. The rejected file is counted as failed and stays retryable in
 * the next run (no remote state is changed).
 */
internal object CirqaPullRejectionPolicy {
    const val MAX_CONSECUTIVE = 3
    fun stopAfter(consecutiveRejections: Int): Boolean = consecutiveRejections >= MAX_CONSECUTIVE
}

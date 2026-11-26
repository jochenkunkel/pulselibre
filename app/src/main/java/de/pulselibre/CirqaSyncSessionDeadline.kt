package de.pulselibre

/**
 * End of a history-sync session (#637/#599, 26.11.2026).
 *
 * Until now a fixed 120 s timer ended every run, also while files were still arriving: run `bc355145cf95` stopped with
 * 28 of 29 files although the transfer was progressing. Now the run ends after [IDLE_LIMIT_MS] WITHOUT progress (same
 * 120 s as before) or at the hard cap [MAX_SESSION_MS]. Progress is any change of the run's observable state
 * (pipeline stages, received transfer bytes, failed files). The file/byte budgets of [CirqaSyntheticDownloadLimits]
 * stay as they are, so a run remains bounded.
 */
internal class CirqaSyncSessionDeadline(private val startMs: Long) {
    companion object {
        const val IDLE_LIMIT_MS = 120_000L
        const val MAX_SESSION_MS = 15L * 60_000L
        const val CHECK_INTERVAL_MS = 5_000L
        /** Kept as prefix: report parsing and the timeout finalizer key on it. */
        const val IDLE_MESSAGE = "120-s-Zeitlimit"
        const val TOTAL_MESSAGE = "15-min-Gesamtzeitlimit"
    }

    enum class Expiry { IDLE, TOTAL }

    private var lastSignature: String? = null
    private var lastProgressMs = startMs

    /** Records [signature] at [nowMs]; returns the expiry reason, or null while the run may continue. */
    fun check(signature: String, nowMs: Long): Expiry? {
        if (signature != lastSignature) { lastSignature = signature; lastProgressMs = nowMs }
        return when {
            nowMs - startMs >= MAX_SESSION_MS -> Expiry.TOTAL
            nowMs - lastProgressMs >= IDLE_LIMIT_MS -> Expiry.IDLE
            else -> null
        }
    }
}

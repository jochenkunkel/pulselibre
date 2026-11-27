package de.pulselibre

/** App-entry policy for the bounded historical sync; no timer or background worker. */
object HistoricalSyncPolicy {
    /**
     * Owner decision 26.11.2026 (#637): the band keeps daily-history files (FIT 32/44/49: stress, SpO2, breathing,
     * all-day pulse, sleep) only until Garmin Connect has pulled and flagged them. PulsLibre therefore fetches the
     * history automatically when the app comes to the foreground (bounded by the intervals below; no timer, no
     * background worker). Read-only: PulsLibre never sets flags, so Connect still receives every file.
     */
    const val AUTOMATIC_SYNC_ENABLED = true
    const val MIN_ATTEMPT_INTERVAL_MS = 5 * 60_000L
    const val FRESHNESS_MS = 15 * 60_000L

    fun isDue(now: Long, lastAttempt: Long, lastSuccess: Long): Boolean {
        if (lastAttempt > 0 && now - lastAttempt < MIN_ATTEMPT_INTERVAL_MS) return false
        return lastSuccess <= 0 || now - lastSuccess >= FRESHNESS_MS
    }
}

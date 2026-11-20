package de.pulselibre

/** App-entry policy for the bounded historical sync; no timer or background worker. */
object HistoricalSyncPolicy {
    const val MIN_ATTEMPT_INTERVAL_MS = 5 * 60_000L
    const val FRESHNESS_MS = 15 * 60_000L

    fun isDue(now: Long, lastAttempt: Long, lastSuccess: Long): Boolean {
        if (lastAttempt > 0 && now - lastAttempt < MIN_ATTEMPT_INTERVAL_MS) return false
        return lastSuccess <= 0 || now - lastSuccess >= FRESHNESS_MS
    }
}

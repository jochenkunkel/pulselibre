package de.pulselibre

/**
 * When the polled overview and history reload while live readings arrive (about one per second).
 * New data reloads after a minimum interval; without new data they still reload every [IDLE_MS] so relative times stay
 * correct. A forced refresh (interval reset to 0) is always due.
 */
internal object LiveRefreshPolicy {
    /** Overview tiles: current values, cheap queries. */
    const val DASHBOARD_MIN_MS = 5_000L
    /** History: 30-day summary over unified_measurements plus band series, the most expensive screen. */
    const val HISTORY_MIN_MS = 15_000L
    const val IDLE_MS = 30_000L

    fun due(newData: Boolean, sinceLastMs: Long, minIntervalMs: Long): Boolean =
        (newData && sinceLastMs >= minIntervalMs) || sinceLastMs >= IDLE_MS
}

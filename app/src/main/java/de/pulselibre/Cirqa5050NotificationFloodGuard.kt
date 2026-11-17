package de.pulselibre

/**
 * Bounded observation policy for the opt-in 5050 diagnostic.
 * Counts incoming BLE notifications after the 5050 response was queued,
 * regardless of whether their contents are valid GFDI frames.
 * Never sends BLE commands.
 */
object Cirqa5050NotificationFloodGuard {
    const val MAX_NEW_NOTIFICATIONS = 120

    fun shouldAbort(baseline: Int, current: Int): Boolean {
        if (baseline < 0 || current < baseline) return true
        return current.toLong() - baseline.toLong() >= MAX_NEW_NOTIFICATIONS
    }
}

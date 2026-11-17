package de.pulselibre

/**
 * Offline policy for aborting an opt-in 5050 diagnostic after repeated
 * malformed GFDI frames. The counter comes from CRC/length/COBS rejection.
 * Only rejections after the 5050 response is queued count.
 */
object Cirqa5050MalformedFrameGuard {
    const val MAX_NEW_REJECTIONS = 3

    fun shouldAbort(baseline: Int, current: Int): Boolean {
        // Invalid counters are a failed diagnostic, not successful evidence.
        if (baseline < 0 || current < baseline) return true
        return current - baseline >= MAX_NEW_REJECTIONS
    }
}

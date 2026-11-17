package de.pulselibre

/**
 * Pure counter comparison for an explicitly bounded GFDI-5050 diagnostic.
 * Only frames after the queued response count as device retries.
 */
object Cirqa5050RetryGuard {
    fun hasRetry(baseline5050Frames: Int, observed5050Frames: Int): Boolean {
        require(baseline5050Frames >= 0)
        require(observed5050Frames >= 0)
        return observed5050Frames > baseline5050Frames
    }
}

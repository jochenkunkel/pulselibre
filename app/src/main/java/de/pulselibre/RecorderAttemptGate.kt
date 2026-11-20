package de.pulselibre

/**
 * Main-thread confined attempt lifecycle. A callback is accepted only for the
 * current generation and the exact probe owner. Terminal events consume the
 * attempt once; stop/restart invalidates all pending callbacks.
 */
internal class RecorderAttemptGate {
    private var generation = 0
    private var activeOwner: Any? = null
    private var running = false

    fun start(): Int {
        running = true
        activeOwner = null
        return ++generation
    }

    fun attach(token: Int, owner: Any): Boolean {
        if (!running || token != generation || activeOwner != null) return false
        activeOwner = owner
        return true
    }

    fun accepts(token: Int, owner: Any): Boolean =
        running && token == generation && activeOwner === owner

    /** Returns a fresh token for the one allowed reconnect schedule. */
    fun finish(token: Int, owner: Any): Int? {
        if (!accepts(token, owner)) return null
        activeOwner = null
        return ++generation
    }

    fun canRetry(token: Int): Boolean =
        running && token == generation && activeOwner == null

    fun stop() {
        running = false
        activeOwner = null
        generation++
    }
}

/** Reconnect delay of the continuous recorder: 2, 4, 8, 16, 32, then 60 seconds (bounded, deterministic). */
internal object RecorderBackoff {
    const val BASE_MS = 2_000L
    const val MAX_MS = 60_000L

    /** [attemptNumber] is 1-based and counts every attempt since the service started. */
    fun delayMs(attemptNumber: Int): Long =
        (BASE_MS shl (attemptNumber - 1).coerceIn(0, 5)).coerceAtMost(MAX_MS)
}

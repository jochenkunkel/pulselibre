package de.pulselibre

/** Separate gate; never references or rearms the older experiment preference keys. */
object CirqaLegacy5030AttemptGate {
    const val PREFERENCE_KEY = "legacy5030OnlyAttempted"
    // Explicit user approval on 2026-11-18: one 5030-only run, no follow-up commands.
    // Persistent consumption still applies; this does not rearm any older gate.
    const val RELEASED_FOR_DEVICE_TEST = true

    fun consume(
        released: Boolean,
        userConfirmed: Boolean,
        readConsumed: () -> Boolean,
        persistConsumed: () -> Boolean
    ): Boolean = synchronized(this) {
        released && userConfirmed && !readConsumed() && persistConsumed()
    }
}

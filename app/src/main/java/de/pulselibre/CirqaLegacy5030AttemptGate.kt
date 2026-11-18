package de.pulselibre

/** Separate gate; never references or rearms the older experiment preference keys. */
object CirqaLegacy5030AttemptGate {
    const val PREFERENCE_KEY = "legacy5030OnlyAttemptedV2"
    // Explicit user re-approval on 2026-11-18 after recorder-busy abortion: one 5030-only run.
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

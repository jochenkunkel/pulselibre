package de.pulselibre

/** Separate gate; never references or rearms the older experiment preference keys. */
object CirqaLegacy5030AttemptGate {
    const val PREFERENCE_KEY = "legacy5030OnlyAttempted"
    // Jour fixe: preparation only. Change only after a separately recorded approval.
    const val RELEASED_FOR_DEVICE_TEST = false

    fun consume(
        released: Boolean,
        userConfirmed: Boolean,
        readConsumed: () -> Boolean,
        persistConsumed: () -> Boolean
    ): Boolean = synchronized(this) {
        released && userConfirmed && !readConsumed() && persistConsumed()
    }
}

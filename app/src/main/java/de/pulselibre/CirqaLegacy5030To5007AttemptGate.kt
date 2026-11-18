package de.pulselibre

/** Separate persistent one-shot gate for the combined 5030 -> 5007 test. */
object CirqaLegacy5030To5007AttemptGate {
    const val PREFERENCE_KEY = "legacy5030To5007Attempted"
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

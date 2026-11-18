package de.pulselibre

/** Separate persistent one-shot gate for the bounded directory-read-only phase. */
object CirqaDirectoryReadV2AttemptGate {
    const val PREFERENCE_KEY = "cirqaDirectoryReadV2Attempted"
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

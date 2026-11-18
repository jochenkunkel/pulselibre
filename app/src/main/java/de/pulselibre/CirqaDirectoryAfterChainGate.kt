package de.pulselibre

/** Persistent one-shot gate for full-chain + directory-body read in one connection. */
object CirqaDirectoryAfterChainGate {
    const val PREFERENCE_KEY = "cirqaDirectoryAfterChainAttemptedV3"
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

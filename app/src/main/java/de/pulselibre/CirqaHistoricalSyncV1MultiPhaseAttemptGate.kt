package de.pulselibre

/** Isolated persistent one-shot gate for Historical Sync V1 multi-phase test. */
object CirqaHistoricalSyncV1MultiPhaseAttemptGate {
    const val PREFERENCE_KEY = "historicalSyncV1MultiPhaseAttempted"
    fun keyForPhase(phase: Int): String = "historicalSyncV1Phase${phase}AttemptedV4"
    // Explicit authorization per development instructions; single-shot fail-closed.
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

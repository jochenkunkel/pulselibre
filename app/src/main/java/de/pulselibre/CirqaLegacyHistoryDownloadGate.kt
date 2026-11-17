package de.pulselibre

/** Separate lifetime one-shot gate for the explicitly approved bounded history transfer. */
object CirqaLegacyHistoryDownloadGate {
    fun canAttempt(consumed: Boolean): Boolean = !consumed
}

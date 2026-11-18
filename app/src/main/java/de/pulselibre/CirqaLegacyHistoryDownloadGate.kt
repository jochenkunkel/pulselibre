package de.pulselibre

/** Separate lifetime one-shot gate for the explicitly approved bounded history transfer. */
object CirqaLegacyHistoryDownloadGate {
    fun canAttempt(consumed: Boolean): Boolean = !consumed

    /** One separately authorized follow-up after the first directory lacked entry diagnostics. */
    fun canRunMetadataFollowUp(consumed: Boolean): Boolean = !consumed
}

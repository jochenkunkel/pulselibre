package de.pulselibre

/**
 * Offline evidence rule for a 5037 synchronization indication.
 * A historical-sync signal must be a newly validated 5037 body after
 * completion of the 5050 host write and its positive ACK.
 *
 * This rule does not authorize any BLE transmission or file download.
 */
object Cirqa5037SyncEvidenceGate {
    fun isPostHandshakeSignal(
        baselineValid5037: Int,
        currentValid5037: Int,
        responseWriteConfirmed: Boolean,
        fresh5050AckConfirmed: Boolean
    ): Boolean {
        return baselineValid5037 >= 0 &&
            currentValid5037 > baselineValid5037 &&
            responseWriteConfirmed &&
            fresh5050AckConfirmed
    }
}

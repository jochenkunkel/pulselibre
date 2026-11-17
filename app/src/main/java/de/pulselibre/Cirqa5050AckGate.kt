package de.pulselibre

/**
 * Conservative offline rule for interpreting a GFDI-5000 ACK of host 5050.
 * An ACK must not count before the complete 5050 GATT write sequence finishes.
 */
object Cirqa5050AckGate {
    fun isConfirmed(
        responseQueued: Boolean,
        allWritesConfirmed: Boolean,
        ackFor5050Observed: Boolean
    ): Boolean = responseQueued && allWritesConfirmed && ackFor5050Observed
}

package de.pulselibre

/**
 * Offline-only HANDSHAKE_COMPLETE candidate from Connect 5.31 yb60.
 * No host identity, capability change, Bluetooth I/O or automatic activation.
 */
object CirqaLegacy5030RequestCandidate {
    fun gfdiFrame(): ByteArray {
        val body = CirqaConnect531SystemEvent.body(CirqaConnect531SystemEvent.HANDSHAKE_COMPLETE)
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            CirqaConnect531SystemEvent.TYPE.toByte(),
            (CirqaConnect531SystemEvent.TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }
}

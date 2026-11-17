package de.pulselibre

/**
 * Offline-only reference-format GFDI RESPONSE(5000) encoder.
 *
 * The generic 3-byte status body is documented by independent Garmin
 * implementations, but its sufficiency for CIRQA 5050 is NOT established.
 * This object must never be wired to GATT without a separate protocol review.
 */
object CirqaOfflineGfdiStatusCandidate {
    fun ackFrame(originalType: Int): ByteArray {
        require(originalType in 5001..65535)
        val body = byteArrayOf(originalType.toByte(), (originalType ushr 8).toByte(), 0)
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            5000.toByte(), (5000 ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    /** Only verified framing, not an application-level ACK acceptance check. */
    fun isWellFormedAck(frame: ByteArray, originalType: Int): Boolean {
        if (frame.size != 9 || GarminFrameValidator.inspectVerified(frame) == null) return false
        fun u16(at: Int) = (frame[at].toInt() and 255) or ((frame[at + 1].toInt() and 255) shl 8)
        return u16(2) == 5000 && u16(4) == originalType && frame[6] == 0.toByte()
    }
}

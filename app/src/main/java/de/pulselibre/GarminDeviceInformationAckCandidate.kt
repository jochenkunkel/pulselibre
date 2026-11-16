package de.pulselibre

/**
 * Offline-only candidate for a minimal GFDI 5024 status ACK.
 *
 * The published fenix 8 watch-emulation DeviceInformation ACK includes
 * watch identity fields; these are deliberately NOT copied or transmitted.
 * A three-byte response body is only a protocol hypothesis, not verified
 * as sufficient for CIRQA bootstrap.
 */
object GarminDeviceInformationAckCandidate {
    fun frame(): ByteArray {
        val type = 5000
        val body = byteArrayOf(5024.toByte(), (5024 ushr 8).toByte(), 0)
        val length = 6 + body.size
        val prefix = byteArrayOf(
            length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun validCandidate(): Boolean {
        val bytes = frame()
        return bytes.size == 9 &&
            GarminFrameValidator.inspectVerified(bytes) != null &&
            (bytes[4].toInt() and 255) == (5024 and 255) &&
            (bytes[5].toInt() and 255) == (5024 ushr 8) &&
            bytes[6] == 0.toByte()
    }
}

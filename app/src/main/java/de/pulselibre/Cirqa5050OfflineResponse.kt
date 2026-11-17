package de.pulselibre

/**
 * Offline-only Garmin-reference-format 5050 response encoder.
 * Caller must explicitly supply verified host capability bytes.
 * Never wired to BLE; no default/guessed capabilities.
 */
object Cirqa5050OfflineResponse {
    fun frame(explicitCapabilities: ByteArray): ByteArray {
        require(explicitCapabilities.isNotEmpty() && explicitCapabilities.size <= 255)
        val body = byteArrayOf(explicitCapabilities.size.toByte()) + explicitCapabilities
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            5050.toByte(), (5050 ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }
}

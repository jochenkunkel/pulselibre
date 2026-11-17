package de.pulselibre

/**
 * Offline-only reconstruction of Garmin Connect 5.31's Legacy GFDI 5007
 * PENDING_UPLOADS_ONLY request. Construction performs no Bluetooth I/O; the
 * dedicated device test is separately one-shot gated.
 */
object CirqaLegacy5007RequestCandidate {
    const val MESSAGE_TYPE = 5007
    const val PENDING_UPLOADS_ONLY = 0x03
    const val MAX_GATT_VALUE_BYTES_AT_DEFAULT_MTU = 20

    fun body(): ByteArray = byteArrayOf(PENDING_UPLOADS_ONLY.toByte())

    /** GFDI envelope candidate: u16LE length | u16LE type | body | CRC-16/ARC LE. */
    fun gfdiFrame(): ByteArray {
        val payload = body()
        val size = payload.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + payload
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    /** Existing ML COBS framing, including zero delimiters. */
    fun cobsDelimitedFrame(): ByteArray {
        val frame = gfdiFrame()
        val output = ArrayList<Byte>(frame.size + 4)
        output.add(0)
        var index = 0
        while (index < frame.size) {
            val start = index
            while (index < frame.size && frame[index] != 0.toByte() && index - start < 254) index++
            output.add((index - start + 1).toByte())
            for (offset in start until index) output.add(frame[offset])
            if (index < frame.size && frame[index] == 0.toByte()) {
                index++
                if (index == frame.size) output.add(1)
            }
        }
        output.add(0)
        return output.toByteArray()
    }

    /**
     * Characteristic value for a runtime-assigned GFDI handle; not an ATT PDU.
     */
    fun mlGattValueCandidate(dynamicHandle: Int): ByteArray {
        require(dynamicHandle in 1..127)
        return byteArrayOf(dynamicHandle.toByte()) + cobsDelimitedFrame()
    }

    /** Connect 5.31's returned response payload uses its first byte as status. */
    fun connectResponseStatus(responsePayload: ByteArray): Int? =
        responsePayload.firstOrNull()?.toInt()?.and(0xff)

    fun isConnectResponseSupported(responsePayload: ByteArray): Boolean =
        connectResponseStatus(responsePayload) == 0
}

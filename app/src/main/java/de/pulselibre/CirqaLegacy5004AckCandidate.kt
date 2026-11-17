package de.pulselibre

/** Positive, read-only acknowledgement for one fully validated uncompressed 5004 chunk. */
object CirqaLegacy5004AckCandidate {
    const val MESSAGE_TYPE = 5000
    const val RESPONSE_TO = 5004

    fun gfdiFrame(nextOffset: Int): ByteArray {
        require(nextOffset >= 0)
        val body = byteArrayOf(
            RESPONSE_TO.toByte(), (RESPONSE_TO ushr 8).toByte(),
            0, // GFDI status = ACK
            0, // transfer status = OK
            nextOffset.toByte(), (nextOffset ushr 8).toByte(),
            (nextOffset ushr 16).toByte(), (nextOffset ushr 24).toByte()
        )
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlGattValueCandidate(dynamicHandle: Int, nextOffset: Int): ByteArray {
        require(dynamicHandle in 1..127)
        return byteArrayOf(dynamicHandle.toByte()) +
            GarminFileListRequest.cobs(gfdiFrame(nextOffset))
    }
}

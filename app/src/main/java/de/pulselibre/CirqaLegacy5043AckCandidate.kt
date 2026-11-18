package de.pulselibre

/**
 * OFFLINE CANDIDATE — not wired to any transport, never sent automatically.
 *
 * Connect 5.31 `ProtobufHandler.onMessageReceived` (t3z.java) answers every
 * incoming 5043/5044 chunk with a GFDI 5000 ACK (ly00.ACK=0) whose body is:
 *   requestId u16LE | receivedOffset u32LE | successFlag u8 | c4z status u8
 * `c4z.NO_ERROR = 0`; successFlag 0 means OK. The responseTo field is 5043.
 * Requires explicit user approval before any device transmission.
 */
object CirqaLegacy5043AckCandidate {
    const val MESSAGE_TYPE = 5000
    const val RESPONSE_TO = 5043

    fun gfdiFrame(
        requestId: Int, receivedOffset: Long, ok: Boolean = true, status: Int = 0,
        responseTo: Int = RESPONSE_TO
    ): ByteArray {
        require(responseTo in 0..0xffff)
        require(requestId in 0..0xffff)
        require(receivedOffset in 0L..0xffffffffL)
        require(status in 0..255)
        val body = byteArrayOf(
            responseTo.toByte(), (responseTo ushr 8).toByte(),
            0, // GFDI status = ly00.ACK
            requestId.toByte(), (requestId ushr 8).toByte(),
            receivedOffset.toByte(), (receivedOffset ushr 8).toByte(),
            (receivedOffset ushr 16).toByte(), (receivedOffset ushr 24).toByte(),
            if (ok) 0 else 1, status.toByte()
        )
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlGattValueCandidate(
        dynamicHandle: Int, requestId: Int, receivedOffset: Long, responseTo: Int = RESPONSE_TO
    ): ByteArray {
        require(dynamicHandle in 1..127)
        return byteArrayOf(dynamicHandle.toByte()) + GarminFileListRequest.cobs(
            gfdiFrame(requestId, receivedOffset, responseTo = responseTo)
        )
    }
}

package de.pulselibre

/**
 * Redacted observation of a CRC-verified GFDI transport ACK for a 5043 chunk.
 *
 * This is transport evidence only; it does not establish that a FileAccess
 * item-list request was accepted by the device application.
 */
internal object CirqaLegacy5043AckObservation {
    data class Ack(
        val gfdiAck: Int,
        val requestId: Int,
        val receivedOffset: Long,
        val successFlag: Int,
        val status: Int
    )

    fun parse(frame: ByteArray): Ack? {
        val header = GarminGfdiHeader.inspect(frame) ?: return null
        if (header.type != 5000 || header.responseTo != 5043 || frame.size != 17) return null
        return Ack(
            gfdiAck = u8(frame, 6),
            requestId = u8(frame, 7) or (u8(frame, 8) shl 8),
            receivedOffset = (0..3).fold(0L) { value, index ->
                value or (u8(frame, 9 + index).toLong() shl (index * 8))
            },
            successFlag = u8(frame, 13),
            status = u8(frame, 14)
        )
    }

    private fun u8(bytes: ByteArray, index: Int): Int = bytes[index].toInt() and 0xff
}

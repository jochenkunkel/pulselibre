package de.pulselibre

/**
 * OFFLINE candidate — not sent automatically, requires explicit approval.
 *
 * Smart{ file_access_service(43) = Service{ pull_item_request(1) =
 *   PullItemDataRequest{ item(1) = the exact ItemReference bytes from the
 *   item list, priority(2)=0, offset(4)=0 } } }, wrapped in a GFDI 5043 frame.
 * Read-only: requests file data, never changes device state.
 */
object CirqaFileAccessPullRequest {
    const val MESSAGE_TYPE = 5043

    fun smart(item: CirqaFileAccessItemListParser.Item): ByteArray {
        val ref = bytesField(1, item.uid) + bytesField(2, item.dataType) +
            varintField(3, item.dataSize)
        // TransportProtocol.MULTILINK_TRANSPORT_PIPE = 0 (the DEX MUTF-8 bytes
        // C0 80 encode a NUL byte, not the integer 64). Repeated enum fields keep
        // explicit zeroes on the wire. Without it: NO_SUPPORTED_TRANSPORT (2).
        val pull = bytesField(1, ref) + varintField(2, 0) + varintField(3, 0) + varintField(4, 0)
        return bytesField(43, bytesField(1, pull))
    }

    fun gfdiFrame(requestId: Int, smart: ByteArray): ByteArray {
        require(requestId in 0..0xffff)
        val body = byteArrayOf(
            requestId.toByte(), (requestId ushr 8).toByte(),
            0, 0, 0, 0,
            smart.size.toByte(), (smart.size ushr 8).toByte(), (smart.size ushr 16).toByte(), (smart.size ushr 24).toByte(),
            smart.size.toByte(), (smart.size ushr 8).toByte(), (smart.size ushr 16).toByte(), (smart.size ushr 24).toByte()
        ) + smart
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlValue(handle: Int, requestId: Int, item: CirqaFileAccessItemListParser.Item): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) + GarminFileListRequest.cobs(gfdiFrame(requestId, smart(item)))
    }

    private fun bytesField(number: Int, bytes: ByteArray): ByteArray =
        varint((number.toLong() shl 3) or 2) + varint(bytes.size.toLong()) + bytes

    private fun varintField(number: Int, value: Long): ByteArray =
        varint(number.toLong() shl 3) + varint(value)

    private fun varint(value: Long): ByteArray {
        require(value >= 0)
        var remaining = value
        val out = ArrayList<Byte>()
        do {
            val b = (remaining and 127).toInt()
            remaining = remaining ushr 7
            out.add((if (remaining == 0L) b else b or 128).toByte())
        } while (remaining != 0L)
        return out.toByteArray()
    }
}

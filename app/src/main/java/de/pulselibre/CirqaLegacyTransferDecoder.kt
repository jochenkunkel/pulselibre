package de.pulselibre

/**
 * Offline-only decoders for Connect 5.31 legacy GFDI transfer bodies.
 *
 * No BLE transport, CRC acceptance or ACK sender. The independently
 * reconstructed 5002 request codec and CRC-16/ARC helper live in separate
 * offline-only classes; DEX confirms yw40 compares the updated CRC accumulator.
 */
object CirqaLegacyTransferDecoder {
    enum class ReadStatus {
        SUCCESS, FILE_INDEX_DOES_NOT_EXIST, FILE_INDEX_NOT_READABLE,
        NOT_READY, INVALID_REQUEST, CRC_MISMATCH,
        DATA_REQUESTED_EXCEEDS_FILE_SIZE
    }

    data class ReadResponse(val status: ReadStatus, val sizeBytes: ByteArray?)

    fun readResponse(body: ByteArray): ReadResponse? {
        if (body.isEmpty()) return null
        val status = when (body[0].toInt() and 255) {
            0 -> ReadStatus.SUCCESS
            1 -> ReadStatus.FILE_INDEX_DOES_NOT_EXIST
            2 -> ReadStatus.FILE_INDEX_NOT_READABLE
            3 -> ReadStatus.NOT_READY
            4 -> ReadStatus.INVALID_REQUEST
            5 -> ReadStatus.CRC_MISMATCH
            6 -> ReadStatus.DATA_REQUESTED_EXCEEDS_FILE_SIZE
            else -> return null
        }
        // a60.F is verified as unsigned u32 LE; preserve bytes for the
        // bounded interpretation in CirqaLegacy5002Codec.
        if (status == ReadStatus.SUCCESS && body.size < 5) return null
        return ReadResponse(status, if (status == ReadStatus.SUCCESS)
            body.copyOfRange(1, 5) else null)
    }

    data class PlainChunk(
        val transferStatus: Int,
        val crc16: Int,
        val offset: Long,
        val payload: ByteArray
    )

    fun plainChunk(body: ByteArray): PlainChunk? {
        if (body.size < 7) return null
        fun u8(i: Int) = body[i].toInt() and 255
        val offset = u8(3).toLong() or (u8(4).toLong() shl 8) or
            (u8(5).toLong() shl 16) or (u8(6).toLong() shl 24)
        return PlainChunk(
            transferStatus = u8(0),
            crc16 = u8(1) or (u8(2) shl 8),
            offset = offset,
            payload = body.copyOfRange(7, body.size)
        )
    }

    data class CompressedChunk(
        val sequence: Int,
        val flags: Int,
        val uncompressedOffset: Long,
        val expectedCrc16: Int,
        val compressedBytes: ByteArray
    )

    fun compressedChunk(body: ByteArray): CompressedChunk? {
        if (body.size < 8) return null
        fun u8(i: Int) = body[i].toInt() and 255
        val offset = u8(2).toLong() or (u8(3).toLong() shl 8) or
            (u8(4).toLong() shl 16) or (u8(5).toLong() shl 24)
        return CompressedChunk(
            sequence = u8(0), flags = u8(1),
            uncompressedOffset = offset,
            expectedCrc16 = u8(6) or (u8(7) shl 8),
            compressedBytes = body.copyOfRange(8, body.size)
        )
    }
}

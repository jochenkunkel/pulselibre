package de.pulselibre

/**
 * Offline-only FileSync V2 directory request builder.
 *
 * The optional cursor and page are wire-level pagination hints from the
 * GarminHRM600 public reference. Neither is sent to CIRQA by this class.
 * No type, date, sync-status or FIT filter is applied.
 */
object GarminFileListPagination {
    fun smartRequest(cursorId: Long? = null, startPageId: Long? = null): ByteArray {
        require(cursorId == null || startPageId == null) { "Use either cursor or page, never both" }
        require(cursorId == null || cursorId >= 0)
        require(startPageId == null || startPageId >= 0)
        val request = when {
            cursorId != null -> varintField(1, cursorId)
            startPageId != null -> varintField(2, startPageId)
            else -> byteArrayOf()
        }
        val fileSync = bytesField(9, request)
        return bytesField(43, fileSync)
    }

    fun gfdi(sequence: Int, counter: Int, cursorId: Long? = null, startPageId: Long? = null): ByteArray =
        GarminCompactTransport.frame(0x39, sequence, counter, smartRequest(cursorId, startPageId))

    private fun bytesField(field: Int, bytes: ByteArray): ByteArray =
        varint((field.toLong() shl 3) or 2) + varint(bytes.size.toLong()) + bytes

    private fun varintField(field: Int, value: Long): ByteArray =
        varint(field.toLong() shl 3) + varint(value)

    private fun varint(value: Long): ByteArray {
        require(value >= 0)
        var remaining = value
        val result = ArrayList<Byte>()
        do {
            val b = (remaining and 127).toInt()
            remaining = remaining ushr 7
            result.add((if (remaining == 0L) b else b or 128).toByte())
        } while (remaining != 0L)
        return result.toByteArray()
    }
}

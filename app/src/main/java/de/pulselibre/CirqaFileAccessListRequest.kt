package de.pulselibre

/**
 * OFFLINE candidate — not sent automatically, requires explicit approval.
 *
 * Connect 5.31 (`com.garmin.device.filetransfer.b.i`) wraps
 * `GDIFileAccess.Service.item_list_request` (field 9) into
 * `GDISmartProto.Smart.file_access_service` (field 43) and sends it through the
 * protobuf messenger (GFDI 5043), NOT through the compact transport.
 *
 * This is a read-only item list request: no archive, flags, delete or reset.
 */
object CirqaFileAccessListRequest {
    const val MESSAGE_TYPE = 5043

    /**
     * Connect's listing loop continues only while a response has session_id.
     * next_transaction_id is returned as transaction context for a later
     * listing/sync; it is not the cursor for the current item-list session.
     */
    fun nextPageSessionId(page: CirqaFileAccessItemListParser.ItemListPage): Long? =
        page.sessionId

    /** Connect FileAccess ItemListRequest fields: session_id=1, transaction_id=2. */
    fun smartRequest(sessionId: Long? = null, transactionId: Long? = null): ByteArray {
        require(sessionId == null || transactionId == null) {
            "Use the active session ID or the next transaction ID, never both"
        }
        require(sessionId == null || sessionId in 0..0xffff_ffffL)
        require(transactionId == null || transactionId in 0..0xffff_ffffL)
        val request = when {
            sessionId != null -> varintField(1, sessionId)
            transactionId != null -> varintField(2, transactionId)
            else -> byteArrayOf()
        }
        return bytesField(43, bytesField(9, request))
    }

    /** One complete, unfragmented Smart protobuf in a 5043 transport frame. */
    fun gfdiFrame(requestId: Int, smart: ByteArray = GarminFileListPagination.smartRequest()): ByteArray {
        require(requestId in 0..0xffff)
        require(smart.isNotEmpty() && smart.size <= 32 * 1024)
        val body = byteArrayOf(
            requestId.toByte(), (requestId ushr 8).toByte(),
            0, 0, 0, 0,                                  // offset u32 LE = 0
            smart.size.toByte(), (smart.size ushr 8).toByte(),
            (smart.size ushr 16).toByte(), (smart.size ushr 24).toByte(), // total
            smart.size.toByte(), (smart.size ushr 8).toByte(),
            (smart.size ushr 16).toByte(), (smart.size ushr 24).toByte()  // chunk
        ) + smart
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlValue(
        handle: Int,
        requestId: Int,
        sessionId: Long? = null,
        transactionId: Long? = null
    ): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) +
            GarminFileListRequest.cobs(gfdiFrame(requestId, smartRequest(sessionId, transactionId)))
    }

    private fun varintField(number: Int, value: Long): ByteArray {
        require(number in 1..15 && value >= 0)
        val out = java.io.ByteArrayOutputStream()
        writeVarint(out, (number shl 3).toLong())
        writeVarint(out, value)
        return out.toByteArray()
    }

    private fun bytesField(number: Int, value: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        writeVarint(out, ((number shl 3) or 2).toLong())
        writeVarint(out, value.size.toLong())
        out.write(value)
        return out.toByteArray()
    }

    private fun writeVarint(out: java.io.ByteArrayOutputStream, value: Long) {
        var remaining = value
        while (remaining and 0x7f.inv().toLong() != 0L) {
            out.write((remaining.toInt() and 0x7f) or 0x80)
            remaining = remaining ushr 7
        }
        out.write(remaining.toInt())
    }
}

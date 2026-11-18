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

    fun mlValue(handle: Int, requestId: Int): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) +
            GarminFileListRequest.cobs(gfdiFrame(requestId))
    }
}

package de.pulselibre

/**
 * OFFLINE candidate — not sent automatically, requires explicit approval.
 *
 * Connect 5.31 `SyncMessageManager.syncNow()` sends
 * `Smart{ core_service{ sync_request{ use_this_connection=false } } }` through
 * the GFDI protobuf messenger (5043) and waits for `sync_response` before file
 * transfer starts. Binary evidence: GDICoreExtension `core_service` = field 13,
 * CoreService `sync_request` = field 1; proto3 omits the default-false bool,
 * so the nested SyncRequest message is empty on the wire.
 *
 * CAUTION: a sync request may make the device start uploading buffered data and
 * may trigger device-side disposition (e.g. archiving after a successful
 * upload). We never send archive/flag commands ourselves. Explicit release
 * required before any transmission.
 */
object CirqaSyncRequestCandidate {
    const val MESSAGE_TYPE = 5043

    /** Smart{ field13 = CoreService{ field1 = empty SyncRequest } } */
    fun smart(): ByteArray = byteArrayOf(0x6a, 0x02, 0x0a, 0x00)

    fun gfdiFrame(requestId: Int): ByteArray {
        require(requestId in 0..0xffff)
        val smart = smart()
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

    fun mlValue(handle: Int, requestId: Int): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) + GarminFileListRequest.cobs(gfdiFrame(requestId))
    }
}

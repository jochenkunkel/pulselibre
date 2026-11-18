package de.pulselibre

/** Self-contained GFDI request builders for Historical Sync V1. Offline only. */
object CirqaHistoricalSyncV1Wire {
    fun gfdiFrame(type: Int, body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlValue(handle: Int, type: Int, body: ByteArray): ByteArray {
        require(handle in 1..127)
        return byteArrayOf(handle.toByte()) + GarminFileListRequest.cobs(gfdiFrame(type, body))
    }

    fun systemEventComplete(handle: Int): ByteArray = mlValue(handle, 5030, byteArrayOf(8, 0))
    // Connect 5.31 filter enum zgd: NO_FILTER=0, DEFAULT=1, PENDING_UPLOADS_ONLY=3.
    const val NO_FILTER = 0
    const val DEFAULT_FILTER = 1
    const val PENDING_UPLOADS_ONLY = 3

    fun filter(handle: Int, value: Int): ByteArray {
        require(value in 0..255)
        return mlValue(handle, 5007, byteArrayOf(value.toByte()))
    }

    /** Historical filter used by the current gated combined run. */
    const val DIRECTORY_FILTER = NO_FILTER

    fun filterPendingUploads(handle: Int): ByteArray = filter(handle, PENDING_UPLOADS_ONLY)
    fun supportedFileTypes(handle: Int): ByteArray = mlValue(handle, 5031, byteArrayOf())
    fun directoryRead(handle: Int, index: Int = 0): ByteArray {
        val body = byteArrayOf(
            index.toByte(), (index ushr 8).toByte(), 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
        )
        return mlValue(handle, 5002, body)
    }
}

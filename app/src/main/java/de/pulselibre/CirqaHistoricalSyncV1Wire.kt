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
    fun filterPendingUploads(handle: Int): ByteArray = mlValue(handle, 5007, byteArrayOf(3))
    fun supportedFileTypes(handle: Int): ByteArray = mlValue(handle, 5031, byteArrayOf())
    fun directoryRead(handle: Int, index: Int = 0): ByteArray {
        val body = byteArrayOf(
            index.toByte(), (index ushr 8).toByte(), 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
        )
        return mlValue(handle, 5002, body)
    }
}

package de.pulselibre

/** Offline-only Connect 5.31 request candidate; an empty 5031 body. */
object CirqaLegacy5031RequestCandidate {
    const val MESSAGE_TYPE = 5031

    fun body(): ByteArray = byteArrayOf()

    fun gfdiFrame(): ByteArray {
        val prefix = byteArrayOf(6, 0, MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte())
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlGattValueCandidate(dynamicHandle: Int): ByteArray {
        require(dynamicHandle in 1..127)
        return byteArrayOf(dynamicHandle.toByte()) + GarminFileListRequest.cobs(gfdiFrame())
    }
}

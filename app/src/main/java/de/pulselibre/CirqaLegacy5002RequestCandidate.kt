package de.pulselibre

/** Connect 5.31 legacy directory/file read request candidate. */
object CirqaLegacy5002RequestCandidate {
    const val MESSAGE_TYPE = 5002

    fun gfdiFrame(fileIndex: Int, compressed: Boolean = false): ByteArray {
        val body = CirqaLegacy5002Codec.request(fileIndex, compressed)
        val size = body.size + 6
        val prefix = byteArrayOf(
            size.toByte(), (size ushr 8).toByte(),
            MESSAGE_TYPE.toByte(), (MESSAGE_TYPE ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    fun mlGattValueCandidate(dynamicHandle: Int, fileIndex: Int, compressed: Boolean = false): ByteArray {
        require(dynamicHandle in 1..127)
        return byteArrayOf(dynamicHandle.toByte()) + GarminFileListRequest.cobs(
            gfdiFrame(fileIndex, compressed)
        )
    }
}

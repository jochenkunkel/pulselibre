package de.pulselibre

/**
 * Offline-only Garmin compact ACK encoder, based on GarminHRM600 gfdi.py.
 * Nothing here authorizes transmission: CIRQA must first demonstrate support.
 */
object GarminCompactAck {
    fun generic(kind: Int, sequence: Int): ByteArray {
        require(kind in 0..255 && sequence in 1..127)
        val original = 5000 + kind
        return wrap(sequence, byteArrayOf(original.toByte(), (original ushr 8).toByte(), 0))
    }

    fun chunk(kind: Int, sequence: Int, counter: Int, offset: Long): ByteArray {
        require(kind in listOf(0x2b, 0x2c))
        require(sequence in 1..127 && counter in 0..65535)
        require(offset in 0..0xffffffffL)
        val original = 5000 + kind
        val body = byteArrayOf(
            original.toByte(), (original ushr 8).toByte(), 0,
            counter.toByte(), (counter ushr 8).toByte(),
            offset.toByte(), (offset ushr 8).toByte(),
            (offset ushr 16).toByte(), (offset ushr 24).toByte(),
            0, 0
        )
        return wrap(sequence, body)
    }

    private fun wrap(sequence: Int, body: ByteArray): ByteArray {
        val type = 0x8000 or (sequence shl 8)
        val length = body.size + 6
        val prefix = byteArrayOf(
            length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }
}

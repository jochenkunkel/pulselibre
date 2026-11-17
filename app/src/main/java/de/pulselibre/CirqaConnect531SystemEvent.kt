package de.pulselibre

/**
 * Offline Connect 5.31 GFDI 5030 system-event body characterization.
 * From the examined SystemEventSender: [eventCode, 0].
 * No BLE transmission, session counters or CIRQA capability assumptions.
 */
object CirqaConnect531SystemEvent {
    const val TYPE = 5030
    const val HOST_DID_ENTER_FOREGROUND = 6
    const val HANDSHAKE_COMPLETE = 8

    fun body(eventCode: Int): ByteArray {
        require(eventCode in 0..255)
        return byteArrayOf(eventCode.toByte(), 0)
    }

    fun inspect(body: ByteArray): Int? {
        if (body.size != 2 || body[1] != 0.toByte()) return null
        return body[0].toInt() and 255
    }

    fun isHandshakeComplete(body: ByteArray): Boolean =
        inspect(body) == HANDSHAKE_COMPLETE
}

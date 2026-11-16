package de.pulselibre

/**
 * Offline-only inspection of a Garmin GFDI packet header.
 * Never accepts BLE advertising payloads as GFDI packets; callers must supply
 * bytes from a separately authorized and identified Garmin protocol source.
 * CRC and message body are intentionally NOT decoded or validated here.
 */
object GarminFrameHeader {
    data class Header(val declaredLength: Int, val messageType: Int, val knownType: String)

    fun inspect(packet: ByteArray): Header? {
        if (packet.size < 6 || packet.size > 65535) return null
        val length = u16(packet, 0)
        if (length != packet.size) return null
        val type = u16(packet, 2)
        val name = when (type) {
            5000 -> "STATUS"
            5002 -> "DOWNLOAD_REQUEST"
            5004 -> "FILE_TRANSFER_DATA"
            5011 -> "FIT_DEFINITION"
            5012 -> "FIT_DATA"
            5024 -> "DEVICE_INFORMATION"
            5043 -> "PROTOBUF_REQUEST"
            5044 -> "PROTOBUF_RESPONSE"
            5101 -> "AUTH_NEGOTIATION"
            else -> "UNKNOWN"
        }
        return Header(length, type, name)
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
}

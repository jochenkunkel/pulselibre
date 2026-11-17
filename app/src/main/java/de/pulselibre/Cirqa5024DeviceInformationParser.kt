package de.pulselibre

/**
 * Offline-only parser for the fixed Garmin Connect 5.31 DeviceInformation
 * layout and three length-prefixed UTF-8 strings. Optional product-specific
 * trailing bytes are not interpreted or accepted as host capabilities.
 */
object Cirqa5024DeviceInformationParser {
    data class DeviceInformation(
        val protocolVersion: Int,
        val productNumber: Int,
        val unitId: Long,
        val softwareVersion: Int,
        val maxPacketSize: Int,
        val strings: List<String>,
        val optionalTrailingBytes: Int
    )

    fun parse(body: ByteArray): DeviceInformation? {
        var offset = 0
        fun u8(): Int? = if (offset < body.size) body[offset++].toInt() and 255 else null
        fun u16(): Int? {
            val lo = u8() ?: return null
            val hi = u8() ?: return null
            return lo or (hi shl 8)
        }
        val protocol = u16() ?: return null
        val product = u16() ?: return null
        val low = u16() ?: return null
        val high = u16() ?: return null
        val unit = low.toLong() or (high.toLong() shl 16)
        val software = u16() ?: return null
        val maxPacket = u16() ?: return null
        val strings = ArrayList<String>(3)
        repeat(3) {
            val size = u8() ?: return null
            if (size > body.size - offset) return null
            val bytes = body.copyOfRange(offset, offset + size)
            val decoded = try {
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (_: java.nio.charset.CharacterCodingException) { return null }
            strings.add(decoded)
            offset += size
        }
        return DeviceInformation(protocol, product, unit, software, maxPacket,
            strings, body.size - offset)
    }
}

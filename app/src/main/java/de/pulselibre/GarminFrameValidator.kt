package de.pulselibre

/**
 * Pure offline integrity check for a complete GFDI frame.
 * Uses the standard reflected CRC-16/ARC parameters (poly 0xA001, init 0).
 * Does not parse FIT data, touch Bluetooth, or establish device identity.
 */
object GarminFrameValidator {
    fun crc16Arc(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Int {
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
        var crc = 0
        for (i in offset until offset + length) {
            crc = crc xor (bytes[i].toInt() and 0xff)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xffff
    }

    /** Null means incomplete, length mismatch, or CRC failure. */
    fun inspectVerified(packet: ByteArray): GarminFrameHeader.Header? {
        val header = GarminFrameHeader.inspect(packet) ?: return null
        val crcOffset = packet.size - 2
        val expected = (packet[crcOffset].toInt() and 0xff) or
            ((packet[crcOffset + 1].toInt() and 0xff) shl 8)
        return header.takeIf { crc16Arc(packet, 0, crcOffset) == expected }
    }
}

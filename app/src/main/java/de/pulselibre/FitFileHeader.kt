package de.pulselibre

/**
 * Strict, offline-only inspection of a complete FIT file envelope.
 *
 * This is NOT a FIT record decoder or proof of Garmin/CIRQA origin.
 * The CRC is an integrity check, not cryptographic authentication.
 * No FIT records or health measurements are decoded.
 */
object FitFileHeader {
    const val MAX_FILE_BYTES = 8 * 1024 * 1024

    data class Header(
        val headerSize: Int,
        val protocolVersion: Int,
        val profileVersion: Int,
        val dataSize: Long
    )

    /**
     * FIT uses CRC-16/ARC (reflected polynomial 0xA001, initial value zero).
     * Bounds are checked before any byte access.
     */
    fun crc16(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): Int {
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
        var crc = 0
        for (index in offset until offset + length) {
            crc = crc xor (bytes[index].toInt() and 0xff)
            repeat(8) {
                crc = if ((crc and 1) != 0) (crc ushr 1) xor 0xA001 else crc ushr 1
            }
        }
        return crc and 0xffff
    }

    /** Rejects structurally invalid envelopes or CRC mismatches. */
    fun inspectVerified(file: ByteArray): Header? {
        val header = inspect(file) ?: return null
        if (header.headerSize == 14 && crc16(file, 0, 14) != 0) return null
        return header.takeIf { crc16(file) == 0 }
    }

    /** Accepts standard 12- and 14-byte FIT headers and an exact file envelope. */
    fun inspect(file: ByteArray): Header? {
        if (file.size < 14 || file.size > MAX_FILE_BYTES) return null
        val headerSize = file[0].toInt() and 0xff
        if (headerSize != 12 && headerSize != 14) return null
        if (file.size < headerSize + 2) return null
        if (file[8] != '.'.code.toByte() || file[9] != 'F'.code.toByte() ||
            file[10] != 'I'.code.toByte() || file[11] != 'T'.code.toByte()) return null
        val dataSize = (file[4].toLong() and 0xff) or
            ((file[5].toLong() and 0xff) shl 8) or
            ((file[6].toLong() and 0xff) shl 16) or
            ((file[7].toLong() and 0xff) shl 24)
        if (dataSize + headerSize + 2L != file.size.toLong()) return null
        val profileVersion = (file[2].toInt() and 0xff) or
            ((file[3].toInt() and 0xff) shl 8)
        return Header(headerSize, file[1].toInt() and 0xff, profileVersion, dataSize)
    }
}

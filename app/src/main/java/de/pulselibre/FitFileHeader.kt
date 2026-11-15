package de.pulselibre

/**
 * Strict, offline-only inspection of a complete FIT file envelope.
 *
 * This is NOT a FIT record decoder or proof of Garmin/CIRQA origin.
 * Header/file CRC bytes are reserved but not yet verified; consumers must
 * not interpret records as authenticated or validated measurements.
 */
object FitFileHeader {
    const val MAX_FILE_BYTES = 8 * 1024 * 1024

    data class Header(
        val headerSize: Int,
        val protocolVersion: Int,
        val profileVersion: Int,
        val dataSize: Long
    )

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

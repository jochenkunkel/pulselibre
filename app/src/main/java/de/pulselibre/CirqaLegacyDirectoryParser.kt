package de.pulselibre

/**
 * Offline parser for the legacy GFDI directory file (file index 0).
 * Based on Connect 5.31 FileManager observations, not verified on CIRQA.
 * The first 16 bytes are opaque; trailing incomplete entries are rejected
 * instead of silently discarded as in Connect.
 */
object CirqaLegacyDirectoryParser {
    data class Entry(
        val fileIndex: Int,
        val type: Int,
        val subtypeBytes: List<Int>,
        val reserved: Int,
        val attributes: Int,
        val fieldA: Long,
        val fieldB: Long
    ) {
        /** Identified by Connect's k7h model; still not validated on CIRQA. */
        val size: Long get() = fieldA
        val timestamp: Long get() = fieldB
        val subtype: Int get() = subtypeBytes[0]
        val readable: Boolean get() = attributes and 0x80 != 0
    }

    data class Directory(val header: ByteArray, val entries: List<Entry>)

    fun parse(bytes: ByteArray): Directory? {
        if (bytes.size !in 16..CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES ||
            (bytes.size - 16) % 16 != 0) return null
        fun u8(i: Int) = bytes[i].toInt() and 255
        fun u16(i: Int) = u8(i) or (u8(i + 1) shl 8)
        fun u32(i: Int): Long =
            u8(i).toLong() or (u8(i + 1).toLong() shl 8) or
                (u8(i + 2).toLong() shl 16) or (u8(i + 3).toLong() shl 24)
        val entries = (16 until bytes.size step 16).map { i ->
            Entry(
                fileIndex = u16(i),
                type = u8(i + 2),
                subtypeBytes = listOf(u8(i + 3), u8(i + 4), u8(i + 5)),
                reserved = u8(i + 6),
                attributes = u8(i + 7),
                fieldA = u32(i + 8),
                fieldB = u32(i + 12)
            )
        }
        return Directory(bytes.copyOfRange(0, 16), entries)
    }

    /** A structural parser may retain duplicate rows for analysis; a downloader must not. */
    fun parseForDownload(bytes: ByteArray): Directory? {
        val directory = parse(bytes) ?: return null
        return directory.takeIf {
            it.entries.map(Entry::fileIndex).toSet().size == it.entries.size
        }
    }
}

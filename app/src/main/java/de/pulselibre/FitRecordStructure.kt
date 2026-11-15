package de.pulselibre

/**
 * Offline FIT record framing only. No field values, timestamps or health metrics
 * are decoded. Only accepts files whose header and file CRC pass validation.
 */
object FitRecordStructure {
    data class Definition(val localType: Int, val globalMessage: Int, val fieldCount: Int)
    data class Summary(val definitions: List<Definition>, val dataRecords: Int)

    fun inspect(file: ByteArray): Summary? {
        val header = FitFileHeader.inspectVerified(file) ?: return null
        val end = header.headerSize + header.dataSize.toInt()
        val definitions = arrayOfNulls<Int>(16)
        val result = mutableListOf<Definition>()
        var dataRecords = 0
        var cursor = header.headerSize
        fun take(count: Int): Int? {
            if (count < 0 || cursor > end - count) return null
            val old = cursor
            cursor += count
            return old
        }
        while (cursor < end) {
            val record = take(1) ?: return null
            val tag = file[record].toInt() and 0xff
            if (tag and 0x80 != 0) {
                // Compressed timestamp headers reference a previously defined local type.
                val localType = (tag ushr 5) and 0x03
                val length = definitions[localType] ?: return null
                if (take(length) == null) return null
                dataRecords++
                continue
            }
            val localType = tag and 0x0f
            if (tag and 0x40 != 0) {
                val start = take(5) ?: return null
                val architecture = file[start + 1].toInt() and 0xff
                if (architecture != 0 && architecture != 1) return null
                val global = if (architecture == 0)
                    (file[start + 2].toInt() and 0xff) or ((file[start + 3].toInt() and 0xff) shl 8)
                else
                    ((file[start + 2].toInt() and 0xff) shl 8) or (file[start + 3].toInt() and 0xff)
                val fields = file[start + 4].toInt() and 0xff
                val fieldStart = take(fields * 3) ?: return null
                var recordLength = 0
                for (index in 0 until fields) {
                    recordLength += file[fieldStart + index * 3 + 1].toInt() and 0xff
                }
                if (tag and 0x20 != 0) {
                    val developerCountOffset = take(1) ?: return null
                    val developerFields = file[developerCountOffset].toInt() and 0xff
                    val developerStart = take(developerFields * 3) ?: return null
                    for (index in 0 until developerFields) {
                        recordLength += file[developerStart + index * 3 + 1].toInt() and 0xff
                    }
                }
                definitions[localType] = recordLength
                result.add(Definition(localType, global, fields))
            } else {
                // Bit 5 is reserved for normal data record headers.
                if (tag and 0x20 != 0) return null
                val length = definitions[localType] ?: return null
                if (take(length) == null) return null
                dataRecords++
            }
        }
        return Summary(result, dataRecords)
    }
}

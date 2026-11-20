package de.pulselibre

/**
 * Reads a few raw example values of ONE field from a CRC-verified FIT original, for the explicit "Beispielbytes ansehen"
 * action in the unknown-fields list (#204 phase B, item 8). Read-only and bounded: nothing is stored, nothing is
 * interpreted (no unit, no scale, no medical label) and nothing leaves the device.
 *
 * It only walks the FIT record structure (definitions, compressed-timestamp headers, developer fields) like the decoder
 * does and returns the bytes of the requested field. A structurally invalid file returns null (fail closed).
 */
internal object FitFieldSampleReader {
    const val MAX_SAMPLES = 3
    const val MAX_BYTES_PER_SAMPLE = 32

    /** [samples] holds at most [MAX_SAMPLES] values, each cut to [MAX_BYTES_PER_SAMPLE]; [occurrences] is the full count. */
    data class Preview(val samples: List<ByteArray>, val occurrences: Int, val truncatedBytes: Boolean)

    private class Definition(
        val global: Int, val bigEndian: Boolean,
        val fields: List<Triple<Int, Int, Int>>,   // number, size, baseType
        val developer: List<Triple<Int, Int, Int>> // number, size, developer data index
    )

    /**
     * @param developerIndex null for a standard field; the developer data index for a developer field.
     * @return null for an invalid file, otherwise the preview (possibly with zero occurrences).
     */
    fun read(file: ByteArray, global: Int, fieldNumber: Int, developerIndex: Int?): Preview? {
        val header = FitFileHeader.inspectVerified(file) ?: return null
        val dataEnd = header.headerSize + header.dataSize.toInt()
        val definitions = arrayOfNulls<Definition>(16)
        val samples = ArrayList<ByteArray>(MAX_SAMPLES)
        var occurrences = 0
        var cut = false
        var cursor = header.headerSize

        fun need(count: Int) = count >= 0 && cursor <= dataEnd - count
        fun u8(index: Int) = file[index].toInt() and 0xff

        while (cursor < dataEnd) {
            val headerByte = u8(cursor++)
            val compressed = headerByte and 0x80 != 0
            if (!compressed && headerByte and 0x40 != 0) {
                if (headerByte and 0x10 != 0 || !need(5)) return null
                val localType = headerByte and 0x0f
                val reserved = u8(cursor++)
                val architecture = u8(cursor++)
                if (reserved != 0 || architecture !in 0..1) return null
                val big = architecture == 1
                val globalNumber = if (big) (u8(cursor) shl 8) or u8(cursor + 1) else (u8(cursor + 1) shl 8) or u8(cursor)
                cursor += 2
                val fieldCount = u8(cursor++)
                if (!need(fieldCount * 3)) return null
                val fields = ArrayList<Triple<Int, Int, Int>>(fieldCount)
                repeat(fieldCount) {
                    val number = u8(cursor); val size = u8(cursor + 1); val type = u8(cursor + 2)
                    cursor += 3
                    if (size == 0) return null
                    fields += Triple(number, size, type)
                }
                val developer = ArrayList<Triple<Int, Int, Int>>()
                if (headerByte and 0x20 != 0) {
                    if (!need(1)) return null
                    val count = u8(cursor++)
                    if (!need(count * 3)) return null
                    repeat(count) {
                        val number = u8(cursor); val size = u8(cursor + 1); val index = u8(cursor + 2)
                        cursor += 3
                        if (size == 0) return null
                        developer += Triple(number, size, index)
                    }
                }
                definitions[localType] = Definition(globalNumber, big, fields, developer)
                continue
            }
            if (!compressed && headerByte and 0x30 != 0) return null
            val localType = if (compressed) (headerByte ushr 5) and 0x03 else headerByte and 0x0f
            val definition = definitions[localType] ?: return null
            val timestampField = if (compressed) definition.fields.firstOrNull { it.first == 253 } ?: return null else null
            for (field in definition.fields) {
                if (compressed && field == timestampField) continue // carried by the compressed header, no bytes here
                if (!need(field.second)) return null
                if (developerIndex == null && definition.global == global && field.first == fieldNumber) {
                    occurrences++
                    if (samples.size < MAX_SAMPLES) {
                        val length = minOf(field.second, MAX_BYTES_PER_SAMPLE)
                        if (length < field.second) cut = true
                        samples += file.copyOfRange(cursor, cursor + length)
                    }
                }
                cursor += field.second
            }
            for (field in definition.developer) {
                if (!need(field.second)) return null
                if (developerIndex != null && definition.global == global &&
                    field.first == fieldNumber && field.third == developerIndex) {
                    occurrences++
                    if (samples.size < MAX_SAMPLES) {
                        val length = minOf(field.second, MAX_BYTES_PER_SAMPLE)
                        if (length < field.second) cut = true
                        samples += file.copyOfRange(cursor, cursor + length)
                    }
                }
                cursor += field.second
            }
        }
        return if (cursor == dataEnd) Preview(samples, occurrences, cut) else null
    }

    /** Text for one source file in the preview dialog; [index] is a local ordinal, never a file name or hash. */
    fun describe(index: Int, preview: Preview?): String {
        val title = "Datei $index"
        if (preview == null) return "$title: Original nicht lesbar oder nicht gültig."
        if (preview.occurrences == 0) return "$title: Feld in dieser Datei nicht vorhanden."
        val shown = preview.samples.mapIndexed { i, b -> "  ${i + 1}: ${hex(b)}" }
        val more = preview.occurrences - preview.samples.size
        return buildString {
            append("$title: ${preview.occurrences} Vorkommen, erste ${preview.samples.size} Werte\n")
            append(shown.joinToString("\n"))
            if (more > 0) append("\n  … und $more weitere")
            if (preview.truncatedBytes) append("\n  (je Wert auf $MAX_BYTES_PER_SAMPLE Byte gekürzt)")
        }
    }

    /** "0A FF 03" – uppercase hex pairs, no interpretation. */
    fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xff) }
}

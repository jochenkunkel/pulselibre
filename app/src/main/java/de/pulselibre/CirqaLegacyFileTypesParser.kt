package de.pulselibre

/** Strict offline decoder for GFDI 5031 Supported File Types response. */
object CirqaLegacyFileTypesParser {
    data class FileType(val type: Int, val subtype: Int, val name: String)

    fun parse(body: ByteArray): List<FileType>? {
        if (body.isEmpty()) return null
        var offset = 1
        val count = body[0].toInt() and 255
        val result = ArrayList<FileType>(count)
        repeat(count) {
            if (offset + 3 > body.size) return null
            val type = body[offset++].toInt() and 255
            val subtype = body[offset++].toInt() and 255
            val length = body[offset++].toInt() and 255
            if (length > body.size - offset) return null
            val bytes = body.copyOfRange(offset, offset + length)
            offset += length
            val decoder = Charsets.UTF_8.newDecoder()
            val name = try {
                decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (_: java.nio.charset.CharacterCodingException) {
                return null
            }
            result.add(FileType(type, subtype, name))
        }
        if (offset != body.size) return null
        return result
    }
}

package de.pulselibre

/**
 * Experimental read-only FileSync V2 directory query, derived from public protocol facts
 * documented by MIT-licensed GarminHRM600. Never sends FileRequest or FileSetFlags.
 * A response is only reported after validated COBS + GFDI length + CRC.
 */
class GarminFileListProbe {
    private val pending = ArrayList<Byte>()
    private var collecting = false
    var frames = 0
        private set
    var validLists = 0
        private set
    var fileEntries = 0
        private set
    var rejectedFrames = 0
        private set
    val frameTypes = linkedMapOf<String, Int>()
    var otherMessages = 0
        private set

    fun summary(): String = "GFDI-Frames: $frames; FileListResponse: $validLists; " +
        "Dateieinträge: $fileEntries; Typen: $frameTypes; andere Nachrichten: $otherMessages; ungültige Frames: $rejectedFrames"

    fun observeMlPayload(payload: ByteArray) {
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && pending.isNotEmpty()) decode(pending.toByteArray())
                pending.clear()
                collecting = true
            } else if (collecting) {
                if (pending.size >= 8192) {
                    pending.clear()
                    collecting = false
                    rejectedFrames++
                } else pending.add(byte)
            }
        }
    }

    private fun decode(encoded: ByteArray) {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < encoded.size) {
            val code = encoded[i++].toInt() and 255
            if (code == 0 || i + code - 1 > encoded.size) { rejectedFrames++; return }
            repeat(code - 1) { out.add(encoded[i++]) }
            if (code != 255 && i < encoded.size) out.add(0)
        }
        val frame = out.toByteArray()
        if (GarminFrameValidator.inspectVerified(frame) == null) { rejectedFrames++; return }
        frames++
        val type = u16(frame, 2)
        val label = if (type and 0x8000 != 0) "compact-${type and 255}" else "gfdi-$type"
        frameTypes[label] = (frameTypes[label] ?: 0) + 1
        if (type and 0x8000 == 0 || type and 255 !in listOf(0x2b, 0x2c, 0x39, 0x3a)) {
            otherMessages++
            return
        }
        val body = frame.copyOfRange(4, frame.size - 2)
        val payload = if (type and 255 in listOf(0x2b, 0x2c)) {
            // Chunked protobuf responses need transport ACK/reassembly.
            // Do not mistake a partial chunk for a valid list.
            otherMessages++
            return
        } else if (body.size >= 2) body.copyOfRange(2, body.size) else return
        parseSmart(payload)
    }

    private fun parseSmart(bytes: ByteArray) {
        for ((field, value) in fields(bytes)) {
            if (field != 43 || value == null) continue
            for ((sub, data) in fields(value)) {
                if (sub != 10 || data == null) continue
                validLists++
                fileEntries += fields(data).count { it.first == 4 && it.second != null }
            }
        }
    }

    // Bounded wire-type 0/1/2/5 reader; malformed protobuf is ignored.
    private fun fields(bytes: ByteArray): List<Pair<Int, ByteArray?>> {
        val result = ArrayList<Pair<Int, ByteArray?>>()
        var i = 0
        fun varint(): Long? {
            var n = 0L
            for (shift in 0..63 step 7) {
                if (i >= bytes.size) return null
                val b = bytes[i++].toInt() and 255
                n = n or ((b and 127).toLong() shl shift)
                if (b and 128 == 0) return n
            }
            return null
        }
        while (i < bytes.size && result.size < 256) {
            val tag = varint() ?: break
            val field = (tag ushr 3).toInt()
            if (field <= 0) break
            val value = when ((tag and 7).toInt()) {
                0 -> { if (varint() == null) break; null }
                1 -> { if (i + 8 > bytes.size) break; i += 8; null }
                2 -> {
                    val len = varint() ?: break
                    if (len > bytes.size - i || len < 0) break
                    val v = bytes.copyOfRange(i, i + len.toInt())
                    i += len.toInt()
                    v
                }
                5 -> { if (i + 4 > bytes.size) break; i += 4; null }
                else -> break
            }
            result.add(field to value)
        }
        return result
    }

    private fun u16(bytes: ByteArray, offset: Int) =
        (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)

}

package de.pulselibre

/**
 * Tolerant, read-only decoder for a Garmin device-status battery response (#142/#159).
 *
 * Notifications from the Garmin ML data channel are COBS-framed and prefixed with a one-byte
 * data handle. Depending on device/firmware the Smart protobuf may arrive inside a compact
 * GFDI frame, a 16-bit GFDI frame, or directly. We try each documented layout in turn and
 * accept only a field that the strict protobuf parser actually recognises — nothing is guessed.
 */
internal object GarminDeviceStatusResponseDecoder {
    /** Decode a concatenated COBS stream (leading/trailing 0 delimiters) into individual frames. */
    fun cobsFrames(stream: ByteArray): List<ByteArray> {
        val frames = ArrayList<ByteArray>()
        var i = 0
        while (i < stream.size) {
            while (i < stream.size && stream[i] == 0.toByte()) i++ // skip delimiters / padding
            if (i >= stream.size) break
            val out = ArrayList<Byte>()
            var code = stream[i].toInt() and 0xff
            i++
            if (code == 0) continue
            var good = true
            while (code != 1) {
                val count = code - 1
                if (i + count > stream.size) { good = false; break }
                for (j in 0 until count) out.add(stream[i + j])
                i += count
                if (i >= stream.size) { good = false; break }
                code = stream[i].toInt() and 0xff
                i++
                if (code == 0) break // zero is the frame delimiter: clean end of this frame
            }
            if (good && out.isNotEmpty()) frames.add(out.toByteArray())
        }
        return frames
    }

    /** Strip an optional leading data handle byte from a notification payload. */
    fun stripHandle(payload: ByteArray): ByteArray =
        if (payload.size > 1 && (payload[0].toInt() and 0xff) in 1..127) payload.copyOfRange(1, payload.size) else payload

    /**
     * Try to decode a battery response from one or more notification payloads. Returns the first
     * response that passes strict protobuf parsing, or null when none of the layouts matched.
     */
    fun decode(payloads: List<ByteArray>): GarminDeviceStatusProtobuf.BatteryResponse? {
        // Each notification may carry its own handle + COBS chunk; reassemble and also try singly.
        val joined = ByteArrayOutputStreamLike()
        payloads.forEach { p -> joined.add(stripHandle(p)) }
        val candidates = ArrayList<ByteArray>()
        candidates.add(joined.toBytes())
        payloads.forEach { candidates.add(stripHandle(it)) }

        for (candidate in candidates) {
            val frames = cobsFrames(candidate)
            val sources = if (frames.isEmpty()) listOf(candidate) else frames + candidate
            for (source in sources) {
                for (protobuf in protobufCandidates(source)) {
                    GarminDeviceStatusProtobuf.parseBatteryResponse(protobuf)?.let { return it }
                }
            }
        }
        return null
    }

    /**
     * Additional values the band offers in ML notifications, even when no battery response is
     * present. Only printable, letter-bearing runs are kept; every retained value gets its own
     * stable key, source and observation time. Nothing is inferred from binary bytes.
     */
    fun extraValues(payloads: List<ByteArray>, nowMs: Long, limit: Int = 8): List<LiveDeviceMetadata> {
        val found = LinkedHashSet<String>()
        for (payload in payloads) {
            val candidate = stripHandle(payload)
            val frames = cobsFrames(candidate)
            for (source in (frames + listOf(candidate))) {
                val current = StringBuilder()
                fun flush() {
                    val text = current.toString().trim()
                    current.setLength(0)
                    if (text.length in 3..256 && text.any { it.isLetter() } &&
                        text.all { it == ' ' || it.isLetterOrDigit() || it in ".-_+" }) found.add(text)
                }
                for (byte in source) {
                    val value = byte.toInt() and 0xff
                    if (value in 32..126) current.append(value.toChar()) else flush()
                }
                flush()
            }
            if (found.size >= limit) break
        }
        return found.take(limit).mapIndexed { index, text ->
            LiveDeviceMetadata("gdi_text_${index + 1}", text, null,
                DeviceMetadataSources.GARMIN_GDI_OTHER, nowMs)
        }
    }

    /** Possible protobuf bodies inside a transport frame, most specific first. */
    private fun protobufCandidates(frame: ByteArray): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        if (frame.size >= 10) {
            val high = frame[3].toInt() and 0xff
            val low = frame[2].toInt() and 0xff
            if (high and 0x80 != 0) {
                // Compact GFDI frame: protobuf starts at byte 6, length = frame - 8 (CRC + header).
                val bodySize = frame.size - 8
                if (bodySize in 1..65535) out.add(frame.copyOfRange(6, frame.size - 2))
            } else {
                // 16-bit GFDI frame with CRC: type in bytes 2..3, body after 4 bytes, minus 2 CRC.
                val bodySize = frame.size - 6
                if (bodySize in 1..65535) out.add(frame.copyOfRange(4, frame.size - 2))
            }
            if (low == 0 && high == 0) out.add(frame.copyOfRange(6, frame.size - 2))
        }
        out.add(frame) // some devices deliver the bare Smart protobuf
        return out
    }

    /** Tiny growable byte buffer (kept dependency-free and JVM-testable). */
    private class ByteArrayOutputStreamLike {
        private val data = ArrayList<Byte>()
        fun add(bytes: ByteArray) { for (b in bytes) data.add(b) }
        fun toBytes(): ByteArray = ByteArray(data.size) { data[it] }
    }
}

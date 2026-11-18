package de.pulselibre

/**
 * Isolated, offline state machine for reading a bounded legacy directory file
 * (index 0) via uncompressed 5004 chunks. Validates GFDI CRC, chunk CRC
 * (cumulative CRC-16/ARC), transfer status and exact offsets; produces a
 * redacted directory report. No BLE I/O; the probe sends 5004 ACKs from state.
 */
class CirqaDirectoryReadV2Session(val token: Any = Any()) {
    enum class Phase { NEW, RECEIVING, COMPLETED, STOPPED }
    enum class Result { ACCEPTED, COMPLETE, DUPLICATE, MALFORMED, CRC_ERROR, OFFSET_ERROR, STOPPED }

    @Volatile var phase = Phase.NEW
        private set
    var reason = "not-started"
        private set
    var expectedSize = 0
        private set
    var receivedBytes = 0
        private set
    var chunkCount = 0
        private set
    var nonDataFrames = 0
        private set
    var notificationsSeen = 0
        private set
    var framesSeen = 0
        private set
    var dataFramesProcessed = 0
        private set
    var controlFrameTypes = ""
        private set
    var protobufDiag = ""
        private set
    var eventLog = ""
        private set
    var firstChunkParse = ""
        private set
    private var startedAtMs = 0L
    private val controlTypes = LinkedHashMap<Int, Int>()
    private val protobufReassembler = GarminProtobufReassembler()
    private val pendingProtobufAcks = ArrayList<Pair<Int, Long>>()
    private val encoded = ArrayList<Byte>()
    private var collecting = false
    var directoryEntries = 0
        private set
    var fitCandidates = 0
        private set
    var redactedEntryReport = ""
        private set

    private val bytes = java.io.ByteArrayOutputStream()
    private var crc = 0            // accumulator after last accepted chunk
    private var prevCrc = 0        // accumulator before last accepted chunk
    private var lastOffset = -1L
    private var lastPayload: ByteArray? = null

    @Synchronized fun begin(size: Int): Boolean {
        if (phase != Phase.NEW || size !in 16..CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES ||
            size % 16 != 0) return stop("invalid-size")
        expectedSize = size
        startedAtMs = System.nanoTime() / 1_000_000L
        phase = Phase.RECEIVING
        reason = "receiving-directory"
        return true
    }

    /** Feed one ML characteristic notification (handle already stripped). */
    @Synchronized fun onNotification(payload: ByteArray): Result {
        if (phase != Phase.RECEIVING) return Result.STOPPED
        notificationsSeen++
        // The device may interleave control/status frames (5000 ACK echoes, 5043
        // protobuf) with the 5004 data stream, and a COBS frame may span two
        // notifications. Decode every complete frame, like the proven legacy
        // CirqaLegacy5004ResponseObserver does; never drop a later 5004 frame just
        // because a control frame came first.
        val frames = extractFrames(payload)
        if (frames.isEmpty()) return Result.ACCEPTED // partial frame; keep accumulating
        var dataSeen = false
        for (frame in frames) {
            if (phase != Phase.RECEIVING) break
            framesSeen++
            val result = processFrame(frame)
            if (result == Result.ACCEPTED || result == Result.DUPLICATE) {
                if (result == Result.ACCEPTED) dataSeen = true
                continue
            }
            return result
        }
        controlFrameTypes = controlTypes.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}x${it.value}" }
        return if (dataSeen) Result.ACCEPTED else Result.DUPLICATE
    }

    private fun processFrame(frame: ByteArray): Result {
        if (GarminFrameValidator.inspectVerified(frame) == null) return fail("gfdi-crc-error", Result.MALFORMED)
        val h = GarminGfdiHeader.inspect(frame) ?: return fail("bad-envelope", Result.MALFORMED)
        // Control/status frames may interleave the data stream. Count and ignore them;
        // only 5004 frames carry directory bytes.
        if (h.type != 5004) {
            nonDataFrames++
            controlTypes[h.type] = (controlTypes[h.type] ?: 0) + 1
            controlFrameTypes = controlTypes.entries.sortedBy { it.key }
                .joinToString(",") { "${it.key}x${it.value}" }
            appendEvent("${h.type}")
            if (h.type == 5043) inspectProtobuf(frame)
            return Result.DUPLICATE
        }
        dataFramesProcessed++
        if (h.compact) return fail("compact-frame", Result.MALFORMED)
        val body = frame.copyOfRange(4, frame.size - 2)
        val chunk = CirqaLegacyTransferDecoder.plainChunk(body)
            ?: return fail("bad-chunk-header", Result.MALFORMED)
        appendEvent("5004[ts=${chunk.transferStatus},crc=${chunk.crc16},off=${chunk.offset},len=${chunk.payload.size}]")
        if (chunk.transferStatus != 0) return fail("transfer-status", Result.MALFORMED)

        // Duplicate chunk (retry): same offset/payload as the last accepted one,
        // with CRC computed from the accumulator before that chunk. Verify without appending.
        val isDuplicate = chunk.offset == lastOffset.toLong() && chunk.payload.contentEquals(lastPayload)
        val verifyBase = if (isDuplicate) prevCrc else crc
        val updated = CirqaLegacyCrc16.compute(chunk.payload, verifyBase)
        if (updated != chunk.crc16) return fail("crc-error", Result.CRC_ERROR)
        if (isDuplicate) return Result.DUPLICATE
        if (chunk.offset != receivedBytes.toLong()) return fail("offset-error", Result.OFFSET_ERROR)
        if (chunk.payload.isEmpty() || receivedBytes + chunk.payload.size > expectedSize) {
            return fail("size-error", Result.MALFORMED)
        }
        if (chunkCount == 0) {
            val parsed = CirqaLegacyDirectoryParser.parse(chunk.payload)
            firstChunkParse = if (parsed == null) "parse-null" else
                "entries=${parsed.entries.size}; idx0=${parsed.entries.firstOrNull()?.fileIndex}" +
                    "; type0=${parsed.entries.firstOrNull()?.type}"
        }
        bytes.write(chunk.payload)
        prevCrc = crc
        crc = updated
        receivedBytes += chunk.payload.size
        chunkCount++
        lastOffset = chunk.offset
        lastPayload = chunk.payload.copyOf()

        return if (receivedBytes == expectedSize) complete() else Result.ACCEPTED
    }

    /** Extract all complete COBS frames; keep a partial frame for the next notification. */
    private fun extractFrames(payload: ByteArray): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    decodeCobs(encoded.toByteArray())?.let { out.add(it) }
                    encoded.clear()
                }
                collecting = true
            } else if (collecting) {
                if (encoded.size >= MAX_ENCODED_BYTES) {
                    fail("rx-budget", Result.MALFORMED)
                    return emptyList()
                }
                encoded += byte
            }
        }
        return out
    }

    private fun decodeCobs(bytes: ByteArray): ByteArray? {
        val output = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val code = bytes[index++].toInt() and 255
            if (code == 0 || index + code - 1 > bytes.size) return null
            repeat(code - 1) { output += bytes[index++] }
            if (code != 0xff && index < bytes.size) output += 0
        }
        return output.toByteArray()
    }

    /** Next ACK offset; null when not receiving. */
    /** RequestId/receivedOffset pairs for transport ACKs to interleaved 5043 chunks. */
    @Synchronized fun drainProtobufAcks(): List<Pair<Int, Long>> {
        val out = pendingProtobufAcks.toList()
        pendingProtobufAcks.clear()
        return out
    }

    @Synchronized fun nextAckOffset(): Int? = if (phase == Phase.RECEIVING) receivedBytes else null

    @Synchronized fun completedBytes(): ByteArray? = if (phase == Phase.COMPLETED) bytes.toByteArray() else null

    @Synchronized fun abort(): Boolean {
        if (phase != Phase.RECEIVING) return false
        return stop("aborted")
    }

    @Synchronized fun summary(): String =
        "DirectoryReadV2: phase=$phase; reason=$reason; size=$expectedSize/$receivedBytes; chunks=$chunkCount; " +
            "notifs=$notificationsSeen; frames=$framesSeen; dataFrames=$dataFramesProcessed; " +
            "control=$controlFrameTypes; events=$eventLog; firstChunk=$firstChunkParse; " +
            "protobuf=$protobufDiag; entries=$directoryEntries; fitCandidates=$fitCandidates; $redactedEntryReport"

    private fun complete(): Result {
        val data = bytes.toByteArray()
        val dir = CirqaLegacyDirectoryParser.parse(data)
            ?: return fail("directory-parse-error", Result.MALFORMED)
        directoryEntries = dir.entries.size
        val supported = setOf(128 to 4, 128 to 32, 128 to 36, 128 to 41, 128 to 44,
            128 to 68, 128 to 70, 128 to 72, 128 to 73, 128 to 74)
        val rows = dir.entries.take(24).map { e ->
            val fit = e.type == 128 && e.readable && (e.type to e.subtype) in supported &&
                e.size in 1..CirqaLegacyDownloadLimits.MAX_FIT_FILE_BYTES.toLong()
            "idx=${e.fileIndex},type=${e.type},sub=${e.subtype},read=${e.readable},size=${e.size},fit=$fit"
        }
        fitCandidates = dir.entries.count { e ->
            e.type == 128 && e.readable && (e.type to e.subtype) in supported &&
                e.size in 1..CirqaLegacyDownloadLimits.MAX_FIT_FILE_BYTES.toLong()
        }
        // Redacted: no timestamps, no identifier bytes, no raw data.
        redactedEntryReport = "entries=[${rows.joinToString("; ")}]" +
            if (dir.entries.size > 24) "; more=${dir.entries.size - 24}" else ""
        phase = Phase.COMPLETED
        reason = "directory-complete-fit-candidates=$fitCandidates"
        return Result.COMPLETE
    }

    /** Read-only: record 5043 transport framing and redacted protobuf field shapes. */
    private fun inspectProtobuf(frame: ByteArray) {
        val body = frame.copyOfRange(4, frame.size - 2)
        if (body.size >= 14) {
            val id = u16(body, 0)
            val offset = u32(body, 2)
            val total = u32(body, 6)
            val chunk = u32(body, 10)
            appendProtobufDiag("5043[id=$id,off=$offset,total=$total,chunk=$chunk];")
            if (pendingProtobufAcks.size < 16) pendingProtobufAcks.add(id to offset)
        }
        protobufReassembler.accept(frame)?.let { msg ->
            appendProtobufDiag("complete[len=${msg.size},shape=${CirqaProtobufShapeInspector.nestedShape(msg)}];")
        }
    }

    private fun appendProtobufDiag(part: String) {
        protobufDiag = (protobufDiag + part).take(600)
    }

    private fun appendEvent(part: String) {
        val ms = System.nanoTime() / 1_000_000L - startedAtMs
        eventLog = (eventLog + "$part@${ms}ms;").take(600)
    }

    private fun u16(bytes: ByteArray, i: Int): Int =
        (bytes[i].toInt() and 255) or ((bytes[i + 1].toInt() and 255) shl 8)

    private fun u32(bytes: ByteArray, i: Int): Long =
        (0..3).fold(0L) { n, k -> n or ((bytes[i + k].toLong() and 255L) shl (8 * k)) }

    private companion object { const val MAX_ENCODED_BYTES = 32 * 1024 }

    private fun fail(reason: String, result: Result): Result {
        this.reason = reason
        phase = Phase.STOPPED
        return result
    }

    private fun stop(reason: String): Boolean {
        this.reason = reason
        phase = Phase.STOPPED
        return false
    }
}

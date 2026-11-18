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
        phase = Phase.RECEIVING
        reason = "receiving-directory"
        return true
    }

    /** Feed one ML characteristic notification (handle already stripped). */
    @Synchronized fun onNotification(payload: ByteArray): Result {
        if (phase != Phase.RECEIVING) return Result.STOPPED
        // Decode one COBS-delimited 5004 frame from the notification.
        val frame = decodeSingleFrame(payload) ?: return fail("bad-cobs", Result.MALFORMED)
        if (GarminFrameValidator.inspectVerified(frame) == null) return fail("gfdi-crc-error", Result.MALFORMED)
        val h = GarminGfdiHeader.inspect(frame) ?: return fail("bad-envelope", Result.MALFORMED)
        if (h.type != 5004 || h.compact) return fail("unexpected-type", Result.MALFORMED)
        val body = frame.copyOfRange(4, frame.size - 2)
        val chunk = CirqaLegacyTransferDecoder.plainChunk(body)
            ?: return fail("bad-chunk-header", Result.MALFORMED)
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
        bytes.write(chunk.payload)
        prevCrc = crc
        crc = updated
        receivedBytes += chunk.payload.size
        chunkCount++
        lastOffset = chunk.offset
        lastPayload = chunk.payload.copyOf()

        return if (receivedBytes == expectedSize) complete() else Result.ACCEPTED
    }

    /** Next ACK offset; null when not receiving. */
    @Synchronized fun nextAckOffset(): Int? = if (phase == Phase.RECEIVING) receivedBytes else null

    @Synchronized fun completedBytes(): ByteArray? = if (phase == Phase.COMPLETED) bytes.toByteArray() else null

    @Synchronized fun abort(): Boolean {
        if (phase != Phase.RECEIVING) return false
        return stop("aborted")
    }

    @Synchronized fun summary(): String =
        "DirectoryReadV2: phase=$phase; reason=$reason; size=$expectedSize/$receivedBytes; chunks=$chunkCount; " +
            "entries=$directoryEntries; fitCandidates=$fitCandidates; $redactedEntryReport"

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

    private fun decodeSingleFrame(payload: ByteArray): ByteArray? {
        // Expect [0, code, ... , 0] COBS framing; strip delimiters.
        if (payload.size < 4 || payload[0] != 0.toByte()) return null
        var start = 1
        var end = -1
        for (i in 1 until payload.size) if (payload[i] == 0.toByte()) { end = i; break }
        if (end <= start) return null
        val encoded = payload.copyOfRange(start, end)
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < encoded.size) {
            val n = encoded[i++].toInt() and 255
            if (n == 0 || i + n - 1 > encoded.size) return null
            repeat(n - 1) { out.write(encoded[i++].toInt()) }
            if (n != 255 && i < encoded.size) out.write(0)
        }
        return out.toByteArray()
    }

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

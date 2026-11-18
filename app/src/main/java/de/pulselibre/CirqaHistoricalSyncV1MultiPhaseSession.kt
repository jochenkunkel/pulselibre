package de.pulselibre

/**
 * Multi-phase sequential state machine for Historical Sync V1:
 * Phase A (Bootstrap -> 5030 -> 5007)
 * Phase B (-> 5031 Supported File Types)
 * Phase C (-> Directory Index 0 Read Request & Header/Size Inspection)
 */
class CirqaHistoricalSyncV1MultiPhaseSession(val token: Any = Any(), val maxPhase: Int = 3) {
    enum class Phase {
        NEW, BOOTSTRAP,
        WAITING_5030_WRITE, WAITING_5030_RESPONSE,
        WAITING_5007_WRITE, WAITING_5007_RESPONSE,
        PHASE_A_COMPLETED,
        WAITING_5031_WRITE, WAITING_5031_RESPONSE,
        PHASE_B_COMPLETED,
        WAITING_DIR_WRITE, WAITING_DIR_RESPONSE,
        PHASE_C_COMPLETED,
        OBSERVED, STOPPED
    }

    @Volatile var phase = Phase.NEW
        private set
    var reason = "not-started"
        private set
    var s5030Outer: Int? = null
        private set
    var s5030ResultByte: Int? = null
        private set
    var s5030Inner: Int? = null
        private set
    var s5007Status: Int? = null
        private set
    var s5031Status: Int? = null
        private set
    var supportedTypesCount = 0
        private set
    var directorySize: Int? = null
        private set

    // Redacted diagnostics: counts and structural metadata only, no payloads.
    var notificationsSeen = 0
        private set
    var framesDecoded = 0
        private set
    var lastFrameType: Int? = null
        private set
    var lastResponseTo: Int? = null
        private set
    var lastOuterStatus: Int? = null
        private set
    var lastPayloadLength = 0
        private set
    var cobsErrors = 0
        private set
    var crcErrors = 0
        private set
    var discardedResponses = 0
        private set
    var phaseAtLastReceive = "none"
        private set
    var ackAtMs: Long? = null
        private set
    var writeAtMs: Long? = null
        private set
    var timeoutAtMs: Long? = null
        private set
    var writeCallbackSeen = false
        private set
    var writeOk: Boolean? = null
        private set
    var protobufMessagesSeen = 0
        private set

    private var lastMillis = 0L
    private var deadline = 0L
    private var writeConfirmed = false
    private var collecting = false
    private val encoded = ArrayList<Byte>()
    private var totalBytes = 0

    @Synchronized fun begin(gatePersisted: Boolean, now: Long): Boolean {
        if (phase != Phase.NEW || !gatePersisted || now < 0 || now > Long.MAX_VALUE - SESSION_MILLIS) {
            return stop("not-authorized-or-invalid-start")
        }
        lastMillis = now
        deadline = now + SESSION_MILLIS
        phase = Phase.BOOTSTRAP
        reason = "bootstrap"
        return true
    }

    @Synchronized fun bootstrapReady(callbackToken: Any, confirmed: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.BOOTSTRAP || !confirmed) return false
        phase = Phase.WAITING_5030_WRITE
        reason = "ready-for-5030"
        ackAtMs = now
        return true
    }

    @Synchronized fun claim5030(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5030_WRITE) return stop("unexpected-5030-claim")
        writeConfirmed = false
        phase = Phase.WAITING_5030_RESPONSE
        reason = "awaiting-5030"
        writeAtMs = now
        writeCallbackSeen = false
        writeOk = null
        return true
    }

    @Synchronized fun claim5007(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5007_WRITE) return stop("unexpected-5007-claim")
        writeConfirmed = false
        phase = Phase.WAITING_5007_RESPONSE
        reason = "awaiting-5007"
        return true
    }

    @Synchronized fun claim5031(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5031_WRITE) return stop("unexpected-5031-claim")
        writeConfirmed = false
        phase = Phase.WAITING_5031_RESPONSE
        reason = "awaiting-5031"
        return true
    }

    @Synchronized fun claimDirectory(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_DIR_WRITE) return stop("unexpected-dir-claim")
        writeConfirmed = false
        phase = Phase.WAITING_DIR_RESPONSE
        reason = "awaiting-directory"
        return true
    }

    @Synchronized fun writeCompleted(callbackToken: Any, type: Int, success: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (!success) return stop("write-failed-$type")
        writeCallbackSeen = true
        writeOk = success
        val expected = when (phase) {
            Phase.WAITING_5030_RESPONSE -> 5030
            Phase.WAITING_5007_RESPONSE -> 5007
            Phase.WAITING_5031_RESPONSE -> 5031
            Phase.WAITING_DIR_RESPONSE -> 5002
            else -> -1
        }
        if (type != expected) return stop("unexpected-write-$type")
        writeConfirmed = true
        return true
    }

    @Synchronized fun receive(callbackToken: Any, payload: ByteArray, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase !in setOf(
                Phase.WAITING_5030_RESPONSE, Phase.WAITING_5007_RESPONSE,
                Phase.WAITING_5031_RESPONSE, Phase.WAITING_DIR_RESPONSE
            )) return stop("unexpected-data")

        notificationsSeen++
        phaseAtLastReceive = phase.name
        if (payload.size > MAX_TOTAL_BYTES - totalBytes) return stop("rx-budget")
        totalBytes += payload.size

        for (b in payload) {
            if (b == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    val frame = decode(encoded.toByteArray()) ?: run { cobsErrors++; return stop("bad-cobs") }
                    framesDecoded++
                    encoded.clear()
                    val h = GarminGfdiHeader.inspect(frame) ?: run { crcErrors++; return stop("bad-envelope") }
                    lastFrameType = h.type
                    lastResponseTo = h.responseTo
                    lastOuterStatus = h.responseStatus
                    lastPayloadLength = maxOf(0, frame.size - 9)
                    if (h.compact || h.type != 5000 || h.responseStatus == null) {
                        // A device-originated protobuf message (5043) after 5007 is protocol
                        // evidence, not an error: record it and keep waiting for the 5000 response.
                        if (h.type == 5043) {
                            protobufMessagesSeen++
                            return true
                        }
                        return stop("bad-frame")
                    }

                    when (phase) {
                        Phase.WAITING_5030_RESPONSE -> {
                            if (h.responseTo != 5030 || h.responseStatus != 0) return stop("5030-rejection")
                            s5030Outer = h.responseStatus
                            val len = frame.size - 9
                            s5030ResultByte = if (len > 0) frame[7].toInt() and 255 else null
                            if (s5030ResultByte != 0) return stop("5030-inner-nonzero")
                            phase = Phase.WAITING_5007_WRITE
                            reason = "5030-ok-ready-5007"
                            return true
                        }
                        Phase.WAITING_5007_RESPONSE -> {
                            if (h.responseTo != 5007 || h.responseStatus != 0) return stop("5007-rejection")
                            s5007Status = h.responseStatus
                            phase = Phase.PHASE_A_COMPLETED
                            reason = "phase-a-completed-stop"
                            return true
                        }
                        Phase.WAITING_5031_RESPONSE -> {
                            if (h.responseTo != 5031 || h.responseStatus != 0) return stop("5031-rejection")
                            s5031Status = h.responseStatus
                            if (frame.size < 10) return stop("5031-malformed")
                            val typesBody = frame.copyOfRange(7, frame.size - 2)
                            val parsedTypes = parseFileTypes(typesBody) ?: return stop("5031-types-parse-error")
                            supportedTypesCount = parsedTypes.size
                            phase = Phase.PHASE_B_COMPLETED
                            reason = "phase-b-completed-stop"
                            return true
                        }
                        Phase.WAITING_DIR_RESPONSE -> {
                            if (h.responseTo != 5002 || h.responseStatus != 0) return stop("dir-rejection")
                            if (frame.size < 14) return stop("dir-malformed")
                            val sizeBytes = frame.copyOfRange(7, 11)
                            val size = (sizeBytes[0].toInt() and 255) or
                                ((sizeBytes[1].toInt() and 255) shl 8) or
                                ((sizeBytes[2].toInt() and 255) shl 16) or
                                ((sizeBytes[3].toInt() and 255) shl 24)
                            directorySize = size
                            phase = Phase.PHASE_C_COMPLETED
                            reason = "phase-c-completed-directory-size-obtained"
                            return true
                        }
                        else -> return stop("unexpected-phase")
                    }
                }
                collecting = true
            } else {
                if (!collecting) return stop("missing-delimiter")
                if (encoded.size >= MAX_ENCODED_BYTES) return stop("frame-budget")
                encoded += b
            }
        }
        return true
    }

    @Synchronized fun resumeToPhaseB(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token || phase != Phase.PHASE_A_COMPLETED || maxPhase < 2) return false
        lastMillis = maxOf(lastMillis, now)
        phase = Phase.WAITING_5031_WRITE
        reason = "resumed-to-phase-b"
        return true
    }

    @Synchronized fun resumeToPhaseC(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token || phase != Phase.PHASE_B_COMPLETED || maxPhase < 3) return false
        lastMillis = maxOf(lastMillis, now)
        phase = Phase.WAITING_DIR_WRITE
        reason = "resumed-to-phase-c"
        return true
    }

    @Synchronized fun tick(callbackToken: Any, now: Long): Boolean = current(callbackToken, now)
    @Synchronized fun abort(callbackToken: Any): Boolean = if (callbackToken !== token) false else stop("aborted")

    @Synchronized fun summary(): String =
        "HistoricalSyncV1MultiPhase: phase=$phase; reason=$reason; 5030(outer=$s5030Outer,res=$s5030ResultByte); " +
            "5007=$s5007Status; 5031(status=$s5031Status,types=$supportedTypesCount); dirSize=$directorySize; " +
            "diag(notifs=$notificationsSeen,frames=$framesDecoded,lastType=$lastFrameType,lastTo=$lastResponseTo," +
            "outer=$lastOuterStatus,payloadLen=$lastPayloadLength,cobsErr=$cobsErrors,crcErr=$crcErrors," +
            "discarded=$discardedResponses,phaseAtRx=$phaseAtLastReceive," +
            "relAck=${ackAtMs?.let { it - (writeAtMs ?: it) }},relWrite=${writeAtMs?.let { it - (ackAtMs ?: it) }}," +
            "protobuf=$protobufMessagesSeen,writeCb=$writeCallbackSeen,writeOk=$writeOk,timeout=$timeoutAtMs)."

    private fun current(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token || phase == Phase.STOPPED ||
            phase in setOf(Phase.PHASE_A_COMPLETED, Phase.PHASE_B_COMPLETED, Phase.PHASE_C_COMPLETED)) {
            return false
        }
        if (phase == Phase.NEW) return stop("not-started")
        if (now < lastMillis) return stop("clock-regressed")
        lastMillis = now
        if (now >= deadline) return stop("timeout")
        return true
    }

    private fun stop(value: String): Boolean {
        if (value == "timeout") timeoutAtMs = deadline
        if (phase !in setOf(Phase.STOPPED, Phase.PHASE_A_COMPLETED, Phase.PHASE_B_COMPLETED, Phase.PHASE_C_COMPLETED)) {
            reason = value
            phase = Phase.STOPPED
            encoded.clear()
        }
        return false
    }

    private fun parseFileTypes(body: ByteArray): List<Pair<Int, Int>>? {
        if (body.isEmpty()) return null
        val count = body[0].toInt() and 255
        var offset = 1
        val result = ArrayList<Pair<Int, Int>>()
        repeat(count) {
            if (offset + 2 > body.size) return null
            val type = body[offset++].toInt() and 255
            val subtype = body[offset++].toInt() and 255
            result.add(type to subtype)
        }
        return result
    }

    private fun decode(bytes: ByteArray): ByteArray? {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < bytes.size) {
            val n = bytes[i++].toInt() and 255
            if (n == 0 || i + n - 1 > bytes.size) return null
            repeat(n - 1) { out += bytes[i++] }
            if (n != 255 && i < bytes.size) out += 0
        }
        return out.toByteArray()
    }

    companion object {
        const val SESSION_MILLIS = 45_000L
        const val MAX_ENCODED_BYTES = 256
        const val MAX_TOTAL_BYTES = 2048
    }
}

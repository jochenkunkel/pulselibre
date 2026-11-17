package de.pulselibre

/**
 * Experimental read-only FileSync V2 directory query, derived from public protocol facts
 * documented by MIT-licensed GarminHRM600. Never sends FileRequest or FileSetFlags.
 * A response is only reported after validated COBS + GFDI length + CRC.
 */
class GarminFileListProbe {
    val bootstrap = GarminBootstrapObserver()
    val bootstrapDiagnosis = GarminBootstrapDiagnosis()
    val postBootstrap = GarminPostBootstrapObserver()
    val handshake = CirqaBootstrapStateMachine()
    val structure5050 = Cirqa5050StructureObserver()
    private val handshakeStartNanos = System.nanoTime()
    var ackAttempted = false
        set(value) {
            if (value && !field) handshake.onAckAttempt()
            field = value
        }
    var captureRawFrames = false
    private val rawFrames = mutableListOf<String>()
    private val rawStartNanos = System.nanoTime()
    private var rawBytes = 0
    private var droppedRawFrames = 0

    fun rawCaptureReport(): String =
        "GFDI-Rohdaten (CRC-geprüft; vollständige Frames, Hex; nur auf Nutzerfreigabe teilen): " +
            "Frames=${rawFrames.size}; Bytes=$rawBytes; wegen Limit verworfen=$droppedRawFrames\\n" +
            rawFrames.joinToString("\\n")
    val chunks = GarminChunkObserver()
    val protobuf = GarminProtobufReassembler()
    val compactSession = CirqaCompactSessionObserver()
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
    val responseStatuses = linkedMapOf<String, Int>()
    var responseErrors = 0
        private set
    var otherMessages = 0
        private set
    var validSynchronizationShapes = 0
        private set
    var malformedSynchronizationShapes = 0
        private set
    val synchronizationMaskBitCounts = linkedMapOf<Int, Int>()


    fun summary(): String = "GFDI-Frames: $frames; FileListResponse: $validLists; " +
        "Dateieinträge: $fileEntries; Typen: $frameTypes; 5037-Strukturen gültig=$validSynchronizationShapes/ungültig=$malformedSynchronizationShapes; 5037-Maskenbitanzahl=$synchronizationMaskBitCounts; GFDI-5000-Status: $responseStatuses; Antwortfehler: $responseErrors; andere Nachrichten: $otherMessages; ungültige Frames: $rejectedFrames; ${bootstrap.summary()}; ${bootstrapDiagnosis.summary()}; ${postBootstrap.summary()}; ${handshake.summary()}; ${structure5050.summary()}; ${chunks.summary()}; ${protobuf.summary()}; ${compactSession.summary()}"

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
        compactSession.observe(frame)
        if (captureRawFrames) {
            if (rawFrames.size < 32 && rawBytes + frame.size <= 8192) {
                rawFrames.add("RX +${(System.nanoTime() - rawStartNanos) / 1_000_000}ms Frame #$frames " +
                    "${GarminGfdiHeader.describe(frame)} " +
                    "CRC=OK: " + frame.joinToString("") { "%02x".format(it.toInt() and 255) })
                rawBytes += frame.size
            } else droppedRawFrames++
        }
        handshake.observe(frame, (System.nanoTime() - handshakeStartNanos) / 1_000_000)
        structure5050.observe(frame)
        bootstrap.observe(frame)
        bootstrapDiagnosis.observe(frame)
        postBootstrap.observe(frame, ackAttempted)
        chunks.observe(frame)
        protobuf.accept(frame)?.let { parseSmart(it) }
        val type = u16(frame, 2)
        val compact = type and 0x8000 != 0
        val effectiveType = if (compact) 5000 + (type and 255) else type
        val label = "gfdi-$effectiveType"
        frameTypes[label] = (frameTypes[label] ?: 0) + 1
        // Passive inspection only. GFDI envelope CRC was checked above.
        // Never acknowledge 5037 or initiate FileSync from this observer.
        if (effectiveType == 5037) {
            val shape = Cirqa5037OfflineSynchronization.inspect(
                frame.copyOfRange(4, frame.size - 2)
            )
            if (shape == null) {
                malformedSynchronizationShapes++
            } else {
                validSynchronizationShapes++
                val bitCount = shape.setFileTypeBits.size
                synchronizationMaskBitCounts[bitCount] =
                    (synchronizationMaskBitCounts[bitCount] ?: 0) + 1
            }
        }
        if (effectiveType == 5000 && frame.size >= 9) {
            val original = u16(frame, 4)
            val status = frame[6].toInt() and 255
            val name = when (status) {
                0 -> "ACK"
                1 -> "NAK"
                2 -> "UNSUPPORTED"
                3 -> "COBS_ERROR"
                4 -> "CRC_ERROR"
                5 -> "LENGTH_ERROR"
                else -> "UNKNOWN_$status"
            }
            val sequence = if (compact) (type ushr 8) and 0x7f else -1
            val key = "request=$original/status=$name/seq=$sequence"
            responseStatuses[key] = (responseStatuses[key] ?: 0) + 1
            if (status != 0) responseErrors++
        }
        if (!compact || type and 255 !in listOf(0x2b, 0x2c, 0x39, 0x3a)) {
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
        val outer = strictFields(bytes) ?: return
        for ((field, value) in outer) {
            if (field != 43 || value == null) continue
            val smart = strictFields(value) ?: continue
            for ((sub, data) in smart) {
                if (sub != 10 || data == null) continue
                val entries = strictFields(data) ?: continue
                validLists++
                bootstrap.observeFileListResponse()
                fileEntries += entries.count { it.first == 4 && it.second != null }
            }
        }
    }

    /**
     * Rejects an entire malformed protobuf rather than accepting a valid-looking
     * prefix as a FileListResponse. Bounds protect diagnostics from untrusted BLE.
     */
    private fun strictFields(bytes: ByteArray): List<Pair<Int, ByteArray?>>? {
        val result = ArrayList<Pair<Int, ByteArray?>>()
        var i = 0
        fun varint(): Long? {
            var n = 0L
            for (shift in 0..63 step 7) {
                if (i >= bytes.size) return null
                val b = bytes[i++].toInt() and 255
                if (shift == 63 && b > 1) return null
                n = n or ((b and 127).toLong() shl shift)
                if (b and 128 == 0) return n
            }
            return null
        }
        while (i < bytes.size) {
            if (result.size >= 256) return null
            val tag = varint() ?: return null
            val field = tag ushr 3
            if (field == 0L || field > Int.MAX_VALUE) return null
            val value = when ((tag and 7).toInt()) {
                0 -> { if (varint() == null) return null; null }
                1 -> { if (bytes.size - i < 8) return null; i += 8; null }
                2 -> {
                    val len = varint() ?: return null
                    if (len < 0 || len > bytes.size - i) return null
                    val v = bytes.copyOfRange(i, i + len.toInt())
                    i += len.toInt()
                    v
                }
                5 -> { if (bytes.size - i < 4) return null; i += 4; null }
                else -> return null
            }
            result.add(field.toInt() to value)
        }
        return result
    }

    private fun u16(bytes: ByteArray, offset: Int) =
        (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)

}

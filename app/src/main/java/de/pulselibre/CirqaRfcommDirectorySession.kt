package de.pulselibre

/**
 * Offline-only directory session for a continuous Classic-RFCOMM GFDI stream.
 *
 * Reuses the proven per-chunk validation in [CirqaDirectoryReadV2Session] and
 * the Connect-shaped ACK builders. Returns GFDI ACK frames to the caller; it
 * never opens a socket or writes bytes itself. Prepared for a separately
 * released/gated device test only.
 */
class CirqaRfcommDirectorySession {
    private val framer = CirqaRfcommStreamFramer()
    private var directory: CirqaDirectoryReadV2Session? = null
    var controlFrames = 0
        private set
    var ackFramesBuilt = 0
        private set
    var sizeError = ""
        private set

    fun begin(size: Int): Boolean {
        val session = CirqaDirectoryReadV2Session()
        if (!session.begin(size)) return false
        directory = session
        return true
    }

    val phase get() = directory?.phase
    val receivedBytes get() = directory?.receivedBytes ?: 0
    val expectedSize get() = directory?.expectedSize ?: 0
    val chunkCount get() = directory?.chunkCount ?: 0
    val completedBytes get() = directory?.completedBytes()
    fun summary(): String = directory?.summary() ?: "CirqaRfcomm: not-started"

    /**
     * Feed an arbitrary stream chunk. Returns GFDI ACK frames that the caller
     * must COBS-wrap and write to the socket, in order.
     */
    fun acceptStream(bytes: ByteArray): List<ByteArray> {
        val acks = ArrayList<ByteArray>()
        for (frame in framer.accept(bytes)) {
            if (GarminFrameValidator.inspectVerified(frame) == null) continue
            val h = GarminGfdiHeader.inspect(frame) ?: continue
            when (h.type) {
                5000 -> {
                    controlFrames++
                    // The 5002 response carries the bounded file size; start the
                    // strict directory session from it instead of trusting a guess.
                    if (h.responseTo == 5002 && h.responseStatus == 0 && phase == null) {
                        val payload = frame.copyOfRange(7, frame.size - 2)
                        val size = CirqaLegacy5002Codec.response(payload)?.let { parsed ->
                            if (parsed.status == CirqaLegacyTransferDecoder.ReadStatus.SUCCESS)
                                parsed.unsignedValue?.takeIf { it in 16..CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES.toLong() }?.toInt()
                            else null
                        }
                        if (size == null) sizeError = "invalid-5002-size" else begin(size)
                    }
                }
                5004 -> {
                    val d = directory ?: continue
                    val result = d.onNotification(GarminFileListRequest.cobs(frame))
                    if (result == CirqaDirectoryReadV2Session.Result.ACCEPTED ||
                        result == CirqaDirectoryReadV2Session.Result.DUPLICATE ||
                        result == CirqaDirectoryReadV2Session.Result.COMPLETE) {
                        // nextAckOffset() is null once COMPLETED; the final ACK still
                        // reports the total number of accepted bytes.
                        val offset = d.nextAckOffset() ?: d.receivedBytes
                        acks.add(CirqaLegacy5004AckCandidate.gfdiFrame(offset))
                        ackFramesBuilt++
                    }
                }
                5043 -> {
                    controlFrames++
                    val ack = protobufAck(frame) ?: continue
                    acks.add(ack)
                    ackFramesBuilt++
                }
                else -> controlFrames++
            }
        }
        return acks
    }

    private fun protobufAck(frame: ByteArray): ByteArray? {
        val body = frame.copyOfRange(4, frame.size - 2)
        if (body.size < 14) return null
        fun u16(i: Int) = (body[i].toInt() and 255) or ((body[i + 1].toInt() and 255) shl 8)
        fun u32(i: Int) = (0..3).fold(0L) { n, k -> n or ((body[i + k].toLong() and 255L) shl (8 * k)) }
        return CirqaLegacy5043AckCandidate.gfdiFrame(u16(0), u32(2))
    }
}

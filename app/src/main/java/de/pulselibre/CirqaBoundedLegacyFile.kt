package de.pulselibre

import java.io.ByteArrayOutputStream

/**
 * Offline, strict single-file leg after a separately authorized directory claim.
 * Request/response parsing accepts decoded frames only. No BLE, ACK sender, DB
 * or automatic retries. Uses cumulative Connect CRC only after offset acceptance.
 * A future caller must enforce the shared four-file/16-MiB session budget.
 */
class CirqaBoundedLegacyFile(
    private val index: Int,
    private val directorySize: Long? = null,
    val token: Any = Any(),
    private val sessionDeadlineMillis: Long? = null
) {
    enum class Phase { NEW, AWAITING_RESPONSE, RECEIVING, VERIFIED, FAILED }
    var phase = Phase.NEW
        private set
    var failure: String? = null
        private set
    private var bytes = ByteArrayOutputStream()
    private var expectedSize: Int? = null
    private var crc = 0
    private var writeConfirmed = false
    private var lastNow = 0L
    private var deadline = 0L
    private val limit = if (index == 0) CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES
        else CirqaLegacyDownloadLimits.MAX_FIT_FILE_BYTES

    init {
        require(index in 0..65535)
        require(if (index == 0) directorySize == null else directorySize in 1..limit.toLong())
    }

    fun claim(now: Long): ByteArray? {
        if (phase != Phase.NEW || now < 0 || now > Long.MAX_VALUE - MAX_MILLIS) {
            fail("invalid-or-duplicate-claim")
            return null
        }
        lastNow = now
        deadline = now + MAX_MILLIS
        if (sessionDeadlineMillis != null) {
            if (sessionDeadlineMillis <= now) {
                fail("expired-parent-session")
                return null
            }
            deadline = minOf(deadline, sessionDeadlineMillis)
        }
        phase = Phase.AWAITING_RESPONSE
        return CirqaLegacy5002RequestCandidate.gfdiFrame(index)
    }

    fun writeCompleted(callbackToken: Any, success: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.AWAITING_RESPONSE || writeConfirmed || !success) return fail("write-error")
        writeConfirmed = true
        if (expectedSize != null) phase = Phase.RECEIVING
        return true
    }

    fun response5002(callbackToken: Any, frame: ByteArray, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.AWAITING_RESPONSE || expectedSize != null) return fail("duplicate-read-response")
        if (frame.size != 14) return fail("bad-read-response-size")
        val h = GarminGfdiHeader.inspect(frame) ?: return fail("bad-envelope")
        if (h.compact || h.type != 5000 || h.responseTo != 5002 || h.responseStatus != 0 ||
            frame.size != 14) return fail("bad-read-response")
        val size = CirqaLegacy5002Codec.boundedSuccessSize(frame.copyOfRange(7, 12), limit)
            ?: return fail("bad-size-or-read-status")
        if (index != 0 && size.toLong() != directorySize) return fail("directory-size-mismatch")
        expectedSize = size
        if (writeConfirmed) phase = Phase.RECEIVING
        return true
    }

    /** Returns validated next offset only if an ACK would be eligible; never sends one. */
    fun chunk5004(callbackToken: Any, frame: ByteArray, now: Long): Int? {
        if (!current(callbackToken, now)) return null
        if (phase != Phase.RECEIVING || frame.size > MAX_FRAME_BYTES) {
            fail("unexpected-or-oversize-chunk")
            return null
        }
        val h = GarminGfdiHeader.inspect(frame)
        if (h == null || h.compact || h.type != 5004) {
            fail("bad-chunk-envelope")
            return null
        }
        val chunk = CirqaLegacyTransferDecoder.plainChunk(frame.copyOfRange(4, frame.size - 2))
        val remaining = expectedSize!! - bytes.size()
        if (chunk == null || chunk.transferStatus != 0 || chunk.offset != bytes.size().toLong() ||
            chunk.payload.isEmpty() || chunk.payload.size > remaining) {
            fail("bad-chunk-offset-status-or-size")
            return null
        }
        val updated = CirqaLegacyCrc16.compute(chunk.payload, crc)
        if (updated != chunk.crc16) {
            fail("cumulative-crc-error")
            return null
        }
        crc = updated
        bytes.write(chunk.payload)
        if (bytes.size() == expectedSize) {
            val complete = bytes.toByteArray()
            val valid = if (index == 0) CirqaLegacyDirectoryParser.parseForDownload(complete) != null
                else FitFileHeader.inspectVerified(complete) != null
            if (!valid) {
                fail("invalid-complete-file")
                return null
            }
            phase = Phase.VERIFIED
        }
        return bytes.size()
    }

    fun completedBytes(): ByteArray? =
        if (phase == Phase.VERIFIED) bytes.toByteArray() else null

    fun disconnect(callbackToken: Any): Boolean =
        if (callbackToken !== token) false else fail("disconnected")

    fun tick(callbackToken: Any, now: Long): Boolean = current(callbackToken, now)

    private fun current(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token || phase == Phase.FAILED || phase == Phase.VERIFIED) return false
        if (phase == Phase.NEW || now < lastNow || now >= deadline) return fail("clock-or-timeout")
        lastNow = now
        return true
    }

    private fun fail(reason: String): Boolean {
        if (phase != Phase.FAILED && phase != Phase.VERIFIED) {
            failure = reason
            phase = Phase.FAILED
            bytes = ByteArrayOutputStream()
        }
        return false
    }

    companion object {
        const val MAX_MILLIS = CirqaLegacyDownloadLimits.MAX_SESSION_MILLIS
        const val MAX_FRAME_BYTES = 32 * 1024
    }
}

package de.pulselibre

import java.io.ByteArrayOutputStream

/**
 * Offline-only assembler for uncompressed GFDI 5004 payload bodies.
 * CRC verification is mandatory and supplied by a separately verified implementation.
 * No BLE, ACK sending, compression, disk writes or device mutation.
 */
class CirqaLegacyPlainFileAssembler(
    private val expectedSize: Int,
    private val verifyPayloadCrc: (ByteArray, Int) -> Boolean,
    private val maxFileBytes: Int = 16 * 1024 * 1024
) {
    enum class Result { ACCEPTED, DUPLICATE, COMPLETE, MALFORMED, CRC_ERROR, OFFSET_ERROR, ABORTED }
    private val bytes = ByteArrayOutputStream()
    private var lastOffset: Long? = null
    private var lastLength: Int = 0
    private var lastPayload: ByteArray? = null
    private var consecutiveFailures = 0
    private var aborted = false

    init {
        require(maxFileBytes > 0)
        require(expectedSize in 0..maxFileBytes)
    }

    val receivedBytes: Int get() = bytes.size()
    val isComplete: Boolean get() = !aborted && receivedBytes == expectedSize
    val isAborted: Boolean get() = aborted

    /** Returns a defensive copy only after all bytes have been CRC-verified. */
    fun completedBytes(): ByteArray? = if (isComplete) bytes.toByteArray() else null

    fun accept(body: ByteArray): Result {
        if (aborted) return Result.ABORTED
        if (isComplete) return Result.COMPLETE
        val chunk = CirqaLegacyTransferDecoder.plainChunk(body)
            ?: return fail(Result.MALFORMED)
        if (!verifyPayloadCrc(chunk.payload, chunk.crc16)) return fail(Result.CRC_ERROR)
        val current = receivedBytes.toLong()
        if (chunk.offset == lastOffset &&
            chunk.payload.size == lastLength &&
            lastPayload?.contentEquals(chunk.payload) == true) {
            consecutiveFailures = 0
            return Result.DUPLICATE
        }
        if (chunk.offset != current) return fail(Result.OFFSET_ERROR)
        if (chunk.payload.isEmpty() || chunk.payload.size.toLong() > expectedSize.toLong() - current) {
            return fail(Result.MALFORMED)
        }
        bytes.write(chunk.payload)
        lastOffset = chunk.offset
        lastLength = chunk.payload.size
        lastPayload = chunk.payload.copyOf()
        consecutiveFailures = 0
        return if (isComplete) Result.COMPLETE else Result.ACCEPTED
    }

    private fun fail(result: Result): Result {
        consecutiveFailures++
        if (consecutiveFailures >= 3) {
            aborted = true
            return Result.ABORTED
        }
        return result
    }
}

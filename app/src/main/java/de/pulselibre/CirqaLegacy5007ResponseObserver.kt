package de.pulselibre

/**
 * Bounded COBS/GFDI response inspection for the single authorized 5007 test.
 * It retains no frame or payload. Any valid but unexpected application frame
 * stops the test; only a CRC-valid GFDI 5000 response-to-5007 is classified.
 */
class CirqaLegacy5007ResponseObserver {
    enum class Result { PENDING, SUPPORTED, NOT_SUPPORTED, UNEXPECTED, MALFORMED }

    private val encoded = ArrayList<Byte>()
    private var collecting = false
    private var completedFrames = 0
    var responseType: Int? = null
        private set
    var responseStatus: Int? = null
        private set

    fun observeMlPayload(payload: ByteArray): Result {
        var result = Result.PENDING
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    if (completedFrames > 0) return Result.UNEXPECTED
                    val frameResult = inspectEncoded(encoded.toByteArray())
                    completedFrames++
                    encoded.clear()
                    if (frameResult == Result.MALFORMED || frameResult == Result.UNEXPECTED) {
                        return frameResult
                    }
                    if (frameResult != Result.PENDING) result = frameResult
                }
                encoded.clear()
                collecting = true
            } else if (collecting) {
                if (encoded.size >= MAX_ENCODED_BYTES) {
                    encoded.clear()
                    collecting = false
                    return Result.MALFORMED
                }
                encoded += byte
            }
        }
        return result
    }

    private fun inspectEncoded(bytes: ByteArray): Result {
        val frame = decodeCobs(bytes) ?: return Result.MALFORMED
        if (GarminFrameValidator.inspectVerified(frame) == null) return Result.MALFORMED
        val header = GarminGfdiHeader.inspect(frame) ?: return Result.MALFORMED
        responseType = header.type
        if (header.type != 5000 || header.responseTo != CirqaLegacy5007RequestCandidate.MESSAGE_TYPE ||
            header.responseStatus == null) return Result.UNEXPECTED
        responseStatus = header.responseStatus
        return if (header.responseStatus == 0) Result.SUPPORTED else Result.NOT_SUPPORTED
    }

    private fun decodeCobs(bytes: ByteArray): ByteArray? {
        val decoded = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val code = bytes[index++].toInt() and 0xff
            if (code == 0 || index + code - 1 > bytes.size) return null
            repeat(code - 1) { decoded += bytes[index++] }
            if (code != 0xff && index < bytes.size) decoded += 0
        }
        return decoded.toByteArray()
    }

    companion object {
        private const val MAX_ENCODED_BYTES = 8192
    }
}

package de.pulselibre

/** Retains only response status and parsed file-type metadata, never raw frames. */
class CirqaLegacy5031ResponseObserver {
    enum class Result { PENDING, SUPPORTED, NOT_SUPPORTED, UNEXPECTED, MALFORMED }

    private val encoded = ArrayList<Byte>()
    private var collecting = false
    private var completedFrames = 0
    var responseStatus: Int? = null
        private set
    var fileTypes: List<CirqaLegacyFileTypesParser.FileType>? = null
        private set

    fun observeMlPayload(payload: ByteArray): Result {
        var result = Result.PENDING
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    if (completedFrames > 0) return Result.UNEXPECTED
                    val inspected = inspect(decodeCobs(encoded.toByteArray()) ?: return Result.MALFORMED)
                    completedFrames++
                    encoded.clear()
                    if (inspected != Result.PENDING) result = inspected
                }
                encoded.clear()
                collecting = true
            } else if (collecting) {
                if (encoded.size >= MAX_ENCODED_BYTES) return Result.MALFORMED
                encoded += byte
            }
        }
        return result
    }

    private fun inspect(frame: ByteArray): Result {
        val header = GarminGfdiHeader.inspect(frame) ?: return Result.MALFORMED
        if (header.type != 5000 || header.responseTo != CirqaLegacy5031RequestCandidate.MESSAGE_TYPE ||
            header.responseStatus == null) return Result.UNEXPECTED
        responseStatus = header.responseStatus
        if (header.responseStatus != 0) return Result.NOT_SUPPORTED
        // GFDI response body: original type (2), status (1), then Connect's
        // SupportedFileTypes payload (count and entries), followed by CRC (2).
        if (frame.size < 10) return Result.MALFORMED
        val body = frame.copyOfRange(7, frame.size - 2)
        fileTypes = CirqaLegacyFileTypesParser.parse(body) ?: return Result.MALFORMED
        return Result.SUPPORTED
    }

    private fun decodeCobs(bytes: ByteArray): ByteArray? {
        val output = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val code = bytes[index++].toInt() and 0xff
            if (code == 0 || index + code - 1 > bytes.size) return null
            repeat(code - 1) { output += bytes[index++] }
            if (code != 0xff && index < bytes.size) output += 0
        }
        return output.toByteArray()
    }

    companion object { private const val MAX_ENCODED_BYTES = 8192 }
}

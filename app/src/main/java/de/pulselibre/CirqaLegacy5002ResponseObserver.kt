package de.pulselibre

/** Strict one-frame observer for a GFDI 5000 response to one 5002 request. */
class CirqaLegacy5002ResponseObserver {
    enum class Result { PENDING, SUCCESS, STATUS_ERROR, UNEXPECTED, MALFORMED }

    private val encoded = ArrayList<Byte>()
    private var collecting = false
    private var completedFrames = 0
    var response: CirqaLegacy5002Codec.Response? = null
        private set
    var gfdiStatus: Int? = null
        private set

    fun observeMlPayload(payload: ByteArray): Result {
        var result = Result.PENDING
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    if (completedFrames > 0) return Result.UNEXPECTED
                    val frame = decodeCobs(encoded.toByteArray()) ?: return Result.MALFORMED
                    val inspected = inspect(frame)
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
        if (header.type != 5000 || header.responseTo != CirqaLegacy5002RequestCandidate.MESSAGE_TYPE ||
            header.responseStatus == null) return Result.UNEXPECTED
        gfdiStatus = header.responseStatus
        if (header.responseStatus != 0) return Result.STATUS_ERROR
        if (frame.size < 10) return Result.MALFORMED
        val parsed = CirqaLegacy5002Codec.response(frame.copyOfRange(7, frame.size - 2))
            ?: return Result.MALFORMED
        response = parsed
        return if (parsed.status == CirqaLegacyTransferDecoder.ReadStatus.SUCCESS &&
            parsed.unsignedValue != null) Result.SUCCESS else Result.STATUS_ERROR
    }

    private fun decodeCobs(bytes: ByteArray): ByteArray? {
        val result = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val code = bytes[index++].toInt() and 0xff
            if (code == 0 || index + code - 1 > bytes.size) return null
            repeat(code - 1) { result += bytes[index++] }
            if (code != 0xff && index < bytes.size) result += 0
        }
        return result.toByteArray()
    }

    companion object { private const val MAX_ENCODED_BYTES = 32768 }
}

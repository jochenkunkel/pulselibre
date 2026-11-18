package de.pulselibre

/** Bounded stream decoder for exactly one complete uncompressed 5004 GFDI frame. */
class CirqaLegacy5004ResponseObserver {
    enum class Result { PENDING, DATA, UNEXPECTED, MALFORMED }

    private val encoded = ArrayList<Byte>()
    private var collecting = false
    private var completedFrames = 0
    var body: ByteArray? = null
        private set

    fun observeMlPayload(payload: ByteArray): Result {
        var result = Result.PENDING
        for (byte in payload) {
            if (byte == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    if (completedFrames > 0) return Result.UNEXPECTED
                    val frame = decodeCobs(encoded.toByteArray()) ?: return Result.MALFORMED
                    val verified = GarminFrameValidator.inspectVerified(frame)
                        ?: return Result.MALFORMED
                    val header = GarminGfdiHeader.inspect(frame)
                        ?: return Result.MALFORMED
                    if (verified.messageType != 5004 || header.type != 5004)
                        return Result.UNEXPECTED
                    body = frame.copyOfRange(4, frame.size - 2)
                    completedFrames++
                    encoded.clear()
                    result = Result.DATA
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

    companion object { private const val MAX_ENCODED_BYTES = 32 * 1024 }
}

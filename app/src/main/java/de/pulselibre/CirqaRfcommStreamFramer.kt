package de.pulselibre

/**
 * Offline-only incremental COBS framer for a continuous GFDI byte stream such
 * as Classic RFCOMM. BLE notifications carry one delimited frame each, but a
 * socket delivers an arbitrary byte stream; frames may be split or coalesced.
 *
 * No transport, no Bluetooth, no device writes.
 */
class CirqaRfcommStreamFramer(private val maxFrameBytes: Int = CirqaRfcommSpec.MAX_FRAME_BYTES) {
    init { require(maxFrameBytes in 256..1_048_576) }

    private val encoded = ArrayList<Byte>()
    var rejected = 0
        private set
    var framesDecoded = 0
        private set

    /** Feed an arbitrary socket chunk; return every complete decoded COBS frame. */
    fun accept(bytes: ByteArray): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        for (byte in bytes) {
            if (byte == 0.toByte()) {
                if (encoded.isNotEmpty()) {
                    val decoded = decodeCobs(encoded.toByteArray())
                    if (decoded == null) rejected++ else { out.add(decoded); framesDecoded++ }
                    encoded.clear()
                }
            } else {
                if (encoded.size >= maxFrameBytes) { rejected++; encoded.clear() }
                else encoded.add(byte)
            }
        }
        return out
    }

    fun reset() { encoded.clear() }

    private fun decodeCobs(bytes: ByteArray): ByteArray? {
        val output = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val code = bytes[index++].toInt() and 255
            if (code == 0 || index + code - 1 > bytes.size) return null
            repeat(code - 1) { output += bytes[index++] }
            if (code != 0xff && index < bytes.size) output += 0
        }
        return if (output.isEmpty()) null else output.toByteArray()
    }
}

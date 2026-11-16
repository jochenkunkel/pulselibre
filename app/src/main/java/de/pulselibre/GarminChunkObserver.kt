package de.pulselibre

/**
 * Passive compact protobuf chunk metadata decoder. Does not ACK or assemble data.
 * Only verified GFDI envelopes are accepted; no health payload is retained.
 */
class GarminChunkObserver {
    var observedChunks = 0
        private set
    var invalidChunks = 0
        private set
    val kinds = linkedMapOf<Int, Int>()

    fun observe(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 8) return
        val type = u16(frame, 2)
        if (type and 0x8000 == 0) return
        val kind = type and 255
        if (kind != 0x2b && kind != 0x2c) return
        val bodySize = frame.size - 6
        if (bodySize < 2 || (kind == 0x2b && bodySize < 14)) {
            invalidChunks++
            return
        }
        observedChunks++
        kinds[kind] = (kinds[kind] ?: 0) + 1
    }

    fun summary(): String =
        "Protobuf-Teilpakete: $observedChunks; unvollständig: $invalidChunks; Arten: $kinds"

    private fun u16(bytes: ByteArray, index: Int) =
        (bytes[index].toInt() and 255) or ((bytes[index + 1].toInt() and 255) shl 8)
}

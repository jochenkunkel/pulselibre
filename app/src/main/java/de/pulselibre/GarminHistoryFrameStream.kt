package de.pulselibre

/**
 * Bounded, offline GFDI stream deframer. Accepts bytes only after the caller
 * has separately established an authorized Garmin transport and removed any
 * transport-specific envelope (e.g. COBS). No BLE commands are sent here.
 *
 * Invalid length or CRC discards buffered bytes instead of attempting unsafe
 * byte-by-byte resynchronization; the caller must restart the framing session.
 */
class GarminHistoryFrameStream(private val maxFrameBytes: Int = 65535) {
    init { require(maxFrameBytes in 6..65535) }
    private var pending = ByteArray(0)
    var failed = false
        private set

    fun reset() {
        pending = ByteArray(0)
        failed = false
    }

    fun accept(chunk: ByteArray): List<ByteArray> {
        if (failed || chunk.isEmpty()) return emptyList()
        if (chunk.size > maxFrameBytes || pending.size > maxFrameBytes - chunk.size) {
            failed = true
            pending = ByteArray(0)
            return emptyList()
        }
        pending += chunk
        val frames = ArrayList<ByteArray>()
        while (pending.size >= 2) {
            val length = (pending[0].toInt() and 255) or
                ((pending[1].toInt() and 255) shl 8)
            if (length !in 6..maxFrameBytes) {
                failed = true
                pending = ByteArray(0)
                return emptyList()
            }
            if (pending.size < length) break
            val frame = pending.copyOfRange(0, length)
            if (GarminFrameValidator.inspectVerified(frame) == null) {
                failed = true
                pending = ByteArray(0)
                return emptyList()
            }
            frames.add(frame)
            pending = pending.copyOfRange(length, pending.size)
        }
        return frames
    }
}

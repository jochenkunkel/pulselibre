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
        val frames = ArrayList<ByteArray>()
        var offset = 0
        while (true) {
            if (pending.size < 2) {
                if (offset == chunk.size) break
                val take = minOf(2 - pending.size, chunk.size - offset)
                pending += chunk.copyOfRange(offset, offset + take)
                offset += take
                if (pending.size < 2) break
            }
            val length = (pending[0].toInt() and 255) or
                ((pending[1].toInt() and 255) shl 8)
            if (length !in 6..maxFrameBytes) {
                failed = true
                pending = ByteArray(0)
                return emptyList()
            }
            if (pending.size < length) {
                if (offset == chunk.size) break
                val take = minOf(length - pending.size, chunk.size - offset)
                pending += chunk.copyOfRange(offset, offset + take)
                offset += take
                if (pending.size < length) break
            }
            val frame = pending
            if (GarminFrameValidator.inspectVerified(frame) == null) {
                failed = true
                pending = ByteArray(0)
                return emptyList()
            }
            frames.add(frame)
            pending = ByteArray(0)
        }
        return frames
    }
}

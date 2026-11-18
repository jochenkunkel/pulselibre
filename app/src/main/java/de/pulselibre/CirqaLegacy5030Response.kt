package de.pulselibre

/** Structural metadata only. No inner value is classified as HANDSHAKE_COMPLETE success. */
object CirqaLegacy5030Response {
    data class Metadata(val outerStatus: Int, val payloadLength: Int, val firstResultUnsigned: Int?) {
        /** Connect yb60 sign-extends the first byte; empty response becomes -1. */
        val connectResult: Int get() = firstResultUnsigned?.toByte()?.toInt() ?: -1
    }

    fun inspect(frame: ByteArray): Metadata? {
        if (frame.size > MAX_FRAME_BYTES) return null
        val h = GarminGfdiHeader.inspect(frame) ?: return null
        if (h.compact || h.type != 5000 || h.responseTo != 5030 || h.responseStatus == null) return null
        val length = frame.size - 9
        return Metadata(h.responseStatus, length, if (length > 0) frame[7].toInt() and 255 else null)
    }

    const val MAX_FRAME_BYTES = 64
}

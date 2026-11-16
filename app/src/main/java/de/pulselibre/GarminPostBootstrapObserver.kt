package de.pulselibre

/**
 * Privacy-preserving structural telemetry for the GFDI messages seen after
 * CIRQA DeviceInformation ACK. No payload bytes, FIT fields or identifiers.
 */
class GarminPostBootstrapObserver {
    private val counts = linkedMapOf<Int, Int>()
    private val bodyLengths = linkedMapOf<Int, MutableMap<Int, Int>>()
    private val firstOrder = mutableListOf<Int>()
    var afterAckFrames = 0
        private set

    fun observe(frame: ByteArray, afterAck: Boolean) {
        if (!afterAck || GarminFrameValidator.inspectVerified(frame) == null) return
        val type = (frame[2].toInt() and 255) or ((frame[3].toInt() and 255) shl 8)
        if (type != 5050 && type != 5011 && type != 5024 && type != 5000) return
        afterAckFrames++
        counts[type] = (counts[type] ?: 0) + 1
        val length = frame.size - 6
        val lengths = bodyLengths.getOrPut(type) { linkedMapOf() }
        lengths[length] = (lengths[length] ?: 0) + 1
        if (firstOrder.size < 12) firstOrder.add(type)
    }

    fun summary(): String =
        "Post-ACK-Struktur (nur Metadaten): Frames=$afterAckFrames; Reihenfolge=$firstOrder; " +
            "Typen=$counts; Body-Längen=$bodyLengths; Payload-Inhalte=unterdrückt"
}

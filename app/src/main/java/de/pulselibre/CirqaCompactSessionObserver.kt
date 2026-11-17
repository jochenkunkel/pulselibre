package de.pulselibre

/**
 * Passive, CRC-verified observation of compact GFDI transport identifiers.
 * These are peer (RX) identifiers, NOT proof of the host (TX) counter state.
 * No BLE writes and no payload retention.
 */
class CirqaCompactSessionObserver {
    data class Observation(val kind: Int, val sequence: Int, val counter: Int)
    var received = 0
        private set
    var duplicates = 0
        private set
    var discontinuities = 0
        private set
    var last: Observation? = null
        private set

    fun observe(frame: ByteArray): Observation? {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 8) return null
        val type = (frame[2].toInt() and 255) or ((frame[3].toInt() and 255) shl 8)
        if (type and 0x8000 == 0) return null
        val sequence = (type ushr 8) and 0x7f
        if (sequence !in 1..127) return null
        val current = Observation(
            type and 255, sequence,
            (frame[4].toInt() and 255) or ((frame[5].toInt() and 255) shl 8)
        )
        last?.let { previous ->
            if (previous == current) duplicates++
            else if (current.sequence != (if (previous.sequence == 127) 1 else previous.sequence + 1) ||
                current.counter != ((previous.counter + 1) and 0xffff)) discontinuities++
        }
        last = current
        received++
        return current
    }

    fun summary(): String =
        "Kompakte RX-Frames=$received, Duplikate=$duplicates, Diskontinuitäten=$discontinuities; " +
            "letzte RX-IDs=${last?.let { "seq=${it.sequence}/counter=${it.counter}/kind=${it.kind}" } ?: "keine"}; " +
            "TX-Session-Zähler=UNBEKANNT (RX-Werte nicht als TX übernehmen)"
}

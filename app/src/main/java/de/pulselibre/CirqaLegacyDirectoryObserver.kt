package de.pulselibre

/**
 * Passive, privacy-preserving GFDI legacy directory-path counters.
 * This class does not decode directory contents, infer CIRQA capabilities,
 * send commands or treat the presence of any one type as proof of a sync.
 */
class CirqaLegacyDirectoryObserver {
    private val counts = linkedMapOf<Int, Int>()
    private val relevant = setOf(5007, 5031, 5002, 5004)

    fun observeVerified(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return
        val wireType = (frame[2].toInt() and 255) or
            ((frame[3].toInt() and 255) shl 8)
        // The Connect analysis establishes the ordinary GFDI message IDs.
        // Compact variants are deliberately not guessed or normalized here.
        if (wireType !in relevant) return
        counts[wireType] = (counts[wireType] ?: 0) + 1
    }

    fun count(type: Int): Int = counts[type] ?: 0

    fun summary(): String =
        "Legacy-Verzeichnispfad (nur RX, kein CIRQA-Beweis): " +
            "5007=${count(5007)}, 5031=${count(5031)}, " +
            "5002=${count(5002)}, 5004=${count(5004)}; " +
            "Payloads/Dateinamen=unterdrückt"
}

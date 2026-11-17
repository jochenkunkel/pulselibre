package de.pulselibre

/**
 * Metadata-only evidence for frames observed after a confirmed 5050 write.
 * A GFDI status ACK is not proof that history sync is unlocked.
 */
class Cirqa5050PostWriteEvidence {
    private var baseline: Map<String, Int>? = null
    private val counts = linkedMapOf<String, Int>()

    fun onWriteConfirmed(frameTypes: Map<String, Int>) {
        if (baseline == null) baseline = frameTypes.toMap()
    }

    fun observe(frameTypes: Map<String, Int>) {
        val before = baseline ?: return
        for ((type, total) in frameTypes) {
            val delta = (total - (before[type] ?: 0)).coerceAtLeast(0)
            if (delta > 0) counts[type] = delta
        }
    }

    fun summary(): String {
        val started = baseline != null
        return "Post-5050-Schreibbestätigung: Beobachtung aktiv=$started; " +
            "nachfolgende GFDI-Frames=${counts.values.sum()}; Typen=$counts; " +
            "5050-Wiederholungen=${counts["gfdi-5050"] ?: 0}; " +
            "5011-Frames=${counts["gfdi-5011"] ?: 0}; " +
            "5000-Statusframes=${counts["gfdi-5000"] ?: 0}; " +
            "Historienfreigabe=nicht nachgewiesen"
    }
}

package de.pulselibre

/**
 * Offline-only comparison of two GFDI 5050 host capability candidates.
 *
 * Not a BLE experiment. No transport references or side effects.
 * The SYNC-bit hypothesis is unverified on CIRQA.
 */
object Cirqa5050SyncHypothesis {
    data class Comparison(
        val baseline: ByteArray,
        val syncCandidate: ByteArray,
        val changedBitPositions: List<Int>
    )

    fun compare(): Comparison {
        val baseline = Cirqa5050OfflineHostCapabilities.candidate(byteCount = 15)
        val candidate = Cirqa5050OfflineHostCapabilities.candidate(
            advertiseSyncForOfflineComparison = true, byteCount = 15
        )
        val changed = (0 until baseline.size * 8).filter { bit ->
            val byte = bit / 8
            val mask = 1 shl (bit % 8)
            ((baseline[byte].toInt() xor candidate[byte].toInt()) and mask) != 0
        }
        check(changed == listOf(Cirqa5050OfflineHostCapabilities.SYNC_BIT))
        return Comparison(baseline, candidate, changed)
    }
}

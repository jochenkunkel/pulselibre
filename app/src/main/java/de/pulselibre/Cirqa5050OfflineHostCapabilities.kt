package de.pulselibre

/**
 * Conservative offline-only host capability policy for Garmin GFDI 5050.
 *
 * No CIRQA capability bit has been validated end-to-end. The default mask
 * advertises nothing. SYNC (ordinal 3 in public Garmin capability tables)
 * can be explicitly selected for offline analysis, but is not enabled by
 * default and must not be sent over BLE without a separate review.
 */
object Cirqa5050OfflineHostCapabilities {
    const val REFERENCE_BYTES = 15
    const val CIRQA_OBSERVED_BYTES = 18
    const val SYNC_BIT = 3

    fun candidate(advertiseSyncForOfflineComparison: Boolean = false,
                  byteCount: Int = REFERENCE_BYTES): ByteArray {
        require(byteCount == REFERENCE_BYTES || byteCount == CIRQA_OBSERVED_BYTES)
        val result = ByteArray(byteCount)
        if (advertiseSyncForOfflineComparison) {
            result[SYNC_BIT / 8] = (1 shl (SYNC_BIT % 8)).toByte()
        }
        return result
    }

    fun offlineFrame(advertiseSyncForOfflineComparison: Boolean = false,
                     byteCount: Int = REFERENCE_BYTES): ByteArray =
        Cirqa5050OfflineResponse.frame(candidate(advertiseSyncForOfflineComparison, byteCount))
}

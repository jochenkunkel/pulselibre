package de.pulselibre

/**
 * #637 (S10, 27.11.2026): with the band next to the phone, runs repeatedly failed at connection setup – GATT 133 right
 * after Bluetooth was enabled and on a follow-up run, CCCD error 129 when enabling notifications. The same run in a
 * fresh attempt succeeded. These failures happen BEFORE the band accepted the sync, so nothing was listed, pulled or
 * stored and a repeat cannot duplicate or lose data. Exactly one repeat after a pause; any other outcome is final.
 */
internal object HistorySyncRetryPolicy {
    const val MAX_ATTEMPTS = 2
    const val DELAY_MS = 8_000L

    fun shouldRetry(report: String, attempt: Int): Boolean =
        attempt < MAX_ATTEMPTS &&
            report.contains("incompleteReason=sync-not-accepted") &&
            report.contains("itemListSent=false") &&
            (report.contains("(GATT 133") || report.contains("CCCD-Fehler"))
}

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

    /**
     * S10 28.11.2026: shortly after an activity ended on the band, every pull was rejected with result=3 and the run
     * stopped after [CirqaPullRejectionPolicy.MAX_CONSECUTIVE] rejections; the same download ~15 minutes later moved all
     * 20 files. Nothing was transferred or changed, so one later repeat is safe.
     */
    const val BUSY_DELAY_MS = 5 * 60_000L

    fun shouldRetry(report: String, attempt: Int): Boolean = delayFor(report, attempt) != null

    /** Pause before the single repeat, or null when the outcome is final. */
    fun delayFor(report: String, attempt: Int): Long? = when {
        attempt >= MAX_ATTEMPTS -> null
        report.contains("incompleteReason=sync-not-accepted") && report.contains("itemListSent=false") &&
            (report.contains("(GATT 133") || report.contains("CCCD-Fehler")) -> DELAY_MS
        bandBusy(report) -> BUSY_DELAY_MS
        else -> null
    }

    /** Every requested pull was refused with result=3 and nothing was transferred (band not ready, e.g. after an activity). */
    fun bandBusy(report: String): Boolean =
        report.contains("pull-rejected(result=3)") && report.contains("Ablehnungen in Folge") &&
            report.contains("files=[successful=0,")
}

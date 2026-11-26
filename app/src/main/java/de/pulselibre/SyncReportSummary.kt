package de.pulselibre

/**
 * One readable sentence on top of the technical history-sync report (owner 26.11.2026: the Gerät page showed only
 * "runId=…; runDrained=false; coverage=…"). Pure text parsing of the report the sync already writes: nothing about the
 * download changes, and the full report stays available below (tap to expand).
 */
internal object SyncReportSummary {
    private fun int(report: String, key: String): Int? = Regex("""\b$key=(\d+)""").find(report)?.groupValues?.get(1)?.toIntOrNull()

    fun explain(strings: Strings, report: String): String? {
        if (!report.contains("Sync-Beobachtung")) return null
        val listed = int(report, "listed") ?: int(report, "selected")
        val successful = Regex("""files=\[successful=(\d+)""").find(report)?.groupValues?.get(1)?.toIntOrNull()
        val pullResult = Regex("""\|result=(\d+),""").find(report)?.groupValues?.get(1)
        val pending = int(report, "pendingQueue")
        val drained = report.contains("runDrained=true") && report.contains("incompleteReason=none")
        return when {
            drained -> strings.get(R.string.sync_sum_complete, successful ?: 0)
            report.contains("Pull-Antwort nicht erfolgreich") && pullResult != null ->
                strings.get(R.string.sync_sum_pull_rejected, pullResult, listed ?: 0, pending ?: 0)
            report.contains("incompleteReason=next-transaction-followup-empty") ->
                strings.get(R.string.sync_sum_followup_empty, successful ?: 0, listed ?: successful ?: 0)
            successful != null && listed != null ->
                strings.get(R.string.sync_sum_partial, successful, listed)
            else -> null
        }
    }

    /** Text for the status card: readable summary, then the hint and the technical report. */
    fun display(strings: Strings, report: String): String =
        explain(strings, report)?.let { it + "\n\n" + strings.get(R.string.sync_sum_details) + "\n" + report } ?: report
}

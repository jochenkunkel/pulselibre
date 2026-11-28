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

    /**
     * FIT file types the band offered (its own labels "FIT_TYPE_<n>" = FIT profile file type n). Daily history –
     * stress, SpO2, breathing, all-day pulse, sleep – only comes from monitoring_a (15), monitoring_daily (28),
     * monitoring_b (32) and sleep (49) files; activity files (4) only hold pulse/distance of a recording (#637).
     */
    private val DAILY_HISTORY_TYPES = setOf(15, 28, 32, 49)

    fun offeredFitTypes(report: String): Set<Int> =
        Regex("""types=([^;|\]]*)""").findAll(report)
            .flatMap { Regex("""FIT_TYPE_(\d+):""").findAll(it.groupValues[1]) }
            .mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()

    /** True when the band listed FIT files but none with daily history. */
    fun lacksDailyHistory(report: String): Boolean {
        val types = offeredFitTypes(report)
        return types.isNotEmpty() && types.none { it in DAILY_HISTORY_TYPES }
    }

    /** Files not transferred again because an identical, verified import exists (#733). */
    fun skippedVerified(report: String): Int = int(report, "skipped") ?: 0

    /**
     * #752: PulsLibre asked the band (Smart 47.3) to close its current monitoring data into a file. "sent" without
     * "answered" means the band did not confirm, so daily data since the last download may be missing.
     */
    fun flushUnanswered(report: String): Boolean = report.contains("monitoringFlush=sent")

    /** Text for the status card: readable summary, then the hints and the technical report. */
    fun display(strings: Strings, report: String): String {
        val summary = explain(strings, report) ?: return report
        val hints = buildList {
            skippedVerified(report).takeIf { it > 0 }?.let { add(strings.get(R.string.sync_sum_skipped, it)) }
            if (flushUnanswered(report)) add(strings.get(R.string.sync_sum_flush_unanswered))
            if (lacksDailyHistory(report)) add(strings.get(R.string.sync_sum_no_daily_history))
            if (HistorySyncRetryPolicy.bandBusy(report)) add(strings.get(R.string.sync_sum_band_busy))
        }
        return (listOf(summary) + hints).joinToString("\n\n") + "\n\n" + strings.get(R.string.sync_sum_details) + "\n" + report
    }
}

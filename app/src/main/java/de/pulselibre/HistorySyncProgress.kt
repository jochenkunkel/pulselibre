package de.pulselibre

/**
 * User-facing progress of the historical CIRQA download (GUI side of the contract with the FIT agent).
 *
 * The producer (the sync run) reports a [Stage] and optional file counts; this model only decides how honestly that is
 * shown. Rule: coverage is never claimed. A finished run reads "finished, coverage not proven" unless the producer
 * explicitly passes [coverageProven] = true, and the bar never reaches 100 % without that proof (a drained file list is
 * not a complete device history, see [CirqaSyncCoverage]).
 */
internal enum class HistorySyncStage { IDLE, CONNECTING, STARTED, LISTING_FILES, TRANSFERRING, VALIDATING, IMPORTING, FINISHED, FINISHED_INCOMPLETE, INTERRUPTED, FAILED }

internal data class HistorySyncProgress(
    val stage: HistorySyncStage,
    val filesDone: Int? = null,
    val filesTotal: Int? = null,
    val coverageProven: Boolean = false
) {
    val isRunning: Boolean get() = stage in RUNNING
    val isTerminal: Boolean get() = stage == HistorySyncStage.FINISHED || stage == HistorySyncStage.FINISHED_INCOMPLETE ||
        stage == HistorySyncStage.INTERRUPTED || stage == HistorySyncStage.FAILED

    /** Bar fill 0..1, or null when no honest number exists. Capped below 1 unless the coverage is proven. */
    fun fraction(): Float? {
        val total = filesTotal ?: return null
        val done = filesDone ?: return null
        if (total <= 0 || done < 0) return null
        val raw = (done.toFloat() / total).coerceIn(0f, 1f)
        return if (coverageProven) raw else minOf(raw, MAX_UNPROVEN)
    }

    companion object {
        val RUNNING = setOf(HistorySyncStage.CONNECTING, HistorySyncStage.STARTED, HistorySyncStage.LISTING_FILES,
            HistorySyncStage.TRANSFERRING, HistorySyncStage.VALIDATING, HistorySyncStage.IMPORTING)
        const val MAX_UNPROVEN = 0.95f
        val IDLE = HistorySyncProgress(HistorySyncStage.IDLE)

        /**
         * Interim mapping of the final run report until the sync run reports stages itself (agreed with the FIT agent):
         * `runDrained=true` + `incompleteReason=none` = finished (coverage still NOT proven); any other report that
         * carries a run marker = incomplete; a report without a run marker (error text, timeout) = failed.
         */
        fun fromReport(report: String): HistorySyncProgress = when {
            report.contains("runDrained=true") && report.contains("incompleteReason=none") -> HistorySyncProgress(HistorySyncStage.FINISHED)
            report.contains("runDrained=") || report.contains("incompleteReason=") -> HistorySyncProgress(HistorySyncStage.FINISHED_INCOMPLETE)
            else -> HistorySyncProgress(HistorySyncStage.FAILED)
        }
    }
}

internal object HistorySyncProgressText {
    fun title(strings: Strings, p: HistorySyncProgress): String = strings.get(when (p.stage) {
        HistorySyncStage.IDLE -> R.string.hsp_idle
        HistorySyncStage.CONNECTING -> R.string.hsp_connecting
        HistorySyncStage.STARTED -> R.string.hsp_started
        HistorySyncStage.LISTING_FILES -> R.string.hsp_listing
        HistorySyncStage.TRANSFERRING -> R.string.hsp_transferring
        HistorySyncStage.VALIDATING -> R.string.hsp_validating
        HistorySyncStage.IMPORTING -> R.string.hsp_importing
        HistorySyncStage.FINISHED -> if (p.coverageProven) R.string.hsp_finished_proven else R.string.hsp_finished_unproven
        HistorySyncStage.FINISHED_INCOMPLETE -> R.string.hsp_incomplete
        HistorySyncStage.INTERRUPTED -> R.string.hsp_interrupted
        HistorySyncStage.FAILED -> R.string.hsp_failed
    })

    /** "n of m files" only when both numbers are real; never a percentage and never the word "complete" without proof. */
    fun detail(strings: Strings, p: HistorySyncProgress): String? {
        val done = p.filesDone; val total = p.filesTotal
        val files = if (done != null && total != null && total > 0 && done >= 0) strings.get(R.string.hsp_files, done.coerceAtMost(total), total) else null
        val note = if (p.isTerminal && !p.coverageProven && p.stage != HistorySyncStage.IDLE) strings.get(R.string.hsp_coverage_note) else null
        return listOfNotNull(files, note).takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

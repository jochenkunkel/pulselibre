package de.pulselibre

/**
 * How the last history-sync report is kept for the Gerät page (#637, FIT request 26.11.2026 18:05 UTC).
 *
 * The report was cut at 4000 characters: the long per-item timing came first, so `terminal=[…]` and every
 * `pull#…|result=…` diagnosis fell off – exactly the fields needed to explain a failed pull. Now:
 * 1. the head (run id, message, cause, counters up to `files=[`) stays first,
 * 2. a priority block repeats `terminal=[…]` and all pull results right after it,
 * 3. the unchanged original report follows (no field removed),
 * 4. everything is kept up to [MAX_CHARS]; a cut is marked visibly at the end.
 * Pure text handling: nothing about the sync itself changes.
 */
internal object SyncReportStore {
    const val MAX_CHARS = 64 * 1024
    const val PRIORITY_LABEL = "Diagnose zuerst:"
    const val TRUNCATED_MARK = " … [Bericht gekürzt: Obergrenze 64 KiB erreicht]"

    /** The bracketed segment starting at [key] (`terminal=[`), with nested brackets; null when absent or unbalanced. */
    private fun bracketed(report: String, key: String): String? {
        val start = report.indexOf(key).takeIf { it >= 0 } ?: return null
        var depth = 0
        for (i in start + key.length - 1 until report.length) {
            when (report[i]) { '[' -> depth++; ']' -> { depth--; if (depth == 0) return report.substring(start, i + 1) } }
        }
        return null
    }

    fun pullResults(report: String): List<String> = Regex("""pull#\d+:[^|\]]*\|[^\]|]*""").findAll(report).map { it.value }.toList()

    fun prioritized(report: String): String {
        val terminal = bracketed(report, "terminal=[")
        val pulls = pullResults(report)
        if (terminal == null && pulls.isEmpty()) return report
        val headEnd = report.indexOf("files=[").takeIf { it > 0 } ?: report.indexOf(';').let { if (it < 0) report.length else it + 1 }
        val block = buildString {
            append(" $PRIORITY_LABEL ")
            terminal?.let { append(it).append("; ") }
            if (pulls.isNotEmpty()) append("pulls=[").append(pulls.joinToString(" | ")).append("]; ")
            append("Vollständiger Bericht: ")
        }
        return report.substring(0, headEnd) + block + report
    }

    fun forStorage(report: String): String {
        val text = prioritized(report)
        return if (text.length <= MAX_CHARS) text else text.take(MAX_CHARS - TRUNCATED_MARK.length) + TRUNCATED_MARK
    }
}

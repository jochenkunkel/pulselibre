package de.pulselibre

/** Stable, privacy-redacted summary for the historical-sync status line. */
internal object CirqaSyncRunReport {
    data class Overview(
        val runIdentity: String,
        val message: String,
        val runDrained: Boolean,
        val coverage: String,
        val incompleteReason: String,
        val accepted: Boolean,
        val itemListSent: Boolean,
        val pullSent: Boolean,
        val fileSummary: String,
        val itemSummary: String,
        val diagnostics: String
    )

    /**
     * Keep file-stage totals before verbose item-list details. The UI initially
     * shows only a few lines, so users must not have to expand/scroll past a
     * potentially long list to see whether files reached SQLite.
     */
    fun format(overview: Overview): String =
        "Sync-Beobachtung: ${overview.runIdentity}; ${overview.message}; " +
            "runDrained=${overview.runDrained}; ${overview.coverage} " +
            "incompleteReason=${overview.incompleteReason}; " +
            "accepted=${overview.accepted}; itemListSent=${overview.itemListSent}; " +
            "pullSent=${overview.pullSent}; files=[${overview.fileSummary}]; " +
            "items=[${overview.itemSummary}]; ${overview.diagnostics}"
}

package de.pulselibre

/** Conservative limits for one explicitly authorized local history session. */
object CirqaLegacyDownloadLimits {
    const val MAX_DIRECTORY_BYTES = 1 * 1024 * 1024
    const val MAX_FIT_FILE_BYTES = 4 * 1024 * 1024
    const val MAX_FIT_FILES = 4
    const val MAX_TOTAL_FIT_BYTES = 16 * 1024 * 1024
    const val MAX_SESSION_MILLIS = 5 * 60 * 1000L

    data class EntryDecision(
        val entry: CirqaLegacyDirectoryParser.Entry,
        val reason: String
    ) {
        val eligible: Boolean get() = reason == "eligible"
    }

    fun classifyEntry(
        entry: CirqaLegacyDirectoryParser.Entry,
        supportedTypes: Set<Pair<Int, Int>>
    ): EntryDecision = EntryDecision(
        entry = entry,
        reason = when {
            entry.type != 128 -> "not-fit-type"
            entry.fileIndex !in 1..65535 -> "reserved-or-invalid-index"
            !entry.readable -> "not-readable"
            (entry.type to entry.subtype) !in supportedTypes -> "unsupported-subtype"
            entry.size !in 1..MAX_FIT_FILE_BYTES.toLong() -> "size-out-of-range"
            else -> "eligible"
        }
    )

    fun classifyEntries(
        entries: List<CirqaLegacyDirectoryParser.Entry>,
        supportedTypes: Set<Pair<Int, Int>>
    ): List<EntryDecision> = entries.map { classifyEntry(it, supportedTypes) }

    fun selectReadableSupportedFits(
        entries: List<CirqaLegacyDirectoryParser.Entry>,
        supportedTypes: Set<Pair<Int, Int>>
    ): List<CirqaLegacyDirectoryParser.Entry> =
        classifyEntries(entries, supportedTypes).asSequence()
            .filter { it.eligible }
            .map { it.entry }
            .sortedBy { it.fileIndex }
            .take(MAX_FIT_FILES)
            .toList()
}

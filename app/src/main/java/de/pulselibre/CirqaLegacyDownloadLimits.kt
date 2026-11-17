package de.pulselibre

/** Conservative limits for one explicitly authorized local history session. */
object CirqaLegacyDownloadLimits {
    const val MAX_DIRECTORY_BYTES = 1 * 1024 * 1024
    const val MAX_FIT_FILE_BYTES = 4 * 1024 * 1024
    const val MAX_FIT_FILES = 4
    const val MAX_TOTAL_FIT_BYTES = 16 * 1024 * 1024
    const val MAX_SESSION_MILLIS = 5 * 60 * 1000L

    fun selectReadableSupportedFits(
        entries: List<CirqaLegacyDirectoryParser.Entry>,
        supportedTypes: Set<Pair<Int, Int>>
    ): List<CirqaLegacyDirectoryParser.Entry> =
        entries.asSequence()
            .filter { it.type == 128 && it.readable && (it.type to it.subtype) in supportedTypes }
            .filter { it.size in 1..MAX_FIT_FILE_BYTES.toLong() }
            .sortedBy { it.fileIndex }
            .take(MAX_FIT_FILES)
            .toList()
}

package de.pulselibre

/**
 * Offline-only candidate selection for historical FIT downloads.
 *
 * Supported and uploadable subtypes must be observed from the actual device /
 * verified sync configuration. No fallback subtypes are silently assumed.
 * This does not schedule requests or modify upload/archive flags.
 */
object CirqaLegacyFitFileSelector {
    fun select(
        directory: CirqaLegacyDirectoryParser.Directory,
        supportedTypes: List<CirqaLegacyFileTypesParser.FileType>,
        uploadableSubtypes: Set<Int>
    ): List<CirqaLegacyDirectoryParser.Entry> {
        if (uploadableSubtypes.isEmpty() || supportedTypes.isEmpty()) return emptyList()
        val supportedFitSubtypes = supportedTypes.asSequence()
            .filter { it.type == 0x80 }
            .map { it.subtype }
            .toSet()
        return directory.entries.filter { entry ->
            entry.fileIndex != 0 &&
                entry.type == 0x80 &&
                entry.readable &&
                entry.subtype in uploadableSubtypes &&
                entry.subtype in supportedFitSubtypes
        }.distinctBy { it.fileIndex }
    }
}

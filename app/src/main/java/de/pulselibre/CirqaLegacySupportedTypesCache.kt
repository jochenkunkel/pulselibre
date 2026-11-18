package de.pulselibre

/** Parses only the compact, locally saved result of a CRC-validated 5031 probe. */
object CirqaLegacySupportedTypesCache {
    private val pairPattern = Regex("""(\d{1,3})/(\d{1,3}):""")

    fun parse(summary: String?): Set<Pair<Int, Int>> {
        if (summary.isNullOrBlank() || !summary.contains("Status=0;")) return emptySet()
        return pairPattern.findAll(summary).mapNotNull { match ->
            val type = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val subtype = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (type in 0..255 && subtype in 0..255) type to subtype else null
        }.toSet()
    }
}

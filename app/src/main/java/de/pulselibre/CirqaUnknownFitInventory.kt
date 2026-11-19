package de.pulselibre

import java.io.File
import java.nio.file.Files

/**
 * Read-only, bounded inventory of existing app-private, content-addressed FIT files.
 * Run on a worker thread; never call on the Android UI thread.
 * No raw health values, filenames, or payloads are returned.
 */
internal object CirqaUnknownFitInventory {
    const val MAX_FILES = 256
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    const val MAX_TOTAL_BYTES = 64L * 1024 * 1024
    private val namePattern = Regex("^[0-9a-f]{64}\\.fit$")

    data class Scan(
        val filesChecked: Int,
        val filesRejected: Int,
        val truncated: Boolean,
        val fields: List<FitHistoricalObservations.UnknownField>
    )

    fun scan(root: File, cancelled: () -> Boolean = { false }): Scan {
        if (!root.isDirectory || Files.isSymbolicLink(root.toPath())) return Scan(0, 0, false, emptyList())
        val files = root.listFiles()?.asSequence()
            ?.filter { it.name.matches(namePattern) }
            ?.sortedBy { it.name }
            ?.toList() ?: return Scan(0, 0, false, emptyList())
        val result = ArrayList<FitHistoricalObservations.UnknownField>()
        var checked = 0
        var rejected = 0
        var total = 0L
        var truncated = files.size > MAX_FILES
        for (file in files.take(MAX_FILES)) {
            if (cancelled()) { truncated = true; break }
            if (!file.isFile || Files.isSymbolicLink(file.toPath()) ||
                file.length() !in 1..MAX_FILE_BYTES.toLong() ||
                total + file.length() > MAX_TOTAL_BYTES) {
                rejected++
                if (total + file.length() > MAX_TOTAL_BYTES) { truncated = true; break }
                continue
            }
            total += file.length()
            checked++
            val decoded = try {
                FitHistoricalObservations.decode(file.readBytes())
            } catch (_: java.io.IOException) {
                null
            }
            if (decoded == null || decoded.unknownFields.firstOrNull()?.sourceSha256 != null &&
                decoded.unknownFields.first().sourceSha256 != file.name.removeSuffix(".fit")) {
                rejected++
            } else {
                // The original FIT decoder validates CRC before reporting metadata.
                result.addAll(decoded.unknownFields)
            }
        }
        return Scan(checked, rejected, truncated, result)
    }
}

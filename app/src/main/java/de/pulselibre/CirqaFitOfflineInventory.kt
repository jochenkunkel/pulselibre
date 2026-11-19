package de.pulselibre

import java.io.File
import java.nio.file.Files

/**
 * Read-only local inventory of previously downloaded FIT originals (#150).
 * Does not open Bluetooth, import into SQLite, or expose raw samples/device IDs.
 * Run on a worker thread; never place the private source directory in Git.
 */
internal object CirqaFitOfflineInventory {
    private const val MAX_BYTES = 64L * 1024L * 1024L

    data class Summary(
        val filesSeen: Int,
        val verifiedFiles: Int,
        val rejectedFiles: Int,
        val recordMessages: Int,
        val supportedObservations: Int,
        val importableObservations: Int,
        val missingTimestamps: Int,
        val outsideDisplayRange: Int,
        val metadataRecords: Int,
        val byMetric: Map<FitHistoricalObservations.Metric, Int>
    ) {
        /** Aggregate-only output: no timestamps, identifiers, filenames or samples. */
        fun redactedText(): String = buildString {
            appendLine("FIT Offline-Inventur")
            appendLine("Dateien: $filesSeen; verifiziert: $verifiedFiles; abgewiesen: $rejectedFiles")
            appendLine("Datensätze: $recordMessages; unterstützte Messwerte: $supportedObservations")
            appendLine("Importierbar: $importableObservations; ohne Zeitstempel: $missingTimestamps")
            appendLine("Außerhalb Anzeigegrenzen: $outsideDisplayRange; Metadatensätze: $metadataRecords")
            byMetric.forEach { (metric, count) -> appendLine("${metric.name}: $count") }
            appendLine("Hinweis: Diese Inventur ist kein SQLite-Abgleich und beweist keine vollständige Gerätehistorie.")
        }
    }

    fun inspect(directory: File): Summary {
        require(directory.isDirectory && !Files.isSymbolicLink(directory.toPath())) {
            "Verified FIT directory unavailable or unsafe"
        }
        val files = directory.listFiles()?.filter { it.isFile && it.name.endsWith(".fit") }
            ?: error("Cannot list private FIT directory")
        var verified = 0
        var rejected = 0
        var records = 0
        var observations = 0
        var importable = 0
        var missingTime = 0
        var invalidRange = 0
        var metadata = 0
        val counts = linkedMapOf<FitHistoricalObservations.Metric, Int>()
        files.forEach { file ->
            if (Files.isSymbolicLink(file.toPath()) || file.length() !in 1..MAX_BYTES) {
                rejected++
                return@forEach
            }
            val bytes = file.readBytes()
            if (file.name != CirqaFitBackfill.expectedFileName(bytes)) {
                rejected++
                return@forEach
            }
            val decoded = FitHistoricalObservations.decode(bytes)
            if (decoded == null) {
                rejected++
                return@forEach
            }
            verified++
            records += decoded.dataRecordCount
            metadata += decoded.deviceMetadata.size
            observations += decoded.observations.size
            decoded.observations.forEach { item ->
                counts[item.metric] = (counts[item.metric] ?: 0) + 1
                when (item.quality) {
                    FitHistoricalObservations.Quality.PROFILE_VALID ->
                        if (item.timestampEpochMillis != null) importable++ else missingTime++
                    FitHistoricalObservations.Quality.MISSING_TIMESTAMP -> missingTime++
                    FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE -> invalidRange++
                }
            }
        }
        return Summary(files.size, verified, rejected, records, observations, importable,
            missingTime, invalidRange, metadata, counts.toMap())
    }
}

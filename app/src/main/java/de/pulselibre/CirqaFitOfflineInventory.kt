package de.pulselibre

import java.io.File
import java.nio.file.Files

/**
 * Read-only local inventory of previously downloaded FIT originals (#150).
 * Does not open Bluetooth, import into SQLite, or expose raw samples/device IDs.
 * Run on a worker thread; never place the private source directory in Git.
 */
internal object CirqaFitOfflineInventory {
    private const val MAX_FILES = 256
    private const val MAX_BYTES = 64L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 64L * 1024L * 1024L
    private const val MAX_FIELD_SUMMARIES_PER_FILE = 24

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
        val byMetric: Map<FitHistoricalObservations.Metric, Int>,
        val importableKeysInDatabase: Int?,
        val importableKeysMissingFromDatabase: Int?,
        val verifiedSourcesInDatabase: Int?,
        val filesNotScanned: Int,
        val perFile: List<FileSummary>
    ) {
        /** Redacted count-only output: no timestamps, identifiers, filenames or samples. */
        fun redactedText(): String = buildString {
            appendLine("FIT Offline-Inventur")
            appendLine("Dateien: $filesSeen; verifiziert: $verifiedFiles; abgewiesen: $rejectedFiles")
            if (filesNotScanned > 0) appendLine("Nicht geprüft (Sicherheitslimit): $filesNotScanned")
            appendLine("Datensätze: $recordMessages; unterstützte Messwerte: $supportedObservations")
            appendLine("Importierbar: $importableObservations; ohne Zeitstempel: $missingTimestamps")
            appendLine("Außerhalb Anzeigegrenzen: $outsideDisplayRange; Metadatensätze: $metadataRecords")
            byMetric.forEach { (metric, count) -> appendLine("${metric.name}: $count") }
            appendLine("Importierbare Schlüssel in Datenbank: ${importableKeysInDatabase ?: "nicht abgeglichen"}")
            appendLine("Importierbare Schlüssel dort fehlend: ${importableKeysMissingFromDatabase ?: "nicht abgeglichen"}")
            appendLine("Verifizierte FIT-Quellen in Datenbank: ${verifiedSourcesInDatabase ?: "nicht abgeglichen"}")
            appendLine("Per-Datei-Coverage (nur Zähler; Aliase lokal und laufbezogen):")
            perFile.forEach { file ->
                append("${file.alias}: ${file.status}; Records=${file.recordMessages}; " +
                    "Observations=${file.supportedObservations}; importierbar=${file.importableObservations}; " +
                    "ohneZeit=${file.missingTimestamps}; außerhalb=${file.outsideDisplayRange}; " +
                    "nichtUnterstützteFelder=${file.unsupportedMeasurementFields}; Metadaten=${file.metadataRecords}; " +
                    "unbekannteSignaturen=${file.unknownFieldSignatures}")
                if (file.byMetric.isNotEmpty()) {
                    append("; Metriken=")
                    append(file.byMetric.entries.joinToString(",") { "${it.key.name}:${it.value}" })
                }
                file.databaseMatchedKeys?.let { matched ->
                    append("; DB-Schlüssel=$matched/${file.importableObservations}")
                }
                file.databaseSourcePresent?.let { append("; QuelleInDB=$it") }
                if (file.fieldCoverage.isNotEmpty()) {
                    append("; Felder=")
                    append(file.fieldCoverage.joinToString(","))
                    if (file.fieldCoverageOmitted > 0) {
                        append(",weitere=${file.fieldCoverageOmitted}")
                    }
                }
                appendLine()
            }
            appendLine("Hinweis: Lokaler Abgleich; beweist keine vollständige Gerätehistorie.")
        }
    }

    data class FileSummary(
        val alias: String,
        val status: String,
        val recordMessages: Int,
        val supportedObservations: Int,
        val importableObservations: Int,
        val missingTimestamps: Int,
        val outsideDisplayRange: Int,
        val unsupportedMeasurementFields: Int,
        val metadataRecords: Int,
        val unknownFieldSignatures: Int,
        val byMetric: Map<FitHistoricalObservations.Metric, Int>,
        val databaseMatchedKeys: Int?,
        val databaseSourcePresent: Boolean?,
        /** Structural FIT message/field coverage only; never values or timestamps. */
        val fieldCoverage: List<String>,
        val fieldCoverageOmitted: Int
    )

    fun inspect(
        directory: File,
        databaseSampleIds: Set<String> = emptySet(),
        databaseSourceHashes: Set<String> = emptySet(),
        databaseAvailable: Boolean = false
    ): Summary {
        require(directory.isDirectory && !Files.isSymbolicLink(directory.toPath())) {
            "Verified FIT directory unavailable or unsafe"
        }
        val files = directory.listFiles()?.filter { it.isFile && it.name.endsWith(".fit") }
            ?.sortedBy { it.name } ?: error("Cannot list private FIT directory")
        val fileSummaries = mutableListOf<FileSummary>()
        var verified = 0
        var rejected = 0
        var records = 0
        var observations = 0
        var importable = 0
        var missingTime = 0
        var invalidRange = 0
        var metadata = 0
        var matchedKeys = 0
        var candidateKeys = 0
        var matchedSources = 0
        var bytesRead = 0L
        var filesNotScanned = 0
        val counts = linkedMapOf<FitHistoricalObservations.Metric, Int>()
        fun rejected(alias: String) {
            rejected++
            fileSummaries += FileSummary(
                alias = alias,
                status = "abgewiesen",
                recordMessages = 0,
                supportedObservations = 0,
                importableObservations = 0,
                missingTimestamps = 0,
                outsideDisplayRange = 0,
                unsupportedMeasurementFields = 0,
                metadataRecords = 0,
                unknownFieldSignatures = 0,
                byMetric = emptyMap(),
                databaseMatchedKeys = if (databaseAvailable) 0 else null,
                databaseSourcePresent = if (databaseAvailable) false else null,
                fieldCoverage = emptyList(),
                fieldCoverageOmitted = 0
            )
        }
        for ((index, file) in files.withIndex()) {
            val alias = "FIT-${(index + 1).toString().padStart(4, '0')}"
            if (index >= MAX_FILES) {
                filesNotScanned = files.size - index
                break
            }
            if (Files.isSymbolicLink(file.toPath()) || file.length() !in 1..MAX_BYTES) {
                rejected(alias)
                continue
            }
            if (file.length() > MAX_TOTAL_BYTES - bytesRead) {
                filesNotScanned = files.size - index
                break
            }
            val bytes = file.readBytes()
            bytesRead += bytes.size
            if (file.name != CirqaFitBackfill.expectedFileName(bytes)) {
                rejected(alias)
                continue
            }
            val decoded = FitHistoricalObservations.decode(bytes)
            if (decoded == null) {
                rejected(alias)
                continue
            }
            verified++
            val sourcePresent = bytes.sha256Hex() in databaseSourceHashes
            if (sourcePresent) matchedSources++
            records += decoded.dataRecordCount
            metadata += decoded.deviceMetadata.size
            observations += decoded.observations.size
            decoded.observations.forEach { item ->
                counts[item.metric] = (counts[item.metric] ?: 0) + 1
                when (item.quality) {
                    FitHistoricalObservations.Quality.PROFILE_VALID ->
                        if (item.timestampEpochMillis != null) {
                            importable++
                            candidateKeys++
                            if (item.idempotencyKey in databaseSampleIds) matchedKeys++
                        } else missingTime++
                    FitHistoricalObservations.Quality.MISSING_TIMESTAMP -> missingTime++
                    FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE -> invalidRange++
                }
            }
            val fileImportable = decoded.observations.filter {
                it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                    it.timestampEpochMillis != null
            }
            val fileMatchedKeys = fileImportable.count { it.idempotencyKey in databaseSampleIds }
            val fileMetrics = decoded.observations.groupingBy { it.metric }.eachCount()
            val mappedFieldCoverage = decoded.observations.groupBy {
                Triple(it.globalMessageNumber, it.fieldNumber, it.metric)
            }.map { (key, items) ->
                "g${key.first}.f${key.second}:${key.third.name}:" +
                    "obs${items.size}/imp${items.count {
                        it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                            it.timestampEpochMillis != null
                    }}"
            }
            val unknownFieldCoverage = decoded.unknownFields.map { field ->
                "g${field.globalMessageNumber}.f${field.fieldNumber}:${field.kind.name}:" +
                    "b${field.baseType ?: "-"}s${field.size}di${field.developerDataIndex ?: "-"}:" +
                    "occ${field.occurrenceCount}defs${field.definitionCount}"
            }
            val allFieldCoverage = (mappedFieldCoverage + unknownFieldCoverage).sorted()
            fileSummaries += FileSummary(
                alias = alias,
                status = "verifiziert",
                recordMessages = decoded.dataRecordCount,
                supportedObservations = decoded.observations.size,
                importableObservations = fileImportable.size,
                missingTimestamps = decoded.observations.count {
                    it.quality == FitHistoricalObservations.Quality.MISSING_TIMESTAMP ||
                        it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                        it.timestampEpochMillis == null
                },
                outsideDisplayRange = decoded.observations.count {
                    it.quality == FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE
                },
                unsupportedMeasurementFields = decoded.unsupportedMeasurementFieldCount,
                metadataRecords = decoded.deviceMetadata.size,
                unknownFieldSignatures = decoded.unknownFields.size,
                byMetric = fileMetrics,
                databaseMatchedKeys = fileMatchedKeys.takeIf { databaseAvailable },
                databaseSourcePresent = sourcePresent.takeIf { databaseAvailable },
                fieldCoverage = allFieldCoverage.take(MAX_FIELD_SUMMARIES_PER_FILE),
                fieldCoverageOmitted = (allFieldCoverage.size - MAX_FIELD_SUMMARIES_PER_FILE)
                    .coerceAtLeast(0)
            )
        }
        return Summary(files.size, verified, rejected, records, observations, importable,
            missingTime, invalidRange, metadata, counts.toMap(),
            matchedKeys.takeIf { databaseAvailable },
            (candidateKeys - matchedKeys).takeIf { databaseAvailable },
            matchedSources.takeIf { databaseAvailable }, filesNotScanned, fileSummaries)
    }

    private fun ByteArray.sha256Hex(): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }
}

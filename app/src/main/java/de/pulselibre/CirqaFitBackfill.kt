package de.pulselibre

import java.io.File
import java.security.MessageDigest

/**
 * Offline, repeatable import of already-verified FIT originals.
 *
 * Each file is decoded and committed independently. A process interruption can
 * therefore be resumed by calling [run] again; the destination store's stable
 * (device, metric, timestamp, sample) key prevents duplicate observations.
 */
class CirqaFitBackfill(
    private val fitDirectory: File,
    private val readings: FitObservationStore,
    private val deviceKey: String = "cirqa-fit"
) {
    data class Result(
        val filesSeen: Int,
        val filesImported: Int,
        val invalidFiles: Int,
        val importableObservations: Int,
        /** Per-input summary uses ordinal aliases; no private filename or content hash is exposed. */
        val fileResults: List<FileResult> = emptyList()
    )

    enum class FileStatus { IMPORTED, NO_IMPORTABLE_OBSERVATIONS, INVALID_FILE }

    data class FileResult(
        val alias: String,
        val status: FileStatus,
        val decodedObservations: Int,
        val eligibleObservations: Int,
        val unsupportedMeasurementFields: Int,
        val missingTimestampObservations: Int,
        val outOfRangeObservations: Int
    )

    init {
        require(deviceKey.isNotBlank())
    }

    fun run(): Result {
        require(fitDirectory.isDirectory && !java.nio.file.Files.isSymbolicLink(fitDirectory.toPath())) {
            "FIT source directory must be a private, non-symlink directory"
        }
        val files = fitDirectory.listFiles()
            ?.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
            ?.sortedBy { it.name }
            ?: error("Cannot list FIT source directory")
        var imported = 0
        var invalid = 0
        var decoded = 0
        val fileResults = files.mapIndexed { index, file ->
            val outcome = importVerifiedFileDetailed(file, readings, deviceKey)
            if (outcome == null) {
                invalid++
                FileResult(
                    alias = "file-${(index + 1).toString().padStart(4, '0')}",
                    status = FileStatus.INVALID_FILE,
                    decodedObservations = 0,
                    eligibleObservations = 0,
                    unsupportedMeasurementFields = 0,
                    missingTimestampObservations = 0,
                    outOfRangeObservations = 0
                )
            } else {
                decoded += outcome.eligibleObservations
                imported++
                FileResult(
                    alias = "file-${(index + 1).toString().padStart(4, '0')}",
                    status = if (outcome.eligibleObservations > 0) FileStatus.IMPORTED
                        else FileStatus.NO_IMPORTABLE_OBSERVATIONS,
                    decodedObservations = outcome.decodedObservations,
                    eligibleObservations = outcome.eligibleObservations,
                    unsupportedMeasurementFields = outcome.unsupportedMeasurementFields,
                    missingTimestampObservations = outcome.missingTimestampObservations,
                    outOfRangeObservations = outcome.outOfRangeObservations
                )
            }
        }
        return Result(files.size, imported, invalid, decoded, fileResults)
    }

    companion object {
        private const val MAX_FIT_FILE_BYTES = 64L * 1024L * 1024L
        private data class ImportOutcome(
            val decodedObservations: Int,
            val eligibleObservations: Int,
            val unsupportedMeasurementFields: Int,
            val missingTimestampObservations: Int,
            val outOfRangeObservations: Int
        )

        /**
         * Imports one already-persisted content-addressed FIT file. Returns null
         * for invalid FIT bytes; SQLite exceptions are allowed to propagate.
         */
        fun importVerifiedFile(
            file: File,
            readings: FitObservationStore,
            deviceKey: String = "cirqa-fit"
        ): Int? = importVerifiedFileDetailed(file, readings, deviceKey)?.eligibleObservations

        private fun importVerifiedFileDetailed(
            file: File,
            readings: FitObservationStore,
            deviceKey: String
        ): ImportOutcome? {
            require(deviceKey.isNotBlank())
            if (!file.isFile || file.length() !in 1..MAX_FIT_FILE_BYTES ||
                java.nio.file.Files.isSymbolicLink(file.toPath())) return null
            val bytes = file.readBytes()
            if (file.name != expectedFileName(bytes)) return null
            val result = FitHistoricalObservations.decode(bytes) ?: return null
            // Only timestamped, profile-valid values are importable. Invalid
            // quality values remain in the immutable original, not SQLite.
            val importable = result.observations.filter {
                it.timestampEpochMillis != null &&
                    it.quality == FitHistoricalObservations.Quality.PROFILE_VALID
            }
            readings.commitFitFile(
                sourceSha256 = file.name.removeSuffix(".fit"),
                fileBytes = bytes.size.toLong(),
                relativePath = "verified_fit/${file.name}",
                validatedAtEpochMillis = System.currentTimeMillis(),
                decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                observations = importable,
                deviceMetadata = result.deviceMetadata,
                unmappedFields = result.unknownFields.map { field ->
                    FitFieldInventoryRecord(
                        sourceSha256 = field.sourceSha256,
                        globalMessageNumber = field.globalMessageNumber,
                        kind = if (field.kind == FitHistoricalObservations.UnknownKind.DEVELOPER_FIELD)
                            StoredFitFieldKind.DEVELOPER else StoredFitFieldKind.STANDARD,
                        fieldNumber = field.fieldNumber,
                        baseTypeCode = field.baseType,
                        fieldSize = field.size,
                        developerDataIndex = field.developerDataIndex,
                        classification = when (field.kind) {
                            FitHistoricalObservations.UnknownKind.UNKNOWN_MESSAGE -> "MESSAGE_NOT_DECODED_BY_APP"
                            FitHistoricalObservations.UnknownKind.UNKNOWN_STANDARD_FIELD -> "FIELD_NOT_DECODED_BY_APP"
                            FitHistoricalObservations.UnknownKind.DEVELOPER_FIELD -> "DEVELOPER_FIELD_NOT_DECODED_BY_APP"
                            FitHistoricalObservations.UnknownKind.INCOMPATIBLE_KNOWN_FIELD -> "KNOWN_MAPPING_INCOMPATIBLE"
                        },
                        definitionCount = field.definitionCount.toLong().coerceAtMost(1_000_000L),
                        observedMessageCount = field.occurrenceCount.toLong().coerceAtMost(1_000_000L),
                        validElementCount = null,
                        invalidElementCount = null
                    )
                },
                deviceKey = deviceKey
            )
            return ImportOutcome(
                decodedObservations = result.observations.size,
                eligibleObservations = importable.size,
                unsupportedMeasurementFields = result.unsupportedMeasurementFieldCount,
                missingTimestampObservations = result.observations.count {
                    it.quality == FitHistoricalObservations.Quality.MISSING_TIMESTAMP
                },
                outOfRangeObservations = result.observations.count {
                    it.quality == FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE
                }
            )
        }

        /** Content-addressed stores use this exact lowercase SHA-256 filename. */
        fun expectedFileName(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) } + ".fit"
    }
}

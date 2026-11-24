package de.pulselibre

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import android.database.sqlite.SQLiteException

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
        /** Profile-valid, timestamped candidates decoded, including failed DB commits. */
        val importableObservations: Int,
        /** Per-input summary uses ordinal aliases; no private filename or content hash is exposed. */
        val fileResults: List<FileResult> = emptyList(),
        /** True when safety limits or cancellation prevented a complete directory pass. */
        val truncated: Boolean = false,
        val failedFiles: Int = 0,
        /** Candidates reconciled by successful per-file commits; not newly inserted row count. */
        val committedObservations: Int = 0,
        /** Untrusted decoded values committed in successful per-file transactions. */
        val reviewCandidates: Int = 0
    )

    enum class FileStatus { IMPORTED, NO_IMPORTABLE_OBSERVATIONS, INVALID_FILE, FAILED }
    enum class FailureStage { READ, DATABASE, SOURCE_CONFLICT }

    data class FileResult(
        val alias: String,
        val status: FileStatus,
        val decodedObservations: Int,
        val eligibleObservations: Int,
        val unsupportedMeasurementFields: Int,
        val missingTimestampObservations: Int,
        val outOfRangeObservations: Int,
        val reviewCandidates: Int = 0,
        /** Failure category only; exception text and private paths are never returned. */
        val failureStage: FailureStage? = null
    )

    init {
        require(deviceKey.isNotBlank())
    }

    fun run(cancelled: () -> Boolean = { false }): Result {
        require(fitDirectory.isDirectory && !java.nio.file.Files.isSymbolicLink(fitDirectory.toPath())) {
            "FIT source directory must be a private, non-symlink directory"
        }
        val candidates = fitDirectory.listFiles()
            ?.filter { it.name.matches(CONTENT_ADDRESSED_FIT_NAME) }
            ?.sortedBy { it.name }
            ?: error("Cannot list FIT source directory")
        var truncated = candidates.size > MAX_FILES
        val files = candidates.take(MAX_FILES)
        var totalBytes = 0L
        var imported = 0
        var invalid = 0
        var failed = 0
        var importable = 0
        var committed = 0
        var persistedReviewCandidates = 0
        val fileResults = mutableListOf<FileResult>()
        for ((index, file) in files.withIndex()) {
            if (cancelled()) {
                truncated = true
                break
            }
            val bytes = file.length()
            if (!file.isFile || java.nio.file.Files.isSymbolicLink(file.toPath()) ||
                bytes !in 1..MAX_FIT_FILE_BYTES || bytes > MAX_TOTAL_BYTES - totalBytes
            ) {
                if (bytes > MAX_TOTAL_BYTES - totalBytes) truncated = true
                invalid++
                fileResults += FileResult(
                    alias = "file-${(index + 1).toString().padStart(4, '0')}",
                    status = FileStatus.INVALID_FILE,
                    decodedObservations = 0,
                    eligibleObservations = 0,
                    unsupportedMeasurementFields = 0,
                    missingTimestampObservations = 0,
                    outOfRangeObservations = 0
                )
                if (truncated) break
                continue
            }
            totalBytes += bytes
            var decodedBeforeFailure: ImportOutcome? = null
            val outcome = try {
                importVerifiedFileDetailed(file, readings, deviceKey) {
                    decodedBeforeFailure = it
                }
            } catch (_: IOException) {
                failed++
                fileResults += failedResult(index, FailureStage.READ)
                continue
            } catch (_: SQLiteException) {
                failed++
                importable += decodedBeforeFailure?.eligibleObservations ?: 0
                fileResults += failedResult(index, FailureStage.DATABASE, decodedBeforeFailure)
                continue
            } catch (_: IllegalArgumentException) {
                // require(...) detects a decoder/source contract violation before the
                // atomic commit. Treat it like any other per-file failure so that later
                // files and the unknown-field inventory scan are still processed.
                failed++
                importable += decodedBeforeFailure?.eligibleObservations ?: 0
                fileResults += failedResult(index, FailureStage.SOURCE_CONFLICT, decodedBeforeFailure)
                continue
            } catch (_: IllegalStateException) {
                // check(...)/error(...) in FitObservationCommitter deliberately detect a
                // conflict with already committed data. The transaction is rolled back;
                // the integrity check stays active, but the failure is isolated to this
                // input file instead of aborting the whole import run.
                failed++
                importable += decodedBeforeFailure?.eligibleObservations ?: 0
                fileResults += failedResult(index, FailureStage.SOURCE_CONFLICT, decodedBeforeFailure)
                continue
            }
            if (outcome == null) {
                invalid++
                fileResults += FileResult(
                    alias = "file-${(index + 1).toString().padStart(4, '0')}",
                    status = FileStatus.INVALID_FILE,
                    decodedObservations = 0,
                    eligibleObservations = 0,
                    unsupportedMeasurementFields = 0,
                    missingTimestampObservations = 0,
                    outOfRangeObservations = 0
                )
            } else {
                importable += outcome.eligibleObservations
                committed += outcome.eligibleObservations
                persistedReviewCandidates += outcome.reviewCandidates
                imported++
                fileResults += FileResult(
                    alias = "file-${(index + 1).toString().padStart(4, '0')}",
                    status = if (outcome.eligibleObservations > 0 || outcome.reviewCandidates > 0)
                        FileStatus.IMPORTED
                        else FileStatus.NO_IMPORTABLE_OBSERVATIONS,
                    decodedObservations = outcome.decodedObservations,
                    eligibleObservations = outcome.eligibleObservations,
                    unsupportedMeasurementFields = outcome.unsupportedMeasurementFields,
                    missingTimestampObservations = outcome.missingTimestampObservations,
                    outOfRangeObservations = outcome.outOfRangeObservations,
                    reviewCandidates = outcome.reviewCandidates
                )
            }
        }
        return Result(
            filesSeen = fileResults.size,
            filesImported = imported,
            invalidFiles = invalid,
            importableObservations = importable,
            fileResults = fileResults,
            truncated = truncated,
            failedFiles = failed,
            committedObservations = committed,
            reviewCandidates = persistedReviewCandidates
        )
    }

    private fun failedResult(
        index: Int,
        stage: FailureStage,
        outcome: ImportOutcome? = null
    ) = FileResult(
        alias = "file-${(index + 1).toString().padStart(4, '0')}",
        status = FileStatus.FAILED,
        decodedObservations = outcome?.decodedObservations ?: 0,
        eligibleObservations = outcome?.eligibleObservations ?: 0,
        unsupportedMeasurementFields = outcome?.unsupportedMeasurementFields ?: 0,
        missingTimestampObservations = outcome?.missingTimestampObservations ?: 0,
        outOfRangeObservations = outcome?.outOfRangeObservations ?: 0,
        reviewCandidates = outcome?.reviewCandidates ?: 0,
        failureStage = stage
    )

    companion object {
        const val MAX_FILES = 256
        const val MAX_TOTAL_BYTES = 64L * 1024L * 1024L
        const val MAX_FIT_FILE_BYTES = 8L * 1024L * 1024L
        private val CONTENT_ADDRESSED_FIT_NAME = Regex("^[0-9a-f]{64}\\.fit$")
        private data class ImportOutcome(
            val decodedObservations: Int,
            val eligibleObservations: Int,
            val reviewCandidates: Int,
            val unsupportedMeasurementFields: Int,
            val missingTimestampObservations: Int,
            val outOfRangeObservations: Int
        )

        /**
         * Imports one already-persisted content-addressed FIT file. Returns null
         * for invalid FIT bytes; storage exceptions are allowed to propagate.
         */
        fun importVerifiedFile(
            file: File,
            readings: FitObservationStore,
            deviceKey: String = "cirqa-fit",
            onBeforeSqliteCommit: () -> Unit = {},
            onAfterSqliteCommit: () -> Unit = {}
        ): Int? = importVerifiedFileDetailed(
            file, readings, deviceKey, onBeforeSqliteCommit, onAfterSqliteCommit
        )?.eligibleObservations

        /**
         * Commits a FIT decode already produced by the caller for these exact bytes.
         * The persisted content-addressed file is re-read and compared before the
         * supplied decode is trusted, but the expensive FIT parser is not run twice.
         */
        fun importDecodedVerifiedFile(
            file: File,
            verifiedBytes: ByteArray,
            decoded: FitHistoricalObservations.Result,
            readings: FitObservationStore,
            deviceKey: String = "cirqa-fit",
            onBeforeSqliteCommit: () -> Unit = {},
            onAfterSqliteCommit: () -> Unit = {}
        ): Int? = importDecodedVerifiedFileDetailed(
            file,
            verifiedBytes,
            decoded,
            readings,
            deviceKey,
            onBeforeSqliteCommit,
            onAfterSqliteCommit
        )?.eligibleObservations

        private fun importVerifiedFileDetailed(
            file: File,
            readings: FitObservationStore,
            deviceKey: String,
            onBeforeSqliteCommit: () -> Unit = {},
            onAfterSqliteCommit: () -> Unit = {},
            onDecoded: (ImportOutcome) -> Unit = {}
        ): ImportOutcome? {
            require(deviceKey.isNotBlank())
            if (!file.isFile || file.length() !in 1..MAX_FIT_FILE_BYTES ||
                java.nio.file.Files.isSymbolicLink(file.toPath())) return null
            val bytes = file.readBytes()
            if (file.name != expectedFileName(bytes)) return null
            val result = FitHistoricalObservations.decode(bytes) ?: return null
            return commitDecodedFile(
                file,
                bytes,
                result,
                readings,
                deviceKey,
                onBeforeSqliteCommit,
                onAfterSqliteCommit,
                onDecoded
            )
        }

        private fun importDecodedVerifiedFileDetailed(
            file: File,
            verifiedBytes: ByteArray,
            decoded: FitHistoricalObservations.Result,
            readings: FitObservationStore,
            deviceKey: String,
            onBeforeSqliteCommit: () -> Unit,
            onAfterSqliteCommit: () -> Unit
        ): ImportOutcome? {
            require(deviceKey.isNotBlank())
            if (!file.isFile || java.nio.file.Files.isSymbolicLink(file.toPath()) ||
                verifiedBytes.size !in 1..MAX_FIT_FILE_BYTES ||
                file.length() != verifiedBytes.size.toLong()
            ) return null
            if (file.name != expectedFileName(verifiedBytes) ||
                !file.readBytes().contentEquals(verifiedBytes) ||
                FitFileHeader.inspectVerified(verifiedBytes) == null
            ) return null
            val sourceSha256 = file.name.removeSuffix(".fit")
            if (decoded.observations.any { it.sourceFileSha256 != sourceSha256 } ||
                decoded.deviceMetadata.any { it.sourceFileSha256 != sourceSha256 } ||
                decoded.unknownFields.any { it.sourceSha256 != sourceSha256 } ||
                decoded.reviewCandidates.any { it.sourceFileSha256 != sourceSha256 }
            ) return null
            return commitDecodedFile(
                file,
                verifiedBytes,
                decoded,
                readings,
                deviceKey,
                onBeforeSqliteCommit,
                onAfterSqliteCommit
            )
        }

        private fun commitDecodedFile(
            file: File,
            bytes: ByteArray,
            result: FitHistoricalObservations.Result,
            readings: FitObservationStore,
            deviceKey: String,
            onBeforeSqliteCommit: () -> Unit,
            onAfterSqliteCommit: () -> Unit,
            onDecoded: (ImportOutcome) -> Unit = {}
        ): ImportOutcome {
            // Only timestamped, profile-valid values are importable. Invalid
            // quality values remain in the immutable original, not SQLite.
            val importable = result.observations.filter {
                it.timestampEpochMillis != null &&
                    it.quality == FitHistoricalObservations.Quality.PROFILE_VALID
            }
            val outcome = ImportOutcome(
                decodedObservations = result.observations.size,
                eligibleObservations = importable.size,
                reviewCandidates = result.reviewCandidates.size,
                unsupportedMeasurementFields = result.unsupportedMeasurementFieldCount,
                missingTimestampObservations = result.observations.count {
                    it.quality == FitHistoricalObservations.Quality.MISSING_TIMESTAMP
                },
                outOfRangeObservations = result.observations.count {
                    it.quality == FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE
                }
            )
            onDecoded(outcome)
            onBeforeSqliteCommit()
            try {
                readings.commitFitFile(
                    sourceSha256 = file.name.removeSuffix(".fit"),
                    fileBytes = bytes.size.toLong(),
                    relativePath = "verified_fit/${file.name}",
                    validatedAtEpochMillis = System.currentTimeMillis(),
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = importable,
                    deviceMetadata = result.deviceMetadata,
                    unmappedFields = result.unknownFields.map(CirqaUnknownFitInventory::toStoredField),
                    deviceKey = deviceKey,
                    reviewCandidates = result.reviewCandidates,
                    decodedFields = result.decodedFields
                )
            } finally {
                onAfterSqliteCommit()
            }
            return outcome
        }

        /** Content-addressed stores use this exact lowercase SHA-256 filename. */
        fun expectedFileName(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) } + ".fit"
    }
}

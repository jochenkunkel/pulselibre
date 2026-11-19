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
        val importableObservations: Int
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
        for (file in files) {
            val count = importVerifiedFile(file, readings, deviceKey)
            if (count == null) {
                invalid++
                continue
            }
            decoded += count
            imported++
        }
        return Result(files.size, imported, invalid, decoded)
    }

    companion object {
        private const val MAX_FIT_FILE_BYTES = 64L * 1024L * 1024L

        /**
         * Imports one already-persisted content-addressed FIT file. Returns null
         * for invalid FIT bytes; SQLite exceptions are allowed to propagate.
         */
        fun importVerifiedFile(
            file: File,
            readings: FitObservationStore,
            deviceKey: String = "cirqa-fit"
        ): Int? {
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
            return readings.commitFitFile(
                sourceSha256 = file.name.removeSuffix(".fit"),
                fileBytes = bytes.size.toLong(),
                relativePath = "verified_fit/${file.name}",
                validatedAtEpochMillis = System.currentTimeMillis(),
                decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                observations = importable,
                deviceMetadata = result.deviceMetadata,
                deviceKey = deviceKey
            )
        }

        /** Content-addressed stores use this exact lowercase SHA-256 filename. */
        fun expectedFileName(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) } + ".fit"
    }
}

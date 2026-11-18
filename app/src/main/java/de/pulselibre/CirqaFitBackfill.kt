package de.pulselibre

import java.io.File
import java.security.MessageDigest

/**
 * Offline, repeatable import of already-verified FIT originals.
 *
 * Each file is decoded and committed independently. A process interruption can
 * therefore be resumed by calling [run] again; LocalReadingStore's stable
 * (device, metric, timestamp, sample) key prevents duplicate observations.
 */
class CirqaFitBackfill(
    private val fitDirectory: File,
    private val readings: LocalReadingStore,
    private val deviceKey: String = "cirqa-fit"
) {
    data class Result(
        val filesSeen: Int,
        val filesImported: Int,
        val invalidFiles: Int,
        val observationsDecoded: Int
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
        /**
         * Imports one already-persisted content-addressed FIT file. Returns null
         * for invalid FIT bytes; SQLite exceptions are allowed to propagate.
         */
        fun importVerifiedFile(
            file: File,
            readings: LocalReadingStore,
            deviceKey: String = "cirqa-fit"
        ): Int? {
            require(deviceKey.isNotBlank())
            if (!file.isFile || java.nio.file.Files.isSymbolicLink(file.toPath())) return null
            val bytes = file.readBytes()
            if (file.name != expectedFileName(bytes)) return null
            val result = FitHistoricalObservations.decode(bytes) ?: return null
            // Only timestamped, profile-valid values are importable. Invalid
            // quality values remain available through the source FIT file.
            val batch = result.observations.asSequence()
                .filter {
                    it.timestampEpochMillis != null &&
                        it.quality == FitHistoricalObservations.Quality.PROFILE_VALID
                }
                .map {
                    RawExportRecord(
                        deviceId = deviceKey,
                        metric = it.metric.name.lowercase(),
                        measuredAtEpochMillis = requireNotNull(it.timestampEpochMillis),
                        value = it.value,
                        unit = it.unit,
                        sampleId = it.idempotencyKey
                    )
                }.toList()
            readings.commitBatch(deviceKey, batch)
            return batch.size
        }

        /** Content-addressed stores use this exact lowercase SHA-256 filename. */
        fun expectedFileName(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) } + ".fit"
    }
}

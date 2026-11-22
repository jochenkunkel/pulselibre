package de.pulselibre

import android.database.sqlite.SQLiteException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CirqaFitBackfillTest {
    private data class Commit(
        val sourceSha256: String,
        val fileBytes: Long,
        val relativePath: String,
        val validatedAtEpochMillis: Long,
        val decoderVersion: String,
        val profileVersion: String,
        val observationCount: Int,
        val deviceKey: String,
        val candidateCount: Int
    )

    private class RecordingStore : FitObservationStore {
        val calls = mutableListOf<List<FitHistoricalObservations.Observation>>()
        val candidateCalls = mutableListOf<List<FitHistoricalObservations.ReviewCandidate>>()
        val commits = mutableListOf<Commit>()
        override fun commitFitFile(
            sourceSha256: String,
            fileBytes: Long,
            relativePath: String,
            validatedAtEpochMillis: Long,
            decoderVersion: String,
            profileVersion: String,
            observations: List<FitHistoricalObservations.Observation>,
            deviceMetadata: List<FitHistoricalObservations.DeviceMetadata>,
            unmappedFields: List<FitFieldInventoryRecord>,
            deviceKey: String,
            reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>
        ): Int {
            calls += observations
            commits += Commit(
                sourceSha256 = sourceSha256,
                fileBytes = fileBytes,
                relativePath = relativePath,
                validatedAtEpochMillis = validatedAtEpochMillis,
                decoderVersion = decoderVersion,
                profileVersion = profileVersion,
                observationCount = observations.size,
                deviceKey = deviceKey,
                candidateCount = reviewCandidates.size
            )
            candidateCalls += reviewCandidates
            return observations.size
        }
    }

    private class FailOnceStore : FitObservationStore {
        var attempts = 0
        val committedKeys = mutableSetOf<String>()

        override fun commitFitFile(
            sourceSha256: String,
            fileBytes: Long,
            relativePath: String,
            validatedAtEpochMillis: Long,
            decoderVersion: String,
            profileVersion: String,
            observations: List<FitHistoricalObservations.Observation>,
            deviceMetadata: List<FitHistoricalObservations.DeviceMetadata>,
            unmappedFields: List<FitFieldInventoryRecord>,
            deviceKey: String,
            reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>
        ): Int {
            attempts++
            if (attempts == 1) throw SQLiteException("simulated commit failure")
            observations.forEach { committedKeys += it.idempotencyKey }
            return observations.size
        }
    }

    private fun fit(heartRate: Int = 72, withStress: Boolean = false): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + byteArrayOf(0x10, 0x27, 0, 0, heartRate.toByte()) +
            if (withStress) byteArrayOf(
                0x40, 0, 0, 0xe3.toByte(), 0, 1, 0, 2, 0x83.toByte(),
                0, 42, 0
            ) else byteArrayOf()
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14
        file[1] = 0x20
        file[2] = 0x64
        file[4] = data.size.toByte()
        file[5] = (data.size ushr 8).toByte()
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte()
        file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    @Test fun importsUntrustedCandidateWithUnknownTimeAndStableProvenance() {
        val root = Files.createTempDirectory("fit-backfill-untrusted-test").toFile()
        try {
            val bytes = fit(withStress = true)
            File(root, CirqaFitBackfill.expectedFileName(bytes)).writeBytes(bytes)
            val store = RecordingStore()

            val result = CirqaFitBackfill(root, store).run()
            val candidate = store.candidateCalls.single().single()

            assertEquals(1, result.reviewCandidates)
            assertEquals(1, result.fileResults.single().reviewCandidates)
            assertEquals("stress_level_value", candidate.profileFieldName)
            assertEquals(42.0, candidate.value, 0.0)
            assertEquals("profile_raw", candidate.unit)
            assertEquals(null, candidate.timestampEpochMillis)
            assertEquals(FitHistoricalObservations.TimestampOrigin.UNKNOWN, candidate.timestampOrigin)
            assertEquals("UNTRUSTED_PROFILE_CANDIDATE", candidate.quality)
            assertTrue(candidate.idempotencyKey.isNotBlank())
            assertEquals(1, store.commits.single().candidateCount)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun importsExistingVerifiedFilesOfflineAndReplayKeepsStableKeys() {
        val root = Files.createTempDirectory("fit-backfill-test").toFile()
        try {
            val bytes = fit()
            File(root, CirqaFitBackfill.expectedFileName(bytes)).writeBytes(bytes)
            val store = RecordingStore()
            val backfill = CirqaFitBackfill(root, store)

            val first = backfill.run()
            val second = backfill.run()

            assertEquals(1, first.filesSeen)
            assertEquals(1, first.filesImported)
            assertEquals(1, first.importableObservations)
            assertFalse(first.truncated)
            assertEquals(1, second.filesImported)
            assertEquals(
                store.calls[0].single().idempotencyKey,
                store.calls[1].single().idempotencyKey
            )
            assertEquals(
                store.calls[0].single().timestampEpochMillis,
                store.calls[1].single().timestampEpochMillis
            )
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun provenanceMetadataIsPassedUnchangedToTheStore() {
        val root = Files.createTempDirectory("fit-backfill-provenance-test").toFile()
        try {
            val bytes = fit()
            val file = File(root, CirqaFitBackfill.expectedFileName(bytes))
            file.writeBytes(bytes)
            val store = RecordingStore()

            CirqaFitBackfill(root, store).run()

            val commit = store.commits.single()
            assertEquals(file.name.removeSuffix(".fit"), commit.sourceSha256)
            assertEquals(bytes.size.toLong(), commit.fileBytes)
            assertEquals("verified_fit/${file.name}", commit.relativePath)
            assertEquals(FitHistoricalObservations.DECODER_VERSION, commit.decoderVersion)
            assertEquals(FitHistoricalObservations.PROFILE_VERSION, commit.profileVersion)
            assertEquals("cirqa-fit", commit.deviceKey)
            assertEquals(1, commit.observationCount)
            assertTrue(commit.validatedAtEpochMillis > 0)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun reportsCancellationAsIncompleteInsteadOfSuccess() {
        val root = Files.createTempDirectory("fit-backfill-cancel-test").toFile()
        try {
            val bytes = fit()
            File(root, CirqaFitBackfill.expectedFileName(bytes)).writeBytes(bytes)
            val result = CirqaFitBackfill(root, RecordingStore()).run { true }
            assertEquals(0, result.filesSeen)
            assertTrue(result.truncated)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun sqliteFailureIsReportedPerFileAndLaterFilesContinueThenRetryIdempotently() {
        val root = Files.createTempDirectory("fit-backfill-retry-test").toFile()
        try {
            val firstBytes = fit(heartRate = 72)
            val secondBytes = fit(heartRate = 73)
            File(root, CirqaFitBackfill.expectedFileName(firstBytes)).writeBytes(firstBytes)
            File(root, CirqaFitBackfill.expectedFileName(secondBytes)).writeBytes(secondBytes)
            val store = FailOnceStore()
            val backfill = CirqaFitBackfill(root, store)

            val first = backfill.run()
            val retry = backfill.run()

            assertEquals(2, first.filesSeen)
            assertEquals(1, first.filesImported)
            assertEquals(1, first.failedFiles)
            val failedFile = first.fileResults.single {
                it.status == CirqaFitBackfill.FileStatus.FAILED
            }
            assertEquals(CirqaFitBackfill.FailureStage.DATABASE, failedFile.failureStage)
            assertEquals(1, failedFile.decodedObservations)
            assertEquals(1, failedFile.eligibleObservations)
            assertEquals(1, first.fileResults.count {
                it.status == CirqaFitBackfill.FileStatus.IMPORTED
            })
            assertFalse(first.truncated)
            assertEquals(2, first.importableObservations)
            assertEquals(1, first.committedObservations)
            assertEquals(2, root.listFiles()?.count { it.name.endsWith(".fit") })

            assertEquals(2, retry.filesSeen)
            assertEquals(2, retry.filesImported)
            assertEquals(0, retry.failedFiles)
            assertEquals(2, retry.importableObservations)
            assertEquals(2, retry.committedObservations)
            assertEquals(2, store.committedKeys.size)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }
}

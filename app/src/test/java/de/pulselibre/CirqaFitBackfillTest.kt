package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CirqaFitBackfillTest {
    private class RecordingStore : FitObservationStore {
        val calls = mutableListOf<List<FitHistoricalObservations.Observation>>()
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
            deviceKey: String
        ): Int {
            calls += observations
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
            deviceKey: String
        ): Int {
            attempts++
            if (attempts == 1) throw IllegalStateException("simulated commit failure")
            observations.forEach { committedKeys += it.idempotencyKey }
            return observations.size
        }
    }

    private fun fit(): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + byteArrayOf(0x10, 0x27, 0, 0, 72)
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

    @Test fun failedCommitDoesNotReportFileImportedAndRetryReplaysStableKeys() {
        val root = Files.createTempDirectory("fit-backfill-retry-test").toFile()
        try {
            val bytes = fit()
            File(root, CirqaFitBackfill.expectedFileName(bytes)).writeBytes(bytes)
            val store = FailOnceStore()
            val backfill = CirqaFitBackfill(root, store)

            try {
                backfill.run()
                throw AssertionError("Expected the commit failure to propagate")
            } catch (expected: IllegalStateException) {
                assertEquals("simulated commit failure", expected.message)
            }

            val retry = backfill.run()

            assertEquals(2, store.attempts)
            assertEquals(1, retry.filesSeen)
            assertEquals(1, retry.filesImported)
            assertEquals(1, retry.importableObservations)
            assertEquals(1, store.committedKeys.size)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }
}

package de.pulselibre

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaUnknownFitInventoryTest {
    @Test fun boundedOfflineScanPersistsOnlyValidatedUnknownFieldMetadata() {
        val root = Files.createTempDirectory("unknown-fit-persist-").toFile()
        try {
            val bytes = fitWithUnknownField()
            val name = CirqaFitBackfill.expectedFileName(bytes)
            File(root, name).writeBytes(bytes)
            val store = RecordingFitStore()

            val (scan, persisted) = CirqaUnknownFitInventory.scanAndPersist(
                root = root,
                store = store,
                validatedAtEpochMillis = 1234
            )

            assertEquals(1, scan.filesChecked)
            assertEquals(0, scan.filesRejected)
            assertFalse(scan.truncated)
            assertEquals(1, scan.fields.size)
            assertEquals(1, persisted)
            assertEquals(1, store.sources.size)
            val committed = store.sources.single()
            assertEquals(name.removeSuffix(".fit"), committed.sourceSha256)
            assertEquals(bytes.size.toLong(), committed.fileBytes)
            assertEquals("verified_fit/$name", committed.relativePath)
            assertEquals(1234L, committed.validatedAtEpochMillis)
            assertTrue(committed.observations.isEmpty())
            val field = committed.unmappedFields.single()
            assertEquals(20, field.globalMessageNumber)
            assertEquals(85, field.fieldNumber)
            assertEquals(0x84, field.baseTypeCode)
            assertEquals("FIELD_NOT_DECODED_BY_APP", field.classification)
            assertEquals(1L, field.definitionCount)
            assertEquals(1L, field.observedMessageCount)
            assertEquals(FitFieldInventoryStatus.UNREVIEWED, field.status)
            assertFalse(field.javaClass.declaredFields.any {
                it.name.contains("value", ignoreCase = true) || it.name.contains("payload", ignoreCase = true)
            })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancelledOrInvalidScanDoesNotPersistAnything() {
        val root = Files.createTempDirectory("unknown-fit-persist-cancel-").toFile()
        try {
            val bytes = fitWithUnknownField()
            val file = File(root, CirqaFitBackfill.expectedFileName(bytes))
            file.writeBytes(bytes)
            val store = RecordingFitStore()

            val cancelled = CirqaUnknownFitInventory.scanAndPersist(
                root, store, validatedAtEpochMillis = 1234, cancelled = { true }
            )
            assertTrue(cancelled.first.truncated)
            assertEquals(0, cancelled.first.filesChecked)
            assertEquals(0, cancelled.second)
            assertTrue(store.sources.isEmpty())

            file.writeBytes(bytes.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
            val invalid = CirqaUnknownFitInventory.scanAndPersist(root, store, validatedAtEpochMillis = 1234)
            assertEquals(1, invalid.first.filesRejected)
            assertEquals(0, invalid.second)
            assertTrue(store.sources.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    private data class Commit(
        val sourceSha256: String,
        val fileBytes: Long,
        val relativePath: String,
        val validatedAtEpochMillis: Long,
        val observations: List<FitHistoricalObservations.Observation>,
        val unmappedFields: List<FitFieldInventoryRecord>
    )

    private class RecordingFitStore : FitObservationStore {
        val sources = mutableListOf<Commit>()

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
            reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>,
            decodedFields: List<FitHistoricalObservations.DecodedField>
        ): Int {
            sources += Commit(
                sourceSha256, fileBytes, relativePath, validatedAtEpochMillis,
                observations.toList(), unmappedFields.toList()
            )
            return 0
        }
    }

    private fun fitWithUnknownField(): ByteArray {
        fun u32(value: Int) = byteArrayOf(
            value.toByte(), (value ushr 8).toByte(),
            (value ushr 16).toByte(), (value ushr 24).toByte()
        )
        fun u16(value: Int) = byteArrayOf(value.toByte(), (value ushr 8).toByte())
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 3,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            85.toByte(), 2, 0x84.toByte(),
            0
        ) + u32(100_000) + byteArrayOf(72) + u16(123)
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
        file[file.lastIndex] = (crc ushr 8).toByte()
        return file
    }
}

package de.pulselibre

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalReadingStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun fitWithHeartRate(timestamp: Int, heartRate: Int): ByteArray {
        fun u32(value: Int) = byteArrayOf(
            value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte()
        )
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + u32(timestamp) + byteArrayOf(heartRate.toByte())
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

    @Test fun offlineFitBackfillIsRepeatableAndRejectsInvalidFiles() {
        context.deleteDatabase("pulselibre_readings.db")
        val root = File(context.cacheDir, "fit-backfill-test-${System.nanoTime()}")
        assertTrue(root.mkdir())
        try {
            val bytes = fitWithHeartRate(100_000, 72)
            val valid = File(root, CirqaFitBackfill.expectedFileName(bytes))
            valid.writeBytes(bytes)
            File(root, "not-a-fit.bin").writeBytes(byteArrayOf(1, 2, 3))

            LocalReadingStore(context).use { store ->
                val importer = CirqaFitBackfill(root, store)
                val first = importer.run()
                val repeated = importer.run()
                assertEquals(2, first.filesSeen)
                assertEquals(1, first.filesImported)
                assertEquals(1, first.invalidFiles)
                assertEquals(1, first.importableObservations)
                assertEquals(first, repeated)
                val records = store.readWindow(ExportWindow(0, Long.MAX_VALUE))
                assertEquals(1, records.size)
                assertEquals(72.0, records.single().value, 0.0)
                assertEquals((100_000L + 631_065_600L) * 1_000L,
                    store.cursorFor("cirqa-fit").lastCommittedEpochMillis)
            }
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
            context.deleteDatabase("pulselibre_readings.db")
        }
    }

    @Test fun batchIsDurableIdempotentAndCursorMonotonic() {
        context.deleteDatabase("pulselibre_readings.db")
        val reading = RawExportRecord("test-device", "spo2", 2000, 97.0, "%", "a", 3000)
        LocalReadingStore(context).use { store ->
            store.commitBatch("test-device", listOf(reading))
            store.commitBatch("test-device", listOf(reading))
            assertEquals(1, store.readWindow(ExportWindow(0, 3000)).size)
            assertEquals(2000L, store.cursorFor("test-device").lastCommittedEpochMillis)
        }
        LocalReadingStore(context).use { store ->
            assertEquals(1, store.readWindow(ExportWindow(0, 3000)).size)
            store.commitBatch("test-device", listOf(reading.copy(measuredAtEpochMillis = 1000, sampleId = "b")))
            assertEquals(2000L, store.cursorFor("test-device").lastCommittedEpochMillis)
            assertEquals(2, store.readWindow(ExportWindow(0, 3000)).size)
            assertEquals(0, store.readWindow(ExportWindow(3000, 4000)).size)
        }
    }

    @Test fun invalidBatchDoesNotWriteOrAdvanceCursor() {
        context.deleteDatabase("pulselibre_readings.db")
        LocalReadingStore(context).use { store ->
            try {
                store.commitBatch("test-device", listOf(
                    RawExportRecord("test-device", "hr", 100, 75.0, "bpm"),
                    RawExportRecord("wrong-device", "hr", 200, 80.0, "bpm")
                ))
                fail("Expected validation failure")
            } catch (_: IllegalArgumentException) { }
            assertTrue(store.readWindow(ExportWindow(0, 300)).isEmpty())
            assertNull(store.cursorFor("test-device").lastCommittedEpochMillis)
        }
    }
}

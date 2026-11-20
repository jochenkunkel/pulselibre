package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
            val outOfRangeBytes = fitWithHeartRate(101_000, 250)
            File(root, CirqaFitBackfill.expectedFileName(outOfRangeBytes)).writeBytes(outOfRangeBytes)
            val invalidBytes = byteArrayOf(1, 2, 3)
            File(root, CirqaFitBackfill.expectedFileName(invalidBytes)).writeBytes(invalidBytes)

            LocalReadingStore(context).use { store ->
                val importer = CirqaFitBackfill(root, store)
                val first = importer.run()
                val repeated = importer.run()
                assertEquals(3, first.filesSeen)
                assertEquals(2, first.filesImported)
                assertEquals(1, first.invalidFiles)
                assertEquals(1, first.importableObservations)
                assertEquals(3, first.fileResults.size)
                assertEquals(
                    setOf("file-0001", "file-0002", "file-0003"),
                    first.fileResults.map { it.alias }.toSet()
                )
                assertEquals(1, first.fileResults.count {
                    it.status == CirqaFitBackfill.FileStatus.IMPORTED &&
                        it.decodedObservations == 1 && it.eligibleObservations == 1
                })
                assertEquals(1, first.fileResults.count {
                    it.status == CirqaFitBackfill.FileStatus.INVALID_FILE
                })
                assertEquals(1, first.fileResults.count {
                    it.status == CirqaFitBackfill.FileStatus.NO_IMPORTABLE_OBSERVATIONS &&
                        it.decodedObservations == 1 && it.eligibleObservations == 0 &&
                        it.outOfRangeObservations == 1 &&
                        it.unsupportedMeasurementFields == 0
                })
                assertEquals(first, repeated)
                val originalObservation = FitHistoricalObservations.decode(bytes)!!.observations.single()
                val conflictingBatch = listOf(
                    originalObservation.copy(
                        timestampEpochMillis = originalObservation.timestampEpochMillis!! + 1_000,
                        recordOrdinal = 2,
                        idempotencyKey = "pending-before-conflict"
                    ),
                    originalObservation.copy(value = 73.0)
                )
                assertThrows(IllegalStateException::class.java) {
                    store.commitFitFile(
                        sourceSha256 = valid.name.removeSuffix(".fit"),
                        fileBytes = bytes.size.toLong(),
                        relativePath = "verified_fit/${valid.name}",
                        validatedAtEpochMillis = System.currentTimeMillis(),
                        decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                        profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                        observations = conflictingBatch
                    )
                }
                val records = store.readWindow(ExportWindow(0, Long.MAX_VALUE))
                assertEquals(1, records.size)
                assertEquals(72.0, records.single().value, 0.0)
                assertEquals((100_000L + 631_065_600L) * 1_000L,
                    store.cursorFor("cirqa-fit").lastCommittedEpochMillis)
                val db = store.readableDatabase
                assertEquals(2, db.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_observation_provenance", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var violations = 0
                    while (it.moveToNext()) violations++
                    violations
                })
            }
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
            context.deleteDatabase("pulselibre_readings.db")
        }
    }

    @Test fun schemaV1UpgradePreservesRowsAndAddsProvenanceTables() {
        context.deleteDatabase("pulselibre_readings.db")
        val path = context.getDatabasePath("pulselibre_readings.db")
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("""CREATE TABLE readings (
                device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL,
                value REAL NOT NULL, unit TEXT NOT NULL, sample_id TEXT NOT NULL DEFAULT '',
                received_at_ms INTEGER,
                PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
            )""")
            db.execSQL("""CREATE TABLE sync_cursors (
                device_id TEXT PRIMARY KEY NOT NULL, last_committed_ms INTEGER NOT NULL
            )""")
            db.execSQL("CREATE INDEX readings_by_time ON readings(measured_at_ms)")
            db.execSQL(
                "INSERT INTO readings VALUES('legacy-device','heart_rate',1234,77,'bpm','old-sample',2345)"
            )
            db.execSQL("INSERT INTO sync_cursors VALUES('legacy-device',1234)")
            db.version = 1
        }

        LocalReadingStore(context).use { store ->
            assertEquals(1, store.readWindow(ExportWindow(0, 2000)).size)
            assertEquals("old-sample", store.readWindow(ExportWindow(0, 2000)).single().sampleId)
            assertEquals(1234L, store.cursorFor("legacy-device").lastCommittedEpochMillis)
            assertEquals(0, store.readableDatabase.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                it.moveToFirst(); it.getInt(0)
            })
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

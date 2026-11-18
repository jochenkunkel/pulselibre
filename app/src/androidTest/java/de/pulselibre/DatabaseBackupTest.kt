package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class DatabaseBackupTest {
    @Test fun backupResetAndRestoreRoundTripBothLocalStoresAndCursor() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"reading","received_at_utc":"2026-11-17T08:00:00Z","service":6,"bpm":71}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            val historical = RawExportRecord("test-device", "spo2", 2_000, 97.0, "%", "sample", 3_000)
            val sourceSha = "a".repeat(64)
            val fitObservation = FitHistoricalObservations.Observation(
                metric = FitHistoricalObservations.Metric.HEART_RATE,
                timestampEpochMillis = 4_000,
                value = 65.0,
                unit = "bpm",
                sourceFileSha256 = sourceSha,
                recordOrdinal = 1,
                globalMessageNumber = 20,
                fieldNumber = 3,
                quality = FitHistoricalObservations.Quality.PROFILE_VALID,
                idempotencyKey = "fit-sample",
                duplicateFingerprint = "semantic-fingerprint"
            )
            LocalReadingStore(context).use {
                it.commitBatch("test-device", listOf(historical))
                it.commitFitFile(
                    sourceSha256 = sourceSha,
                    fileBytes = 100,
                    relativePath = "verified_fit/$sourceSha.fit",
                    validatedAtEpochMillis = 5_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(fitObservation)
                )
            }

            val bytes = ByteArrayOutputStream()
            val report = DatabaseBackup.write(context, bytes)
            assertEquals(1L, report.events)
            assertEquals(2L, report.legacyReadings)
            assertEquals(2L, report.syncCursors)
            assertEquals(1L, report.fitSources)
            assertEquals(1L, report.fitProvenance)

            DatabaseBackup.reset(context)
            CirqaDatabase(context).use { assertEquals(0L, it.eventCount()) }
            LocalReadingStore(context).use {
                assertTrue(it.readWindow(ExportWindow(0, Long.MAX_VALUE)).isEmpty())
                assertNull(it.cursorFor("test-device").lastCommittedEpochMillis)
            }

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(bytes.toByteArray()))
            try {
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            CirqaDatabase(context).use { db ->
                assertEquals(1L, db.eventCount())
                assertEquals(listOf(event), db.allLines())
            }
            LocalReadingStore(context).use {
                val restored = it.readWindow(ExportWindow(0, Long.MAX_VALUE))
                assertEquals(2, restored.size)
                assertTrue(restored.contains(historical))
                assertTrue(restored.any { row -> row.deviceId == "cirqa-fit" && row.value == 65.0 })
                assertEquals(2_000L, it.cursorFor("test-device").lastCommittedEpochMillis)
                assertEquals(4_000L, it.cursorFor("cirqa-fit").lastCommittedEpochMillis)
                val db = it.readableDatabase
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_sources WHERE source_sha256 = ?", arrayOf(sourceSha)).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_observation_provenance WHERE sample_id = 'fit-sample'", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var violations = 0
                    while (it.moveToNext()) violations++
                    violations
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun version1BackupRemainsRestorableAfterProvenanceSchemaUpgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-v1-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val sha = "b".repeat(64)
            LocalReadingStore(context).use {
                it.commitFitFile(
                    sourceSha256 = sha,
                    fileBytes = 80,
                    relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 6_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(FitHistoricalObservations.Observation(
                        metric = FitHistoricalObservations.Metric.HEART_RATE,
                        timestampEpochMillis = 7_000,
                        value = 70.0,
                        unit = "bpm",
                        sourceFileSha256 = sha,
                        recordOrdinal = 1,
                        globalMessageNumber = 20,
                        fieldNumber = 3,
                        quality = FitHistoricalObservations.Quality.PROFILE_VALID,
                        idempotencyKey = "old-fit-sample",
                        duplicateFingerprint = "old-fingerprint"
                    ))
                )
            }

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(emptyVersion1Backup()))
            try {
                assertEquals(1, plan.version)
                assertEquals(0L, plan.fitSources)
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            LocalReadingStore(context).use {
                assertTrue(it.readWindow(ExportWindow(0, Long.MAX_VALUE)).isEmpty())
                assertEquals(0, it.readableDatabase.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(0, it.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM fit_observation_provenance", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun orphanedFitProvenanceIsRejectedBeforeRestoreConfirmation() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-fk-test-").toFile()
        val context = isolatedContext(base, root)
        val original = RawExportRecord("keep-device", "heart_rate", 123, 70.0, "bpm", "keep")
        try {
            LocalReadingStore(context).use { it.commitBatch("keep-device", listOf(original)) }
            try {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream(version2ArchiveWithOrphan()))
                fail("Expected orphaned source reference to be rejected")
            } catch (_: Exception) { }
            LocalReadingStore(context).use {
                assertEquals(listOf(original), it.readWindow(ExportWindow(0, 1000)))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun emptyVersion1Backup(): ByteArray {
        val dataEntries = listOf("cirqa_events.jsonl", "legacy_readings.jsonl", "sync_cursors.jsonl")
        val emptySha = MessageDigest.getInstance("SHA-256").digest(byteArrayOf())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val entries = JSONObject()
        dataEntries.forEach { name ->
            entries.put(name, JSONObject().put("records", 0).put("bytes", 0).put("sha256", emptySha))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", 1)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", entries)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                (dataEntries + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    if (name == "manifest.json") zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun version2ArchiveWithOrphan(): ByteArray {
        val payloads = linkedMapOf(
            "cirqa_events.jsonl" to "",
            "legacy_readings.jsonl" to "",
            "sync_cursors.jsonl" to "",
            "fit_sources.jsonl" to "",
            "fit_observation_provenance.jsonl" to
                """{"sample_id":"orphan","device_id":"cirqa-fit","metric":"heart_rate","measured_at_ms":5000,"source_sha256":"${"c".repeat(64)}","record_ordinal":1,"global_message_number":20,"field_number":3,"quality":"PROFILE_VALID","decoder_version":"1"}""" + "\n"
        )
        val metadata = JSONObject()
        payloads.forEach { (name, value) ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            metadata.put(name, JSONObject()
                .put("records", if (value.isEmpty()) 0 else 1)
                .put("bytes", bytes.size)
                .put("sha256", MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it.toInt() and 255) }))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", 2)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", metadata)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                payloads.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
    }

    @Test fun invalidArchiveIsRejectedBeforeAnyStoredDataChanges() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-invalid-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-17T08:00:00Z"}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            assertThrows(IllegalArgumentException::class.java) {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream("not a zip".toByteArray()))
            }
            CirqaDatabase(context).use { assertEquals(listOf(event), it.allLines()) }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}

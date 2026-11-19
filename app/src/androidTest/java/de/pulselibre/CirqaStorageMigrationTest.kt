package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CirqaStorageMigrationTest {
    @Test fun canonicalSchemaV3UpgradeAddsMetadataWithoutChangingExistingFitSources() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "cirqa-metadata-upgrade-").toFile()
        val context = isolatedContext(base, root)
        val sha = "a".repeat(64)
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("cirqa_measurements.db"), null).use { db ->
                db.execSQL("CREATE TABLE fit_sources(source_sha256 TEXT PRIMARY KEY NOT NULL)")
                db.execSQL("INSERT INTO fit_sources(source_sha256) VALUES(?)", arrayOf(sha))
                db.version = 3
            }
            CirqaDatabase(context).use { database ->
                val db = database.writableDatabase
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_sources WHERE source_sha256 = ?", arrayOf(sha))
                    .use { it.moveToFirst(); it.getInt(0) })
                db.execSQL(
                    """INSERT INTO fit_device_metadata
                       (source_sha256, record_ordinal, observed_at_ms, software_version)
                       VALUES(?, 1, 1234, 12.34)""",
                    arrayOf(sha)
                )
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_device_metadata", null)
                    .use { it.moveToFirst(); it.getInt(0) })
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

    @Test fun canonicalSchemaV4UpgradeAddsRawMetadataIndexWithoutChangingExistingRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "cirqa-schema-v5-upgrade-").toFile()
        val context = isolatedContext(base, root)
        val event = """{"schema":1,"type":"reading","received_at_utc":"2026-11-18T08:00:00Z","service":6,"bpm":70}"""
        try {
            CirqaDatabase(context).use { database ->
                assertTrue(database.insertLine(event))
                val db = database.writableDatabase
                db.execSQL("DROP TABLE raw_device_metadata")
                db.version = 4
            }
            CirqaDatabase(context).use { database ->
                assertEquals(listOf(event), database.allLines())
                assertTrue(database.rawDeviceMetadata().isEmpty())
                assertEquals(5, database.writableDatabase.version)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun canonicalSchemaV2UpgradePreservesEventsAndAddsNormalizedTables() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "cirqa-schema-upgrade-").toFile()
        val context = isolatedContext(base, root)
        val event = """{"schema":1,"type":"reading","received_at_utc":"2026-11-17T08:00:00Z","service":6,"bpm":71}"""
        try {
            val path = context.getDatabasePath("cirqa_measurements.db")
            SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
                db.execSQL("""CREATE TABLE events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, line_sha256 TEXT NOT NULL UNIQUE,
                    received_at_utc TEXT NOT NULL, dashboard_time TEXT, kind TEXT NOT NULL,
                    service INTEGER, payload_json TEXT NOT NULL
                )""")
                db.execSQL("CREATE INDEX events_service_time ON events(service, received_at_utc)")
                db.execSQL("CREATE INDEX events_dashboard_time ON events(service, dashboard_time DESC, id ASC) WHERE kind = 'reading'")
                db.execSQL("CREATE INDEX events_kind_time ON events(kind, received_at_utc)")
                db.execSQL(
                    "INSERT INTO events(line_sha256, received_at_utc, dashboard_time, kind, service, payload_json) VALUES('hash', '2026-11-17T08:00:00Z', '2026-11-17T08:00:00Z', 'reading', 6, ?)",
                    arrayOf(event)
                )
                db.version = 2
            }
            CirqaDatabase(context).use { database ->
                assertEquals(listOf(event), database.readingLines())
                assertEquals(0, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations", null
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(0, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM storage_migrations", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun conflictingCanonicalKeyRollsBackAndKeepsHistoricalSource() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "cirqa-storage-conflict-").toFile()
        val context = isolatedContext(base, root)
        val sourceRow = RawExportRecord("device", "heart_rate", 5_000, 65.0, "bpm", "same-key")
        val conflictingRow = sourceRow.copy(value = 66.0)
        try {
            LocalReadingStore(context).use { it.commitBatch("device", listOf(sourceRow)) }
            CirqaDatabase(context).use { database ->
                database.writableDatabase.execSQL(
                    """INSERT INTO observations
                       (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                       VALUES(?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf<Any?>(
                        conflictingRow.deviceId, conflictingRow.metric, conflictingRow.measuredAtEpochMillis,
                        conflictingRow.value, conflictingRow.unit, conflictingRow.sampleId,
                        conflictingRow.receivedAtEpochMillis
                    )
                )
                try {
                    database.ensureHistoricalStoreCopied()
                    throw AssertionError("Expected conflicting row to fail closed")
                } catch (_: IllegalStateException) { }
                assertEquals(1, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations WHERE sample_id = 'same-key'", null
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(66.0, database.readableDatabase.rawQuery(
                    "SELECT value FROM observations WHERE sample_id = 'same-key'", null
                ).use { it.moveToFirst(); it.getDouble(0) }, 0.0)
            }
            LocalReadingStore(context).use {
                assertEquals(listOf(sourceRow), it.readWindow(ExportWindow(0, 10_000)))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun historicalStoreIsCopiedAndVerifiedWithoutDeletingSourceRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "cirqa-storage-migration-").toFile()
        val context = isolatedContext(base, root)
        val sha = "d".repeat(64)
        val reading = RawExportRecord(
            deviceId = "cirqa-fit",
            metric = "heart_rate",
            measuredAtEpochMillis = 8_000,
            value = 68.0,
            unit = "bpm",
            sampleId = "migration-sample",
            receivedAtEpochMillis = 9_000
        )
        try {
            LocalReadingStore(context).use { store ->
                store.commitBatch("cirqa-fit", listOf(reading))
                store.writableDatabase.execSQL(
                    """INSERT INTO fit_sources
                       (source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version)
                       VALUES(?, 128, ?, 9000, '1', '21.222.0')""",
                    arrayOf(sha, "verified_fit/$sha.fit")
                )
                store.writableDatabase.execSQL(
                    """INSERT INTO fit_observation_provenance
                       (sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                        global_message_number, field_number, quality, decoder_version)
                       VALUES('migration-sample', 'cirqa-fit', 'heart_rate', 8000, ?, 1, 20, 3,
                              'PROFILE_VALID', '1')""",
                    arrayOf(sha)
                )
            }

            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                val db = database.readableDatabase
                assertEquals(1, db.rawQuery(
                    "SELECT COUNT(*) FROM observations WHERE device_id = 'cirqa-fit' AND sample_id = 'migration-sample'",
                    null
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(1, db.rawQuery(
                    "SELECT COUNT(*) FROM fit_sources WHERE source_sha256 = ?", arrayOf(sha)
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(1, db.rawQuery(
                    "SELECT COUNT(*) FROM fit_observation_provenance WHERE sample_id = 'migration-sample'", null
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var violations = 0
                    while (it.moveToNext()) violations++
                    violations
                })
            }

            // Reopening retries the copy and must not duplicate or mutate rows.
            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                assertEquals(1, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
            val laterReading = reading.copy(
                metric = "spo2",
                measuredAtEpochMillis = 8_500,
                value = 97.0,
                unit = "%",
                sampleId = "later-sample"
            )
            LocalReadingStore(context).use { it.commitBatch("cirqa-fit", listOf(laterReading)) }
            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                assertEquals(2, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
            LocalReadingStore(context).use { store ->
                assertEquals(listOf(reading, laterReading), store.readWindow(ExportWindow(0, 10_000)))
                assertEquals(1, store.readableDatabase.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
            }
            assertTrue(context.getDatabasePath("pulselibre_readings.db").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File = File(root, name).also { it.parentFile?.mkdirs() }
        override fun getFilesDir(): File = File(root, "files").also { it.mkdirs() }
        override fun openOrCreateDatabase(
            name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?
        ): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}

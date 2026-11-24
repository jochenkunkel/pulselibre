package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/**
 * #144: real Android SQLite proof that the additive v8 -> v9 migration is lossless,
 * idempotent and keeps status codes out of the physiological observation series.
 *
 * Synthetic schema/values only: no device, no BLE, no raw FIT bytes, no personal data.
 * This runs on the emulator / CI only, never on the S25.
 */
@RunWith(AndroidJUnit4::class)
class CirqaDataModelV9InstrumentedTest {

    private val sourceSha = "a".repeat(64)
    private val generationId = "b".repeat(64)
    private val digest = "c".repeat(64)
    private val measuredAt = 1_700_000_000_000L
    private val generatedAt = 1_700_000_001_000L

    @Test fun v8UpgradeBackfillsQualityWithoutTouchingStoredValuesAndIsIdempotent() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "issue144-v8-upgrade-").toFile()
        val context = isolatedContext(base, root)
        try {
            createV8Database(context)

            CirqaDatabase(context).use { database ->
                val db = database.readableDatabase
                assertEquals(11, db.version)

                // Catalogues are additive and carry documented provenance only.
                assertEquals(
                    setOf("UNKNOWN", "LEGACY_PROFILE_VALID"),
                    db.querySet("SELECT status_code FROM quality_status_catalog")
                )
                assertEquals(setOf("UNKNOWN"), db.querySet("SELECT mode_code FROM measurement_modes"))

                // One quality row per stored observation, faithful legacy classification.
                assertEquals(2, count(db, "observations"))
                assertEquals(2, count(db, "observation_quality"))
                assertEquals(
                    setOf("LEGACY_PROFILE_VALID"),
                    db.querySet("SELECT DISTINCT quality_status FROM observation_quality")
                )
                assertEquals(setOf("UNKNOWN"), db.querySet("SELECT DISTINCT measurement_mode FROM observation_quality"))

                // Timestamp origin is preserved per metric; no invented time is added.
                assertEquals(
                    "DIRECT",
                    db.querySet("SELECT timestamp_origin FROM observation_quality WHERE metric = 'heart_rate'").single()
                )
                assertEquals(
                    "RECONSTRUCTED_TS16",
                    db.querySet("SELECT timestamp_origin FROM observation_quality WHERE metric = 'spo2'").single()
                )
                assertEquals(
                    generatedAt,
                    db.rawQuery("SELECT stored_at_ms FROM observation_quality WHERE metric = 'heart_rate'", null).use {
                        assertTrue(it.moveToFirst())
                        it.getLong(0)
                    }
                )

                // Original physiological values are unchanged.
                assertEquals(72.0, db.value("heart_rate"), 0.0)
                assertEquals(98.0, db.value("spo2"), 0.0)
                assertEquals("ok", db.integrity())
                assertTrue(db.foreignKeyViolations() == 0)
            }

            // Reopening must not duplicate the deterministic projection.
            CirqaDatabase(context).use { database ->
                val db = database.readableDatabase
                assertEquals(11, db.version)
                assertEquals(2, count(db, "observation_quality"))
                assertEquals(2, count(db, "observations"))
                assertEquals("ok", db.integrity())
                assertTrue(db.foreignKeyViolations() == 0)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun statusCodesAreEventsAndNeverBecomePhysiologicalValues() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "issue144-status-events-").toFile()
        val context = isolatedContext(base, root)
        try {
            createV8Database(context)

            CirqaDatabase(context).use { database ->
                val db = database.writableDatabase
                val codes = listOf(
                    Triple("status-rr-100", "respiration", -100),
                    Triple("status-rr-200", "respiration", -200),
                    Triple("status-rr-300", "respiration", -300),
                    Triple("status-hr-0", "heart_rate", 0),
                    Triple("status-spo2-255", "spo2", 255)
                )
                codes.forEach { (key, metric, raw) ->
                    db.execSQL(
                        """INSERT INTO observation_status_events
                           (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                            raw_code, status_code, measurement_mode, source_kind, source_sha256,
                            decoder_version, profile_version, stored_at_ms)
                           VALUES (?, 'device', ?, ?, 'DIRECT', ?, 'UNKNOWN', 'UNKNOWN', 'FIT', ?, '1', ?, ?)""",
                        arrayOf<Any>(key, metric, measuredAt, raw, sourceSha, "21.222.0", generatedAt)
                    )
                }
                // An unknown FIT message has no fabricated time: raw text lives only as metadata.
                db.execSQL(
                    """INSERT INTO observation_status_events
                       (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                        raw_value_text, status_code, measurement_mode, source_kind,
                        decoder_version, profile_version, stored_at_ms)
                       VALUES ('unknown-message', 'device', 'unknown', NULL, 'UNKNOWN',
                               'opaque', 'UNKNOWN', 'UNKNOWN', 'FIT', '1', '21.222.0', ?)""",
                    arrayOf(generatedAt)
                )

                assertEquals(6, count(db, "observation_status_events"))
                assertEquals(2, count(db, "observations"))
                assertEquals(0, db.rawQuery("SELECT COUNT(*) FROM observations WHERE value < 0", null).use {
                    assertTrue(it.moveToFirst()); it.getInt(0)
                })
                assertEquals(
                    5,
                    db.rawQuery(
                        "SELECT COUNT(*) FROM observation_status_events WHERE raw_code IN (-100,-200,-300,0,255)",
                        null
                    ).use { assertTrue(it.moveToFirst()); it.getInt(0) }
                )
                assertEquals("ok", db.integrity())
                assertTrue(db.foreignKeyViolations() == 0)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun integrityConstraintsStayActiveAfterUpgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "issue144-integrity-").toFile()
        val context = isolatedContext(base, root)
        try {
            createV8Database(context)
            CirqaDatabase(context).use { database ->
                val db = database.writableDatabase

                assertRejected(db) {
                    db.execSQL(
                        """INSERT INTO observation_quality
                           (device_id, metric, measured_at_ms, sample_id, quality_status,
                            measurement_mode, timestamp_origin, source_kind, decoder_version,
                            profile_version, stored_at_ms)
                           VALUES ('device', 'heart_rate', ?, 'unknown-sample', 'NOT_IN_CATALOG',
                                   'UNKNOWN', 'DIRECT', 'FIT', '1', '21.222.0', ?)""",
                        arrayOf(measuredAt, generatedAt)
                    )
                }
                assertRejected(db) {
                    db.execSQL(
                        """INSERT INTO observation_status_events
                           (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                            status_code, measurement_mode, source_kind, decoder_version,
                            profile_version, stored_at_ms)
                           VALUES ('bad-time', 'device', 'respiration', ?, 'UNKNOWN',
                                   'UNKNOWN', 'UNKNOWN', 'FIT', '1', '21.222.0', ?)""",
                        arrayOf(measuredAt, generatedAt)
                    )
                }
                db.execSQL(
                    """INSERT INTO import_runs
                       (import_run_id, started_at_ms, decoder_version, profile_version, status)
                       VALUES ('run-1', ?, '1', '21.222.0', 'PARTIAL')""",
                    arrayOf(generatedAt)
                )
                assertRejected(db) {
                    db.execSQL(
                        """INSERT INTO import_errors
                           (import_run_id, error_class, redacted_message, occurred_at_ms, decision)
                           VALUES ('run-1', 'SOURCE_CONFLICT', 'redacted', ?, 'NOT_A_DECISION')""",
                        arrayOf(generatedAt)
                    )
                }
                assertEquals("ok", db.integrity())
                assertTrue(db.foreignKeyViolations() == 0)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun createV8Database(context: Context) {
        val db = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath("cirqa_measurements.db"), null
        )
        try {
            db.execSQL(
                """CREATE TABLE fit_sources (
                    source_sha256 TEXT PRIMARY KEY NOT NULL CHECK(length(source_sha256) = 64),
                    file_bytes INTEGER NOT NULL, relative_path TEXT NOT NULL,
                    validated_at_ms INTEGER NOT NULL, decoder_version TEXT NOT NULL,
                    profile_version TEXT NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE TABLE observations (
                    device_id TEXT NOT NULL, metric TEXT NOT NULL,
                    measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
                    value REAL NOT NULL, unit TEXT NOT NULL,
                    sample_id TEXT NOT NULL DEFAULT '', received_at_ms INTEGER,
                    PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
                )"""
            )
            db.execSQL(
                """CREATE TABLE fit_decoder_generations (
                    generation_id TEXT PRIMARY KEY NOT NULL CHECK(length(generation_id) = 64),
                    source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
                    decoder_version TEXT NOT NULL, profile_version TEXT NOT NULL,
                    output_digest TEXT NOT NULL CHECK(length(output_digest) = 64),
                    output_count INTEGER NOT NULL CHECK(output_count >= 0),
                    completed_at_ms INTEGER NOT NULL CHECK(completed_at_ms >= 0),
                    UNIQUE(source_sha256, decoder_version, profile_version),
                    UNIQUE(generation_id, source_sha256)
                )"""
            )
            db.execSQL(
                """CREATE TABLE fit_generation_observations (
                    generation_id TEXT NOT NULL REFERENCES fit_decoder_generations(generation_id) ON DELETE RESTRICT,
                    sample_id TEXT NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
                    measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
                    value REAL NOT NULL, unit TEXT NOT NULL,
                    received_at_ms INTEGER CHECK(received_at_ms IS NULL OR received_at_ms >= 0),
                    record_ordinal INTEGER NOT NULL CHECK(record_ordinal >= 0),
                    global_message_number INTEGER NOT NULL CHECK(global_message_number >= 0),
                    field_number INTEGER NOT NULL CHECK(field_number >= 0),
                    quality TEXT NOT NULL CHECK(quality = 'PROFILE_VALID'),
                    timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN
                        ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN')),
                    PRIMARY KEY(generation_id, sample_id)
                )"""
            )

            db.execSQL(
                "INSERT INTO fit_sources VALUES (?, 100, ?, 10, '1', '21.222.0')",
                arrayOf(sourceSha, "verified_fit/$sourceSha.fit")
            )
            val rows = listOf(
                arrayOf<Any>("device", "heart_rate", measuredAt, 72.0, "bpm", "sample-hr", measuredAt, "DIRECT"),
                arrayOf<Any>("device", "spo2", measuredAt, 98.0, "%", "sample-spo2", measuredAt, "RECONSTRUCTED_TS16")
            )
            rows.forEach { row ->
                db.execSQL(
                    "INSERT INTO observations VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(row[0], row[1], row[2], row[3], row[4], row[5], row[6])
                )
            }
            db.execSQL(
                "INSERT INTO fit_decoder_generations VALUES (?, ?, '1', '21.222.0', ?, 2, ?)",
                arrayOf<Any>(generationId, sourceSha, digest, generatedAt)
            )
            rows.forEach { row ->
                db.execSQL(
                    """INSERT INTO fit_generation_observations VALUES
                       (?, ?, ?, ?, ?, ?, ?, ?, 1, 20, 3, 'PROFILE_VALID', ?)""",
                    arrayOf(generationId, row[5], row[0], row[1], row[2], row[3], row[4], row[6], row[7])
                )
            }
            db.version = 8
        } finally {
            db.close()
        }
    }

    private fun count(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
            assertTrue(it.moveToFirst()); it.getInt(0)
        }

    private fun SQLiteDatabase.querySet(sql: String): Set<String> = rawQuery(sql, null).use { rows ->
        val values = mutableSetOf<String>()
        while (rows.moveToNext()) values += rows.getString(0)
        values
    }

    private fun SQLiteDatabase.value(metric: String): Double =
        rawQuery("SELECT value FROM observations WHERE metric = ?", arrayOf(metric)).use {
            assertTrue(it.moveToFirst()); it.getDouble(0)
        }

    private fun SQLiteDatabase.integrity(): String = rawQuery("PRAGMA integrity_check", null).use {
        assertTrue(it.moveToFirst()); it.getString(0)
    }

    private fun SQLiteDatabase.foreignKeyViolations(): Int = rawQuery("PRAGMA foreign_key_check", null).use {
        var n = 0
        while (it.moveToNext()) n++
        n
    }

    private fun assertRejected(db: SQLiteDatabase, action: () -> Unit) {
        try {
            action()
            fail("Expected the integrity constraint to reject the write")
        } catch (_: android.database.sqlite.SQLiteException) {
            // Expected: CHECK / UNIQUE / FK guards remain active after the additive migration.
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File =
            File(root, name).also { it.parentFile?.mkdirs() }

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?
        ): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}

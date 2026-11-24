package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic schema fixture: only metadata is stored, never measured values or device identifiers. */
@RunWith(AndroidJUnit4::class)
class ImportAuditRecorderTest {
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() {
        db = SQLiteDatabase.create(null)
        db.execSQL("""CREATE TABLE import_runs (
            import_run_id TEXT PRIMARY KEY NOT NULL, started_at_ms INTEGER NOT NULL,
            finished_at_ms INTEGER, source_sha256 TEXT, decoder_version TEXT NOT NULL,
            profile_version TEXT NOT NULL,
            status TEXT NOT NULL CHECK(status IN ('RUNNING','COMPLETED','PARTIAL','FAILED')),
            observed_count INTEGER NOT NULL DEFAULT 0, status_count INTEGER NOT NULL DEFAULT 0,
            error_count INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("""CREATE TABLE import_errors (
            id INTEGER PRIMARY KEY AUTOINCREMENT, import_run_id TEXT NOT NULL,
            source_sha256 TEXT, error_class TEXT NOT NULL, redacted_message TEXT NOT NULL,
            occurred_at_ms INTEGER NOT NULL,
            decision TEXT NOT NULL CHECK(decision IN ('FILE_SKIPPED','RUN_ABORTED')))""")
        db.execSQL("CREATE TABLE quality_status_catalog (status_code TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("INSERT INTO quality_status_catalog(status_code) VALUES ('UNKNOWN')")
        db.execSQL("CREATE TABLE measurement_modes (mode_code TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("INSERT INTO measurement_modes(mode_code) VALUES ('UNKNOWN')")
        db.execSQL("""CREATE TABLE observation_status_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT, idempotency_key TEXT NOT NULL UNIQUE,
            device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER,
            timestamp_origin TEXT NOT NULL, raw_code INTEGER, raw_value_text TEXT,
            status_code TEXT NOT NULL, measurement_mode TEXT NOT NULL,
            sensor_context TEXT NOT NULL, source_kind TEXT NOT NULL, source_sha256 TEXT,
            record_ordinal INTEGER, global_message_number INTEGER, field_number INTEGER,
            decoder_version TEXT NOT NULL, profile_version TEXT NOT NULL,
            observed_at_ms INTEGER, stored_at_ms INTEGER NOT NULL)""")
    }

    @After fun tearDown() { db.close() }

    @Test fun storesRedactedStatusEventWithoutMeasuredValue() {
        assertTrue(ImportAuditRecorder.recordStatusEvent(
            db, "run-2:file-1:READ", "cirqa-fit", "unknown", "READ", 2_000L, "decoder-v2", "21.222.0"
        ))
        assertFalse(ImportAuditRecorder.recordStatusEvent(
            db, "run-2:file-1:READ", "cirqa-fit", "unknown", "READ", 2_000L, "decoder-v2", "21.222.0"
        ))
        db.rawQuery("SELECT measured_at_ms, raw_code, raw_value_text, status_code FROM observation_status_events", null).use {
            assertTrue(it.moveToFirst())
            assertTrue(it.isNull(0))
            assertTrue(it.isNull(1))
            assertEquals("READ", it.getString(2))
            assertEquals("UNKNOWN", it.getString(3))
            assertFalse(it.moveToNext())
        }
    }

    @Test fun storesAggregateRunAndRedactedErrorIdempotently() {
        val sha = "a".repeat(64)
        assertTrue(ImportAuditRecorder.recordRun(
            db, "run-1", 1_000L, 5_000L, sha, "decoder-v1", "21.222.0", "PARTIAL",
            observedCount = 12, statusCount = 2, errorCount = 1
        ))
        assertFalse(ImportAuditRecorder.recordRun(
            db, "run-1", 1_000L, 5_000L, sha, "decoder-v1", "21.222.0", "PARTIAL", 12, 2, 1
        ))
        assertTrue(ImportAuditRecorder.recordFileError(db, "run-1", sha, "SOURCE_CONFLICT", 4_000L))
        assertFalse(ImportAuditRecorder.recordFileError(db, "run-1", sha, "SOURCE_CONFLICT", 4_000L))

        db.rawQuery("SELECT status, observed_count, status_count, error_count FROM import_runs", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("PARTIAL", it.getString(0))
            assertEquals(12, it.getInt(1))
            assertEquals(2, it.getInt(2))
            assertEquals(1, it.getInt(3))
            assertFalse(it.moveToNext())
        }
        db.rawQuery("SELECT error_class, redacted_message FROM import_errors", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("SOURCE_CONFLICT", it.getString(0))
            assertFalse(it.getString(1).contains(sha))
            assertFalse(it.moveToNext())
        }
    }
}

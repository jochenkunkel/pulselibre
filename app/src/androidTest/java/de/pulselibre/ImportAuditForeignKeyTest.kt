package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Generated-only SQLite regression tests; no device/health data. */
@RunWith(AndroidJUnit4::class)
class ImportAuditForeignKeyTest {
    @Test fun fileErrorCanOnlyBeRecordedAfterItsImportRunParentExists() {
        val db = SQLiteDatabase.create(null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL(
                """CREATE TABLE import_runs(
                    import_run_id TEXT PRIMARY KEY NOT NULL,
                    started_at_ms INTEGER NOT NULL,
                    finished_at_ms INTEGER,
                    source_sha256 TEXT,
                    decoder_version TEXT NOT NULL,
                    profile_version TEXT NOT NULL,
                    status TEXT NOT NULL,
                    observed_count INTEGER NOT NULL DEFAULT 0,
                    status_count INTEGER NOT NULL DEFAULT 0,
                    error_count INTEGER NOT NULL DEFAULT 0
                )"""
            )
            db.execSQL(
                """CREATE TABLE import_errors(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    import_run_id TEXT NOT NULL REFERENCES import_runs(import_run_id) ON DELETE RESTRICT,
                    source_sha256 TEXT,
                    error_class TEXT NOT NULL,
                    redacted_message TEXT NOT NULL,
                    occurred_at_ms INTEGER NOT NULL,
                    decision TEXT NOT NULL
                )"""
            )
            val runId = "synthetic-run"
            assertTrue(
                ImportAuditRecorder.recordRun(
                    db, runId, 100, null, null, "decoder-test", "profile-test",
                    "RUNNING", 0, 0, 0
                )
            )
            assertTrue(ImportAuditRecorder.recordFileError(db, runId, null, "SOURCE_CONFLICT", 101))
            assertTrue(ImportAuditRecorder.finishRun(db, runId, 102, "PARTIAL", 0, 0, 1))
            db.rawQuery(
                "SELECT status,error_count FROM import_runs WHERE import_run_id=?",
                arrayOf(runId)
            ).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("PARTIAL", rows.getString(0))
                assertEquals(1, rows.getInt(1))
            }
            db.rawQuery("SELECT COUNT(*) FROM import_errors", null).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(1, rows.getInt(0))
            }
        } finally {
            db.close()
        }
    }
}

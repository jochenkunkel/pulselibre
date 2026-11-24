package de.pulselibre

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Privacy-safe persistence of import-run/audit metadata (issue #601).
 *
 * Only aggregate counters and fixed error classes are stored. No measured values,
 * device addresses, file UIDs or raw bytes are written here. The original inputs
 * remain in the private content-addressed archive; this recorder only makes the
 * processing outcome durable and restorable.
 */
internal object ImportAuditRecorder {
    private const val REDACTED_MESSAGE = "per-file import failure; details in original archive"

    @Synchronized
    fun recordRun(
        db: SQLiteDatabase,
        runId: String,
        startedAtMs: Long,
        finishedAtMs: Long?,
        sourceSha256: String?,
        decoderVersion: String,
        profileVersion: String,
        status: String,
        observedCount: Int,
        statusCount: Int,
        errorCount: Int
    ): Boolean {
        require(runId.isNotBlank() && startedAtMs >= 0 && decoderVersion.isNotBlank() && profileVersion.isNotBlank())
        require(status in setOf("RUNNING", "COMPLETED", "PARTIAL", "FAILED"))
        require(sourceSha256 == null || sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(observedCount >= 0 && statusCount >= 0 && errorCount >= 0)
        val values = ContentValues().apply {
            put("import_run_id", runId)
            put("started_at_ms", startedAtMs)
            put("finished_at_ms", finishedAtMs)
            put("source_sha256", sourceSha256)
            put("decoder_version", decoderVersion)
            put("profile_version", profileVersion)
            put("status", status)
            put("observed_count", observedCount)
            put("status_count", statusCount)
            put("error_count", errorCount)
        }
        return db.insertWithOnConflict("import_runs", null, values, SQLiteDatabase.CONFLICT_IGNORE) >= 0
    }

    @Synchronized
    fun recordFileError(
        db: SQLiteDatabase,
        runId: String,
        sourceSha256: String?,
        errorClass: String,
        occurredAtMs: Long,
        decision: String = "FILE_SKIPPED"
    ): Boolean {
        require(runId.isNotBlank() && errorClass.matches(Regex("[A-Z_]{1,32}")) && occurredAtMs >= 0)
        require(decision in setOf("FILE_SKIPPED", "RUN_ABORTED"))
        require(sourceSha256 == null || sourceSha256.matches(Regex("[0-9a-f]{64}")))
        val alreadyPresent = db.rawQuery(
            """SELECT 1 FROM import_errors WHERE import_run_id=? AND error_class=?
               AND occurred_at_ms=? AND (source_sha256=? OR (source_sha256 IS NULL AND ? IS NULL)) LIMIT 1""",
            arrayOf(runId, errorClass, occurredAtMs.toString(), sourceSha256, sourceSha256)
        ).use { it.moveToFirst() }
        if (alreadyPresent) return false
        val values = ContentValues().apply {
            put("import_run_id", runId)
            put("source_sha256", sourceSha256)
            put("error_class", errorClass)
            put("redacted_message", REDACTED_MESSAGE)
            put("occurred_at_ms", occurredAtMs)
            put("decision", decision)
        }
        return db.insert("import_errors", null, values) >= 0
    }
}

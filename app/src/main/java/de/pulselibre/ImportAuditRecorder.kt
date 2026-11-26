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
            if (finishedAtMs == null) putNull("finished_at_ms") else put("finished_at_ms", finishedAtMs)
            if (sourceSha256 == null) putNull("source_sha256") else put("source_sha256", sourceSha256)
            put("decoder_version", decoderVersion)
            put("profile_version", profileVersion)
            put("status", status)
            put("observed_count", observedCount)
            put("status_count", statusCount)
            put("error_count", errorCount)
        }
        return db.insertWithOnConflict("import_runs", null, values, SQLiteDatabase.CONFLICT_IGNORE) >= 0
    }

    /** Update the parent row with terminal counters; it must exist before child errors are inserted. */
    @Synchronized
    fun finishRun(
        db: SQLiteDatabase,
        runId: String,
        finishedAtMs: Long,
        status: String,
        observedCount: Int,
        statusCount: Int,
        errorCount: Int
    ): Boolean {
        require(runId.isNotBlank() && finishedAtMs >= 0)
        require(status in setOf("COMPLETED", "PARTIAL", "FAILED"))
        require(observedCount >= 0 && statusCount >= 0 && errorCount >= 0)
        val values = ContentValues().apply {
            put("finished_at_ms", finishedAtMs)
            put("status", status)
            put("observed_count", observedCount)
            put("status_count", statusCount)
            put("error_count", errorCount)
        }
        return db.update("import_runs", values, "import_run_id=?", arrayOf(runId)) == 1
    }

    @Synchronized
    fun recordFileError(
        db: SQLiteDatabase,
        runId: String,
        sourceSha256: String?,
        errorClass: String,
        occurredAtMs: Long,
        decision: String = "FILE_SKIPPED",
        /** Already redacted reason (see [redactReason]); the fixed placeholder when none is known. */
        reason: String? = null
    ): Boolean {
        require(runId.isNotBlank() && errorClass.matches(Regex("[A-Z_]{1,32}")) && occurredAtMs >= 0)
        require(decision in setOf("FILE_SKIPPED", "RUN_ABORTED"))
        require(sourceSha256 == null || sourceSha256.matches(Regex("[0-9a-f]{64}")))
        // rawQuery binds every String argument via bindString; a null String throws
        // "bind value at index N is null". Split the lookup so null hashes are matched
        // with SQL IS NULL and never bound as parameters.
        val alreadyPresent = if (sourceSha256 == null) {
            db.rawQuery(
                """SELECT 1 FROM import_errors WHERE import_run_id=? AND error_class=?
                   AND occurred_at_ms=? AND source_sha256 IS NULL LIMIT 1""",
                arrayOf(runId, errorClass, occurredAtMs.toString())
            ).use { it.moveToFirst() }
        } else {
            db.rawQuery(
                """SELECT 1 FROM import_errors WHERE import_run_id=? AND error_class=?
                   AND occurred_at_ms=? AND source_sha256=? LIMIT 1""",
                arrayOf(runId, errorClass, occurredAtMs.toString(), sourceSha256)
            ).use { it.moveToFirst() }
        }
        if (alreadyPresent) return false
        val values = ContentValues().apply {
            put("import_run_id", runId)
            if (sourceSha256 == null) putNull("source_sha256") else put("source_sha256", sourceSha256)
            put("error_class", errorClass)
            put("redacted_message", reason?.let(::redactReason)?.takeIf { it.isNotBlank() } ?: REDACTED_MESSAGE)
            put("occurred_at_ms", occurredAtMs)
            put("decision", decision)
        }
        return db.insert("import_errors", null, values) >= 0
    }

    /**
     * #628: a reason that can classify a per-file failure without leaking data. Keeps the exception type and the
     * developer-written message, replaces every run of 4+ digits/hex characters (values, hashes, offsets) by `#`, drops
     * everything but letters, digits, space and basic punctuation, and caps the length.
     */
    fun redactReason(raw: String): String = raw
        .replace(Regex("""\b(?=[0-9A-Fa-f]*\d)[0-9A-Fa-f]{4,}\b|\d+"""), "#") // hex only with a digit: words stay
        .replace(Regex("""[^A-Za-zÄÖÜäöüß#:.,()/ _-]"""), "")
        .replace(Regex(" {2,}"), " ")
        .trim()
        .take(160)

    fun reasonOf(error: Throwable): String = redactReason("${error.javaClass.simpleName}: ${error.message.orEmpty()}")

    /**
     * One durable status event for a processing outcome. Only fixed error classes and
     * aggregate metadata are stored; no measured value, file name or device identifier.
     */
    @Synchronized
    fun recordStatusEvent(
        db: SQLiteDatabase,
        idempotencyKey: String,
        deviceId: String,
        metric: String,
        errorClass: String,
        occurredAtMs: Long,
        decoderVersion: String,
        profileVersion: String
    ): Boolean {
        require(idempotencyKey.isNotBlank() && deviceId.isNotBlank() && metric.isNotBlank())
        require(errorClass.matches(Regex("[A-Z_]{1,32}")) && occurredAtMs >= 0)
        require(decoderVersion.isNotBlank() && profileVersion.isNotBlank())
        val values = ContentValues().apply {
            put("idempotency_key", idempotencyKey)
            put("device_id", deviceId)
            put("metric", metric)
            putNull("measured_at_ms")
            put("timestamp_origin", "NOT_APPLICABLE")
            putNull("raw_code")
            put("raw_value_text", errorClass)
            put("status_code", "UNKNOWN")
            put("measurement_mode", "UNKNOWN")
            put("sensor_context", "UNKNOWN")
            put("source_kind", "FIT")
            putNull("source_sha256")
            putNull("record_ordinal")
            putNull("global_message_number")
            putNull("field_number")
            put("decoder_version", decoderVersion)
            put("profile_version", profileVersion)
            put("observed_at_ms", occurredAtMs)
            put("stored_at_ms", System.currentTimeMillis())
        }
        return db.insertWithOnConflict("observation_status_events", null, values, SQLiteDatabase.CONFLICT_IGNORE) >= 0
    }
}

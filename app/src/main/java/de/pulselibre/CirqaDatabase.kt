package de.pulselibre

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Canonical local SQLite store for CIRQA events.
 * New events are written here directly; obsolete JSONL recordings are no longer imported.
 * UNIQUE SHA-256 of each full line makes repeated imports idempotent.
 */
class CirqaDatabase(private val contextRef: Context) : SQLiteOpenHelper(contextRef, "cirqa_measurements.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            line_sha256 TEXT NOT NULL UNIQUE,
            received_at_utc TEXT NOT NULL,
            dashboard_time TEXT,
            kind TEXT NOT NULL,
            service INTEGER,
            payload_json TEXT NOT NULL
        )""")
        db.execSQL("CREATE INDEX events_service_time ON events(service, received_at_utc)")
        createDashboardIndex(db)
        db.execSQL("CREATE INDEX events_kind_time ON events(kind, received_at_utc)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion == 1 && newVersion == 2)
        db.execSQL("ALTER TABLE events ADD COLUMN dashboard_time TEXT")
        var lastId = 0L
        while (true) {
            val batch = ArrayList<Pair<Long, String>>()
            db.rawQuery("SELECT id, received_at_utc FROM events WHERE id > ? AND kind = 'reading' ORDER BY id LIMIT 512",
                arrayOf(lastId.toString())).use { rows ->
                while (rows.moveToNext()) batch.add(rows.getLong(0) to rows.getString(1))
            }
            if (batch.isEmpty()) break
            for ((id, timestamp) in batch) {
                db.execSQL("UPDATE events SET dashboard_time = ? WHERE id = ?", arrayOf(timeKey(timestamp), id))
            }
            lastId = batch.last().first
        }
        createDashboardIndex(db)
    }

    private fun createDashboardIndex(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX events_dashboard_time ON events(service, dashboard_time DESC, id ASC) WHERE kind = 'reading'")
    }

    fun latestEventId(): Long = readableDatabase.rawQuery("SELECT COALESCE(MAX(id), 0) FROM events", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Explicit user decision: discard the former JSONL night recording.
        // SQLite events are preserved. Retry on a later open if deletion fails.
        // Once per process is enough (and avoids file-system work on every open); retried if deletion failed.
        if (!legacyCleanupDone && LegacyNightRecording.remove(contextRef.filesDir)) legacyCleanupDone = true
    }

    /** Store a single locally generated JSONL event immediately, without a second BLE path. */
    @Synchronized fun insertLine(line: String): Boolean {
        return insertLine(writableDatabase, line)
    }

    /** Raw measured readings in chronological order, not inferred values. */
    fun readingLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events WHERE kind = ? ORDER BY received_at_utc, id",
            arrayOf("reading")
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    /**
     * One-time, idempotent import of the former generic readings database.
     * The old file is retained as a safety backup, never used for new writes.
     */
    @Synchronized fun importLegacyReadings(): Int {
        val legacy = contextRef.getDatabasePath("pulselibre_readings.db")
        if (!legacy.exists()) return 0
        var imported = 0
        val old = LocalReadingStore(contextRef)
        try {
            old.readWindow(ExportWindow(0, Long.MAX_VALUE)).forEach { record ->
                val line = JSONObject()
                    .put("schema", 1)
                    .put("type", "legacy_record")
                    .put("received_at_utc", java.time.Instant.ofEpochMilli(
                        record.receivedAtEpochMillis ?: record.measuredAtEpochMillis).toString())
                    .put("device_id", record.deviceId)
                    .put("metric", record.metric)
                    .put("measured_at_ms", record.measuredAtEpochMillis)
                    .put("value", record.value)
                    .put("unit", record.unit)
                    .put("sample_id", record.sampleId ?: "")
                    .put("received_at_ms", record.receivedAtEpochMillis ?: JSONObject.NULL)
                    .toString()
                if (insertLine(line)) imported++
            }
        } finally { old.close() }
        return imported
    }

    fun legacyLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events WHERE kind = ? ORDER BY received_at_utc, id",
            arrayOf("legacy_record")
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun allLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events ORDER BY received_at_utc, id", null
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun eventBytes(): Long = readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(LENGTH(CAST(payload_json AS BLOB)) + 1), 0) FROM events", null
    ).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }

    fun eventCount(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM events", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    /** Run a callback against a consistent read snapshot of both local SQLite stores. */
    @Synchronized internal fun <T> readStoredSnapshot(block: (SQLiteDatabase) -> T): T =
        withLegacyDatabase { db ->
            db.beginTransaction()
            try {
                val result = block(db)
                db.setTransactionSuccessful()
                result
            } finally {
                db.endTransaction()
            }
        }

    /** Replace both local stores in a single attached-database transaction. */
    @Synchronized fun replaceAllStoredRows(
        events: Sequence<String>,
        readings: Sequence<RawExportRecord>,
        cursors: Sequence<LocalSyncCursor>
    ) = withLegacyDatabase { db ->
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM main.events")
            db.execSQL("DELETE FROM legacy.readings")
            db.execSQL("DELETE FROM legacy.sync_cursors")

            val eventInsert = db.compileStatement(
                "INSERT INTO main.events(line_sha256, received_at_utc, kind, service, payload_json, dashboard_time) VALUES(?, ?, ?, ?, ?, ?)"
            )
            events.forEach { line ->
                require(isValidStoredLine(line)) { "Ungültiger Ereigniseintrag im Backup" }
                val item = JSONObject(line)
                eventInsert.clearBindings()
                eventInsert.bindString(1, lineSha256(line))
                eventInsert.bindString(2, item.getString("received_at_utc"))
                eventInsert.bindString(3, item.getString("type"))
                if (item.has("service") && !item.isNull("service")) eventInsert.bindLong(4, item.getLong("service"))
                else eventInsert.bindNull(4)
                eventInsert.bindString(5, line)
                val key = if (item.getString("type") == "reading") timeKey(item.getString("received_at_utc")) else null
                if (key == null) eventInsert.bindNull(6) else eventInsert.bindString(6, key)
                eventInsert.executeInsert()
            }

            val readingInsert = db.compileStatement(
                """INSERT INTO legacy.readings
                   (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   VALUES(?, ?, ?, ?, ?, ?, ?)"""
            )
            readings.forEach { record ->
                require(isValidStoredReading(record)) { "Ungültiger Messwert im Backup" }
                readingInsert.clearBindings()
                readingInsert.bindString(1, record.deviceId)
                readingInsert.bindString(2, record.metric)
                readingInsert.bindLong(3, record.measuredAtEpochMillis)
                readingInsert.bindDouble(4, record.value)
                readingInsert.bindString(5, record.unit)
                readingInsert.bindString(6, record.sampleId ?: "")
                if (record.receivedAtEpochMillis == null) readingInsert.bindNull(7)
                else readingInsert.bindLong(7, record.receivedAtEpochMillis)
                readingInsert.executeInsert()
            }

            val cursorInsert = db.compileStatement(
                "INSERT INTO legacy.sync_cursors(device_id, last_committed_ms) VALUES(?, ?)"
            )
            cursors.forEach { value ->
                require(value.deviceId.isNotBlank() && value.lastCommittedEpochMillis >= 0) {
                    "Ungültiger Synchronisationsstand im Backup"
                }
                cursorInsert.clearBindings()
                cursorInsert.bindString(1, value.deviceId)
                cursorInsert.bindLong(2, value.lastCommittedEpochMillis)
                cursorInsert.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertLine(db: SQLiteDatabase, line: String): Boolean {
        val item = try { JSONObject(line) } catch (_: Exception) { return false }
        val kind = item.optString("type")
        if (item.optInt("schema") != 1 || kind.isBlank()) return false
        val values = ContentValues().apply {
            put("line_sha256", lineSha256(line))
            put("received_at_utc", item.optString("received_at_utc"))
            if (kind == "reading") put("dashboard_time", timeKey(item.optString("received_at_utc")))
            put("kind", kind)
            if (item.has("service") && !item.isNull("service")) put("service", item.optInt("service"))
            put("payload_json", line)
        }
        return db.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    private inline fun <T> withLegacyDatabase(block: (SQLiteDatabase) -> T): T {
        LocalReadingStore(contextRef).use { it.writableDatabase }
        val db = writableDatabase
        db.execSQL("ATTACH DATABASE ? AS legacy", arrayOf(contextRef.getDatabasePath("pulselibre_readings.db").absolutePath))
        return try { block(db) }
        finally { runCatching { db.execSQL("DETACH DATABASE legacy") } }
    }

    companion object {
        @Volatile private var legacyCleanupDone = false
        private val keyFormat = java.time.format.DateTimeFormatterBuilder().appendInstant(9).toFormatter()
        internal fun timeKey(time: java.time.Instant): String = keyFormat.format(time)
        private fun timeKey(timestamp: String): String? = try { timeKey(java.time.Instant.parse(timestamp)) }
            catch (_: java.time.format.DateTimeParseException) { null }

        fun isValidStoredLine(line: String): Boolean = try {
            val item = JSONObject(line)
            item.optInt("schema") == 1 && item.optString("type").isNotBlank() &&
                item.optString("received_at_utc").isNotBlank()
        } catch (_: Exception) { false }

        fun isValidStoredReading(record: RawExportRecord): Boolean = record.deviceId.isNotBlank() &&
            record.metric.isNotBlank() && record.unit.isNotBlank() && record.measuredAtEpochMillis >= 0 &&
            record.value.isFinite() && (record.receivedAtEpochMillis == null || record.receivedAtEpochMillis >= 0)

        private fun lineSha256(line: String): String = MessageDigest.getInstance("SHA-256")
            .digest(line.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

data class LocalSyncCursor(val deviceId: String, val lastCommittedEpochMillis: Long)

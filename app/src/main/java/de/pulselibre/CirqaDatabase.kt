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
class CirqaDatabase(private val contextRef: Context) : SQLiteOpenHelper(contextRef, "cirqa_measurements.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            line_sha256 TEXT NOT NULL UNIQUE,
            received_at_utc TEXT NOT NULL,
            kind TEXT NOT NULL,
            service INTEGER,
            payload_json TEXT NOT NULL
        )""")
        db.execSQL("CREATE INDEX events_service_time ON events(service, received_at_utc)")
        db.execSQL("CREATE INDEX events_kind_time ON events(kind, received_at_utc)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never drop raw health records. A future migration must be additive.
        error("Unsupported CIRQA database migration $oldVersion -> $newVersion")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Explicit user decision: discard the former JSONL night recording.
        // SQLite events are preserved. Retry on a later open if deletion fails.
        LegacyNightRecording.remove(contextRef.filesDir)
    }

    /** Store a single locally generated JSONL event immediately, without a second BLE path. */
    @Synchronized fun insertLine(line: String): Boolean {
        val item = try { JSONObject(line) } catch (_: Exception) { return false }
        val kind = item.optString("type")
        val timestamp = item.optString("received_at_utc")
        if (item.optInt("schema") != 1 || kind.isBlank()) return false
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(line.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val values = ContentValues().apply {
            put("line_sha256", sha)
            put("received_at_utc", timestamp)
            put("kind", kind)
            if (item.has("service") && !item.isNull("service")) put("service", item.optInt("service"))
            put("payload_json", line)
        }
        return writableDatabase.insertWithOnConflict(
            "events", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
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
}

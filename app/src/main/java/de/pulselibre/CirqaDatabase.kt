package de.pulselibre

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Versioned, private, lossless mirror of the JSONL diagnostic.
 * JSONL remains the authoritative export; this index is rebuildable.
 * UNIQUE SHA-256 of each full line makes repeated imports idempotent.
 */
class CirqaDatabase(context: Context) : SQLiteOpenHelper(context, "cirqa_measurements.db", null, 1) {
    private val source = File(context.filesDir, "cirqa_overnight.jsonl")

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

    /** Safe to call repeatedly. Malformed/incomplete records are skipped. */
    @Synchronized fun importJsonl(): ImportResult {
        if (!source.exists()) return ImportResult(0, 0)
        val db = writableDatabase
        var imported = 0
        var invalid = 0
        db.beginTransaction()
        try {
            source.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    val item = try { JSONObject(line) } catch (_: Exception) { invalid++; return@forEach }
                    val kind = item.optString("type")
                    val timestamp = item.optString("received_at_utc")
                    if (item.optInt("schema") != 1 || kind.isBlank() ||
                        !timestamp.matches(Regex("^\\d{4}-\\d{2}-\\d{2}T.*Z$"))) {
                        invalid++
                        return@forEach
                    }
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
                    if (db.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L) imported++
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return ImportResult(imported, invalid)
    }

    data class ImportResult(val imported: Int, val invalid: Int)

    fun eventCount(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM events", null).use {
        it.moveToFirst()
        it.getLong(0)
    }
}

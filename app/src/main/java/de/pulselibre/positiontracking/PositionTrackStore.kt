package de.pulselibre.positiontracking

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** App-private, separate database for position tracks (#788); never part of logs or unrequested exports. */
class PositionTrackStore(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT, start_ms INTEGER NOT NULL, end_ms INTEGER, label TEXT)")
        db.execSQL("CREATE TABLE points(session_id INTEGER NOT NULL, time_ms INTEGER NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, " +
            "accuracy_m REAL, provider TEXT, altitude_m REAL, speed_mps REAL, bearing_deg REAL, " +
            "UNIQUE(session_id, time_ms, provider))")
        db.execSQL("CREATE INDEX points_session_time ON points(session_id, time_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun startSession(startMs: Long, label: String?): Long = writableDatabase.insertOrThrow("sessions", null,
        ContentValues().apply { put("start_ms", startMs); put("label", label) })

    fun endSession(id: Long, endMs: Long) {
        writableDatabase.update("sessions", ContentValues().apply { put("end_ms", endMs) }, "id=? AND end_ms IS NULL", arrayOf(id.toString()))
    }

    /** Ends sessions left open by a killed process at their last point (or start). */
    fun closeDangling() {
        writableDatabase.execSQL("UPDATE sessions SET end_ms = COALESCE((SELECT MAX(time_ms) FROM points WHERE session_id = sessions.id), start_ms) WHERE end_ms IS NULL")
    }

    /** Duplicates (same time and provider) are ignored; invalid positions are not stored. */
    fun add(sessionId: Long, p: PositionPoint): Boolean {
        if (!p.isValid()) return false
        return writableDatabase.insertWithOnConflict("points", null, ContentValues().apply {
            put("session_id", sessionId); put("time_ms", p.timeMs); put("lat", p.latitude); put("lon", p.longitude)
            put("accuracy_m", p.accuracyMeters); put("provider", p.provider); put("altitude_m", p.altitudeMeters)
            put("speed_mps", p.speedMps); put("bearing_deg", p.bearingDeg)
        }, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun sessions(): List<PositionSession> = readableDatabase.rawQuery(
        "SELECT id, start_ms, end_ms, label FROM sessions ORDER BY start_ms DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(PositionSession(c.getLong(0), c.getLong(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3))) }
    }

    fun points(sessionId: Long): List<PositionPoint> = readableDatabase.rawQuery(
        "SELECT time_ms, lat, lon, accuracy_m, provider, altitude_m, speed_mps, bearing_deg FROM points WHERE session_id=? ORDER BY time_ms",
        arrayOf(sessionId.toString())).use { c ->
        buildList {
            while (c.moveToNext()) add(PositionPoint(c.getLong(0), c.getDouble(1), c.getDouble(2),
                if (c.isNull(3)) null else c.getFloat(3), c.getString(4),
                if (c.isNull(5)) null else c.getDouble(5), if (c.isNull(6)) null else c.getFloat(6), if (c.isNull(7)) null else c.getFloat(7)))
        }
    }

    fun pointCount(sessionId: Long): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM points WHERE session_id=?",
        arrayOf(sessionId.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    companion object { const val NAME = "position_tracks.db" }
}

package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Private, offline SQLite store. The database contains only validated readings.
 * Each batch and its cursor are committed in one transaction.
 * No Bluetooth pairing, network access or Garmin-specific decoding occurs here.
 */
class LocalReadingStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "pulselibre_readings.db", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE readings (
                device_id TEXT NOT NULL,
                metric TEXT NOT NULL,
                measured_at_ms INTEGER NOT NULL,
                value REAL NOT NULL,
                unit TEXT NOT NULL,
                sample_id TEXT NOT NULL DEFAULT '',
                received_at_ms INTEGER,
                PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
            )"""
        )
        db.execSQL(
            """CREATE TABLE sync_cursors (
                device_id TEXT PRIMARY KEY NOT NULL,
                last_committed_ms INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX readings_by_time ON readings(measured_at_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema version 1; future migrations must preserve existing measurements.
        throw IllegalStateException("Missing database migration from $oldVersion to $newVersion")
    }

    /**
     * Atomic, idempotent insert + per-device checkpoint. Invalid readings fail
     * the entire batch rather than silently advancing the checkpoint.
     */
    fun commitBatch(deviceId: String, readings: List<RawExportRecord>) {
        require(deviceId.isNotBlank())
        require(readings.all {
            it.deviceId == deviceId && it.metric.isNotBlank() && it.unit.isNotBlank() &&
                it.measuredAtEpochMillis >= 0 && it.value.isFinite() &&
                (it.receivedAtEpochMillis == null || it.receivedAtEpochMillis >= 0)
        }) { "Invalid reading in batch" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val insert = db.compileStatement(
                """INSERT OR IGNORE INTO readings
                    (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?)"""
            )
            for (reading in readings) {
                insert.clearBindings()
                insert.bindString(1, reading.deviceId)
                insert.bindString(2, reading.metric)
                insert.bindLong(3, reading.measuredAtEpochMillis)
                insert.bindDouble(4, reading.value)
                insert.bindString(5, reading.unit)
                insert.bindString(6, reading.sampleId ?: "")
                if (reading.receivedAtEpochMillis == null) insert.bindNull(7)
                else insert.bindLong(7, reading.receivedAtEpochMillis)
                insert.executeInsert()
            }
            val newest = readings.maxOfOrNull { it.measuredAtEpochMillis }
            if (newest != null) {
                val insertCursor = db.compileStatement(
                    "INSERT OR IGNORE INTO sync_cursors(device_id, last_committed_ms) VALUES (?, ?)"
                )
                insertCursor.bindString(1, deviceId)
                insertCursor.bindLong(2, newest)
                insertCursor.executeInsert()
                val advanceCursor = db.compileStatement(
                    """UPDATE sync_cursors SET last_committed_ms =
                       MAX(last_committed_ms, ?) WHERE device_id = ?"""
                )
                advanceCursor.bindLong(1, newest)
                advanceCursor.bindString(2, deviceId)
                advanceCursor.executeUpdateDelete()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun cursorFor(deviceId: String): SyncCursor {
        require(deviceId.isNotBlank())
        readableDatabase.rawQuery(
            "SELECT last_committed_ms FROM sync_cursors WHERE device_id = ?",
            arrayOf(deviceId)
        ).use { cursor ->
            return if (cursor.moveToFirst()) SyncCursor(cursor.getLong(0)) else SyncCursor()
        }
    }

    /** Query only requested measurement times, with a stable ordering. */
    fun readWindow(window: ExportWindow): List<RawExportRecord> {
        val result = mutableListOf<RawExportRecord>()
        readableDatabase.rawQuery(
            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
               FROM readings WHERE measured_at_ms >= ? AND measured_at_ms < ?
               ORDER BY measured_at_ms, device_id, metric, sample_id""",
            arrayOf(window.startInclusiveEpochMillis.toString(), window.endExclusiveEpochMillis.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += RawExportRecord(
                    deviceId = cursor.getString(0),
                    metric = cursor.getString(1),
                    measuredAtEpochMillis = cursor.getLong(2),
                    value = cursor.getDouble(3),
                    unit = cursor.getString(4),
                    sampleId = cursor.getString(5).ifEmpty { null },
                    receivedAtEpochMillis = if (cursor.isNull(6)) null else cursor.getLong(6)
                )
            }
        }
        return result
    }
}

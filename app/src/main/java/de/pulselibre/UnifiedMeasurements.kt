package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.time.Instant

/** Where a unified measurement comes from. Each origin keeps its own time basis (see [UnifiedMeasurements]). */
enum class MeasurementOrigin(val code: String) {
    /** Live readings received over the BLE connection; time = reception time on the phone. */
    LIVE("live"),
    /** Observations imported from older stores or backups without FIT provenance; time as stored. */
    IMPORT("import"),
    /** Active FIT decoder generation; time = measurement time of the band. */
    BAND("band")
}

/** Live reading services and the numeric fields that count as measurements (shared with the CSV export). */
object LiveReadingFields {
    val FIELDS: Map<Int, List<Pair<String, String>>> = mapOf(
        6 to listOf("bpm" to "bpm", "resting_bpm" to "bpm"),
        7 to listOf("steps" to "steps", "goal" to "steps"),
        8 to listOf("total" to "kcal", "active" to "kcal"),
        10 to listOf("moderate" to "min", "vigorous" to "min"),
        12 to listOf("rr_ms" to "ms"),
        13 to listOf("stress" to "device_scale"),
        19 to listOf("spo2_percent" to "%"),
        20 to listOf("body_battery" to "device_scale"),
        21 to listOf("breaths_per_min" to "breaths/min")
    )
    const val DEVICE_ID = "CIRQA"

    data class Row(val metric: String, val value: Double, val unit: String, val timeMs: Long, val sampleId: String)

    /** True for a reading whose reception time cannot be parsed (counted as unreadable, A6-8). */
    fun hasUnreadableTime(item: JSONObject): Boolean =
        item.optInt("schema") == 1 && item.optString("type") == "reading" &&
            try { Instant.parse(item.getString("received_at_utc")); false } catch (_: Exception) { true }

    /** Pure mapping of one stored reading event to measurement rows; unreadable or foreign items give an empty list. */
    fun rows(eventId: Long, item: JSONObject): List<Row> {
        if (item.optInt("schema") != 1 || item.optString("type") != "reading") return emptyList()
        val time = try { Instant.parse(item.getString("received_at_utc")).toEpochMilli() } catch (_: Exception) { return emptyList() }
        if (time < 0) return emptyList()
        return FIELDS[item.optInt("service", -1)].orEmpty().mapNotNull { (key, unit) ->
            if (!item.has(key) || item.isNull(key)) return@mapNotNull null
            val value = (item.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: return@mapNotNull null
            Row(key, value, unit, time, "event-$eventId-$key")
        }
    }
}

/** One aggregated line of the Verlauf "Alle Daten" table. */
data class UnifiedDaySummary(
    val dayIndex: Long, val origin: MeasurementOrigin, val metric: String, val unit: String,
    val count: Int, val minimum: Double, val mean: Double, val maximum: Double
)

/**
 * Single place to read every measurement: live readings and imported/band history side by side in one table of
 * the same SQLite file (`unified_measurements`). It is DERIVED data: rebuildable from `events`, `observations` and
 * the active FIT generations, never a source of truth, therefore not part of backups and safe to drop.
 *
 * Origins are kept apart on purpose: the band's clock and the phone's clock are different time bases and values
 * are never merged or interpolated across origins.
 */
class UnifiedMeasurements(private val db: SQLiteDatabase) {
    fun ensureSchema() {
        db.execSQL("""CREATE TABLE IF NOT EXISTS unified_measurements (
            origin TEXT NOT NULL CHECK(origin IN ('live', 'import', 'band')),
            device_id TEXT NOT NULL,
            metric TEXT NOT NULL,
            measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
            value REAL NOT NULL,
            unit TEXT NOT NULL,
            sample_id TEXT NOT NULL DEFAULT '',
            received_at_ms INTEGER,
            PRIMARY KEY(origin, device_id, metric, measured_at_ms, sample_id)
        ) WITHOUT ROWID""")
        db.execSQL("CREATE INDEX IF NOT EXISTS unified_measurements_by_time ON unified_measurements(measured_at_ms)")
        db.execSQL("CREATE TABLE IF NOT EXISTS unified_state(key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
    }

    /** Brings the table up to date; cheap when nothing changed. Idempotent, safe to call often. */
    fun refresh() {
        ensureSchema()
        refreshLive()
        refreshHistorical()
    }

    private fun state(key: String): String? =
        db.rawQuery("SELECT value FROM unified_state WHERE key = ?", arrayOf(key)).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun setState(key: String, value: String) {
        db.execSQL("INSERT OR REPLACE INTO unified_state(key, value) VALUES(?, ?)", arrayOf(key, value))
    }

    private fun scalar(sql: String): Long = db.rawQuery(sql, null).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L }

    private fun refreshLive() {
        val maxId = scalar("SELECT MAX(id) FROM events")
        var cursor = state("live_event_id")?.toLongOrNull() ?: 0L
        var unreadable = state("live_unreadable")?.toLongOrNull() ?: 0L
        if (maxId < cursor) { // events were replaced (restore/reset): rebuild this origin
            db.execSQL("DELETE FROM unified_measurements WHERE origin = 'live'")
            cursor = 0L
            unreadable = 0L
        }
        val insert = db.compileStatement(
            """INSERT OR IGNORE INTO unified_measurements(origin, device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
               VALUES('live', ?, ?, ?, ?, ?, ?, ?)""")
        while (cursor < maxId) {
            val batch = ArrayList<Pair<Long, String>>()
            db.rawQuery("SELECT id, payload_json FROM events WHERE kind = 'reading' AND id > ? ORDER BY id LIMIT 500",
                arrayOf(cursor.toString())).use { rows -> while (rows.moveToNext()) batch += rows.getLong(0) to rows.getString(1) }
            if (batch.isEmpty()) { cursor = maxId; break }
            db.beginTransaction()
            try {
                for ((id, json) in batch) {
                    val item = try { JSONObject(json) } catch (_: Exception) { unreadable++; continue }
                    if (LiveReadingFields.hasUnreadableTime(item)) { unreadable++; continue }
                    for (row in LiveReadingFields.rows(id, item)) {
                        insert.clearBindings()
                        insert.bindString(1, LiveReadingFields.DEVICE_ID); insert.bindString(2, row.metric)
                        insert.bindLong(3, row.timeMs); insert.bindDouble(4, row.value); insert.bindString(5, row.unit)
                        insert.bindString(6, row.sampleId); insert.bindLong(7, row.timeMs)
                        insert.executeInsert()
                    }
                }
                cursor = batch.last().first
                setState("live_event_id", cursor.toString())
                setState("live_unreadable", unreadable.toString())
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (state("live_event_id")?.toLongOrNull() != cursor) setState("live_event_id", cursor.toString())
        if (state("live_unreadable")?.toLongOrNull() != unreadable) setState("live_unreadable", unreadable.toString())
    }

    /** Number of stored live events that could not be read (broken JSON or time); reported by the CSV export (A6-8). */
    fun unreadableLiveEvents(): Long = state("live_unreadable")?.toLongOrNull() ?: 0L

    /** Values of one origin and metric in [startMs, endMs), oldest first, at most [limit] (null = all). */
    fun points(origin: MeasurementOrigin, metric: String, startMs: Long, endMs: Long, limit: Int? = null): List<Triple<Long, Double, String>> {
        val out = ArrayList<Triple<Long, Double, String>>()
        db.rawQuery(
            """SELECT measured_at_ms, value, unit FROM unified_measurements
               WHERE origin = ? AND metric = ? AND measured_at_ms >= ? AND measured_at_ms < ?
               ORDER BY measured_at_ms, device_id, sample_id""" + (limit?.let { " LIMIT $it" } ?: ""),
            arrayOf(origin.code, metric, startMs.toString(), endMs.toString())
        ).use { c -> while (c.moveToNext()) out += Triple(c.getLong(0), c.getDouble(1), c.getString(2)) }
        return out
    }

    /** Newest measurement time of one origin and metric not after [notAfterMs], or null. */
    fun latest(origin: MeasurementOrigin, metric: String, notAfterMs: Long): Long? =
        db.rawQuery("SELECT MAX(measured_at_ms) FROM unified_measurements WHERE origin = ? AND metric = ? AND measured_at_ms <= ?",
            arrayOf(origin.code, metric, notAfterMs.toString())).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }

    fun exists(origin: MeasurementOrigin, metric: String): Boolean =
        db.rawQuery("SELECT 1 FROM unified_measurements WHERE origin = ? AND metric = ? LIMIT 1", arrayOf(origin.code, metric)).use { it.moveToFirst() }

    /** Everything in [startMs, endMs) as export records, oldest first; the single source for the CSV export. */
    fun records(startMs: Long, endMs: Long): List<RawExportRecord> {
        val out = ArrayList<RawExportRecord>()
        db.rawQuery(
            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms FROM unified_measurements
               WHERE measured_at_ms >= ? AND measured_at_ms < ? ORDER BY measured_at_ms, device_id, metric, sample_id""",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { c ->
            while (c.moveToNext()) out += RawExportRecord(c.getString(0), c.getString(1), c.getLong(2), c.getDouble(3),
                c.getString(4), c.getString(5).ifEmpty { null }, if (c.isNull(6)) null else c.getLong(6))
        }
        return out
    }

    private fun refreshHistorical() {
        // Cheap change signature; any change rebuilds the two historical origins (they are rebuildable copies).
        val signature = listOf(
            scalar("SELECT COUNT(*) FROM observations"),
            scalar("SELECT COUNT(*) FROM fit_observation_provenance"),
            scalar("SELECT COUNT(*) FROM fit_generation_observations"),
            scalar("SELECT COUNT(*) FROM fit_active_generations"),
            scalar("SELECT COALESCE(SUM(output_count), 0) FROM fit_decoder_generations")
        ).joinToString(",")
        if (state("historical_signature") == signature) return
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM unified_measurements WHERE origin IN ('import', 'band')")
            db.execSQL(
                """INSERT OR IGNORE INTO unified_measurements(origin, device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   SELECT 'import', r.device_id, r.metric, r.measured_at_ms, r.value, r.unit, r.sample_id, r.received_at_ms
                   FROM observations r LEFT JOIN fit_observation_provenance p ON p.sample_id = r.sample_id
                   WHERE p.sample_id IS NULL""")
            db.execSQL(
                """INSERT OR IGNORE INTO unified_measurements(origin, device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   SELECT 'band', g.device_id, g.metric, g.measured_at_ms, g.value, g.unit, g.sample_id, g.received_at_ms
                   FROM fit_generation_observations g JOIN fit_active_generations a ON a.generation_id = g.generation_id""")
            setState("historical_signature", signature)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    /**
     * Per local day, origin, metric and unit: count, minimum, mean and maximum of [startMs, endMs].
     * Days are counted from the UTC offset [offsetMs] valid at the end of the window (DST changes inside a
     * 30-day window can move a value at the day boundary by one hour); newest day first.
     */
    fun dailySummary(startMs: Long, endMs: Long, offsetMs: Long): List<UnifiedDaySummary> {
        val out = ArrayList<UnifiedDaySummary>()
        db.rawQuery(
            """SELECT (measured_at_ms + ?) / 86400000 AS day, origin, metric, unit, COUNT(*), MIN(value), AVG(value), MAX(value)
               FROM unified_measurements WHERE measured_at_ms >= ? AND measured_at_ms <= ?
               GROUP BY day, origin, metric, unit ORDER BY day DESC, origin, metric""",
            arrayOf(offsetMs.toString(), startMs.toString(), endMs.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val origin = MeasurementOrigin.entries.firstOrNull { it.code == c.getString(1) } ?: continue
                out += UnifiedDaySummary(c.getLong(0), origin, c.getString(2), c.getString(3),
                    c.getInt(4), c.getDouble(5), c.getDouble(6), c.getDouble(7))
            }
        }
        return out
    }
}

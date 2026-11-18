package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Compact primitive store; 30 days of 1 Hz readings stay far below the memory of boxed objects. */
internal class HistoryPointStore {
    private var times = LongArray(4096)
    private var values = IntArray(4096)
    var size = 0
        private set

    fun add(epochMillis: Long, value: Int) {
        if (size == times.size) { times = times.copyOf(size * 2); values = values.copyOf(size * 2) }
        times[size] = epochMillis; values[size] = value; size++
    }

    fun slice(startMs: Long, endMs: Long): List<HistoryPoint> {
        val out = ArrayList<HistoryPoint>()
        for (i in 0 until size) if (times[i] in startMs..endMs) out.add(HistoryPoint(Instant.ofEpochMilli(times[i]), values[i]))
        return out
    }

    fun pruneBefore(cutoffMs: Long) {
        var keep = 0
        for (i in 0 until size) if (times[i] >= cutoffMs) { times[keep] = times[i]; values[keep] = values[i]; keep++ }
        size = keep
    }

    fun clear() { size = 0 }
}

/**
 * Process-wide cache of the last 30 days (#79): one indexed read, then only new events are appended.
 * All ranges are slices of it, so switching 24 h / 7 d / 30 d does not touch SQLite again.
 */
internal object HistoryDataCache {
    val HORIZON: Duration = Duration.ofDays(30)
    private val pulse = HistoryPointStore()
    private val oxygen = HistoryPointStore()
    private var lastEventId = 0L
    private var loadedAtMs = 0L
    private var lastPruneMs = 0L
    private var cachedPath: String? = null

    @Synchronized fun invalidate() { pulse.clear(); oxygen.clear(); lastEventId = 0L; loadedAtMs = 0L }

    @Synchronized fun snapshot(context: Context, range: HistoryRange, now: Instant): HistorySnapshot {
        try { sync(context, now) } catch (e: Exception) { invalidate(); throw e }
        val start = range.startAt(now)
        return HistorySnapshot(range, start, now,
            pulse.slice(start.toEpochMilli(), now.toEpochMilli()),
            oxygen.slice(start.toEpochMilli(), now.toEpochMilli()))
    }

    private fun sync(context: Context, now: Instant) {
        val path = context.getDatabasePath("cirqa_measurements.db").absolutePath
        if (path != cachedPath) { invalidate(); cachedPath = path }
        CirqaDatabase(context).use { db ->
            val readable = db.readableDatabase
            val maxId = readable.rawQuery("SELECT COALESCE(MAX(id), 0) FROM events", null).use { it.moveToFirst(); it.getLong(0) }
            if (maxId < lastEventId) invalidate() // database was reset or replaced
            val sql: String
            val args: Array<String>
            if (loadedAtMs == 0L) {
                val from = CirqaDatabase.timeKey(now - HORIZON)
                sql = """SELECT id, payload_json FROM events WHERE kind = 'reading' AND service IN (6, 19)
                    AND dashboard_time >= ? AND id <= ?"""
                args = arrayOf(from, maxId.toString())
            } else {
                sql = "SELECT id, payload_json FROM events WHERE kind = 'reading' AND service IN (6, 19) AND id > ? AND id <= ?"
                args = arrayOf(lastEventId.toString(), maxId.toString())
            }
            val cutoff = (now - HORIZON).toEpochMilli()
            val nowMs = now.toEpochMilli()
            readable.rawQuery(sql, args).use { c ->
                while (c.moveToNext()) {
                    val item = try { JSONObject(c.getString(1)) } catch (_: Exception) { continue }
                    if (item.optString("type") != "reading" || item.optInt("schema") != 1) continue
                    val t = try { Instant.parse(item.getString("received_at_utc")).toEpochMilli() } catch (_: Exception) { continue }
                    if (t < cutoff || t > nowMs + 1000) continue
                    when (item.optInt("service", -1)) {
                        6 -> item.optLong("bpm", Long.MIN_VALUE).takeIf { it in 1..240 }?.let { pulse.add(t, it.toInt()) }
                        19 -> item.optLong("spo2_percent", Long.MIN_VALUE).takeIf { it in 1..100 }?.let { oxygen.add(t, it.toInt()) }
                    }
                }
            }
            lastEventId = maxId
            loadedAtMs = nowMs
            if (nowMs - lastPruneMs > 600_000L) { pulse.pruneBefore(cutoff); oxygen.pruneBefore(cutoff); lastPruneMs = nowMs }
        }
    }
}

/** Read-only, time-bounded projection of stored CIRQA heart-rate and SpO₂ readings (served from [HistoryDataCache]). */
class CirqaHistoryReader(private val context: Context) {
    fun read(range: HistoryRange, now: Instant = Instant.now()): HistorySnapshot =
        HistoryDataCache.snapshot(context, range, now)
}

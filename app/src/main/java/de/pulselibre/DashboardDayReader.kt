package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

internal data class DashboardDaySeries(
    val points: List<DayObservation>,
    val count: Int,
    val minutesObserved: Int,
    val invalid: Int,
    val goal: Long?
)

/** Streams the indexed day window; keeps original extrema and the latest point per observed minute. */
internal class DashboardDayReader(private val context: Context) {
    fun read(metric: DashboardMeasurement, day: DashboardDay, now: Instant): DashboardDaySeries {
        val buckets = linkedMapOf<Long, List<DayObservation>>()
        var invalid = 0
        var count = 0
        var goal: Long? = null
        var sameTime: Instant? = null
        val valuesAtTime = mutableSetOf<Long>()
        val query = """SELECT payload_json FROM events WHERE kind = 'reading' AND service = ?
            AND dashboard_time >= ? AND dashboard_time < ? AND dashboard_time <= ?
            ORDER BY dashboard_time, id"""
        CirqaDatabase(context).use { db ->
            db.readableDatabase.rawQuery(query, arrayOf(metric.service.toString(),
                CirqaDatabase.timeKey(day.start), CirqaDatabase.timeKey(day.end),
                CirqaDatabase.timeKey(now))).use { cursor ->
                while (cursor.moveToNext()) {
                    val event = runCatching { JSONObject(cursor.getString(0)) }.getOrNull()
                    if (event == null || event.optInt("schema") != 1 ||
                        event.optString("type") != "reading" || event.optInt("service") != metric.service) continue
                    val time = runCatching { Instant.parse(event.getString("received_at_utc")) }.getOrNull()
                    val value = metric.value(event)
                    if (time == null || !day.includes(time, now) || value == null) {
                        invalid++
                        continue
                    }
                    if (metric == DashboardMeasurement.STEPS) goal = DashboardMeasurement.GOAL.value(event)
                    if (time != sameTime) {
                        sameTime = time
                        valuesAtTime.clear()
                    }
                    if (!valuesAtTime.add(value)) continue
                    count++
                    val minute = Duration.between(day.start, time).toMinutes()
                    val point = DayObservation(time, value)
                    val candidates = buckets[minute].orEmpty() + point
                    buckets[minute] = listOf(candidates.minBy { it.value }, candidates.maxBy { it.value }, point)
                        .distinct().sortedBy { it.time }
                }
            }
        }
        return DashboardDaySeries(buckets.values.flatten(), count, buckets.size, invalid, goal)
    }
}

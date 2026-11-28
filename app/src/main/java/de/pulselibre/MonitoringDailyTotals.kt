package de.pulselibre

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * #329 – Daily totals of the band's cumulative monitoring counters (FIT 55/19
 * active_calories, 55/4 active_time), derived per activity type.
 *
 * Proven offline on real monitoring_b files (27.11.2026): within a file the counters rise strictly monotonically per
 * activity type; a new file repeats the last counter value of the previous file (16 cross-file duplicates), so values
 * must never be summed across files or records. Reset proven by two overnight tests on the S10 (27. and 28.11.2026):
 * the band closes the file at local midnight with a record stamped exactly 00:00 that still carries the FINISHED
 * day's value; the next record starts again from a small value (761 -> 4 kcal, 1427 -> 3 kcal). A record stamped
 * exactly at local midnight therefore belongs to the previous day.
 */
internal object MonitoringDailyTotals {
    enum class Reset { LOCAL_MIDNIGHT }

    /** Local day of a counter record; exactly 00:00:00.000 is the closing record of the previous day. */
    fun dayOf(timeMs: Long, zone: ZoneId): LocalDate {
        val local = Instant.ofEpochMilli(timeMs).atZone(zone)
        return if (local.toLocalTime() == java.time.LocalTime.MIDNIGHT) local.toLocalDate().minusDays(1) else local.toLocalDate()
    }

    /**
     * Band active-calorie samples with their activity type, from the active decoder generations. Counter and type
     * come from the same monitoring record but carry different record ordinals (#778); both decoder paths keep file
     * order, so they are matched per (generation, timestamp) by rank. A record without a matching type is skipped.
     */
    fun readActiveCalories(db: android.database.sqlite.SQLiteDatabase, startMs: Long, endMs: Long): List<Sample> {
        data class Row(val gen: String, val time: Long, val ordinal: Int, val metric: String, val value: Double)
        val rows = ArrayList<Row>()
        db.rawQuery("""SELECT o.generation_id, o.measured_at_ms, o.record_ordinal, o.metric, o.value
            FROM fit_generation_observations o JOIN fit_active_generations a ON a.generation_id = o.generation_id
            WHERE o.global_message_number = 55 AND o.measured_at_ms >= ? AND o.measured_at_ms <= ?
              AND (o.metric = 'active_calories' OR (o.metric = 'monitoring_activity_type' AND o.field_number = 5)
                   OR o.metric = 'monitoring_current_activity_type')""",
            arrayOf(startMs.toString(), endMs.toString())).use { c ->
            while (c.moveToNext()) rows.add(Row(c.getString(0), c.getLong(1), c.getInt(2), c.getString(3), c.getDouble(4)))
        }
        val out = ArrayList<Sample>()
        rows.groupBy { it.gen to it.time }.values.forEach { group ->
            val calories = group.filter { it.metric == "active_calories" }.sortedBy { it.ordinal }
            // one type per monitoring record (field 5 or the type component of field 24), ordered by record
            val types = group.filter { it.metric != "active_calories" }.groupBy { it.ordinal }.toSortedMap().values.map { it.first().value.toInt() }
            if (types.size != calories.size) return@forEach // ambiguous: never guess the pairing
            calories.zip(types).forEach { (cal, type) -> out.add(Sample(cal.time, type, cal.value)) }
        }
        return out
    }

    data class Sample(val timeMs: Long, val activityType: Int, val value: Double)

    data class DayTotal(val day: LocalDate, val total: Double, val activityTypes: Int, val monotonic: Boolean)

    /**
     * Per local day: for each activity type the LAST (= highest, if monotonic) counter value of that day; summed over
     * types. [DayTotal.monotonic] is false when a counter decreased within the day – then the reset assumption does
     * not hold for that day and the total must not be shown.
     */
    fun totals(samples: List<Sample>, zone: ZoneId, reset: Reset): Map<LocalDate, DayTotal> {
        check(reset == Reset.LOCAL_MIDNIGHT)
        return samples.filter { it.value.isFinite() && it.value >= 0.0 }
            .groupBy { dayOf(it.timeMs, zone) }
            .mapValues { (day, daySamples) ->
                var monotonic = true
                val lastPerType = daySamples.groupBy { it.activityType }.mapValues { (_, s) ->
                    val ordered = s.sortedBy { it.timeMs }
                    if (ordered.zipWithNext().any { (a, b) -> b.value < a.value }) monotonic = false
                    ordered.last().value
                }
                DayTotal(day, lastPerType.values.sum(), lastPerType.size, monotonic)
            }
    }
}

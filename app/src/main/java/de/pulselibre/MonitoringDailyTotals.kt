package de.pulselibre

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * #329 – PREPARED, NOT WIRED INTO THE UI. Daily totals of the band's cumulative monitoring counters (FIT 55/19
 * active_calories, 55/4 active_time), derived per activity type.
 *
 * Proven offline on real monitoring_b files (27.11.2026): within a file the counters rise strictly monotonically per
 * activity type; a new file repeats the last counter value of the previous file (16 cross-file duplicates), so values
 * must never be summed across files or records. NOT yet proven: when the counter resets (local midnight?). The reset
 * rule is therefore an explicit parameter; the product may only use [Reset.LOCAL_MIDNIGHT] after the overnight test
 * confirms it.
 */
internal object MonitoringDailyTotals {
    enum class Reset { LOCAL_MIDNIGHT }

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
            .groupBy { Instant.ofEpochMilli(it.timeMs).atZone(zone).toLocalDate() }
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

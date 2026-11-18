package de.pulselibre

import java.time.Duration

/**
 * Steps per 15-minute interval of one local day, derived from the cumulative daily step counter (#122).
 * Only differences between consecutive real observations are used; a difference is attributed to the intervals
 * between the two timestamps proportionally to elapsed time. Intervals without any covering observation stay
 * `null` (data gap) and are never drawn as zero; a counter reset or a long silence produces a gap, not a guess.
 */
internal object StepIntervals {
    val INTERVAL: Duration = Duration.ofMinutes(15)
    /** Observations further apart than this do not tell *when* the steps happened. */
    val MAX_GAP: Duration = Duration.ofMinutes(10)

    data class Series(val values: List<Double?>) {
        val observed: Int get() = values.count { it != null }
        val total: Double get() = values.sumOf { it ?: 0.0 }
        val max: Double get() = values.maxOfOrNull { it ?: 0.0 } ?: 0.0
    }

    fun bucketCount(day: DashboardDay): Int =
        ((Duration.between(day.start, day.end).toMillis() + INTERVAL.toMillis() - 1) / INTERVAL.toMillis()).toInt()

    fun compute(observations: List<DayObservation>, day: DashboardDay, maxGap: Duration = MAX_GAP): Series {
        val n = bucketCount(day)
        val values = arrayOfNulls<Double>(n)
        val obs = observations.filter { it.time >= day.start && it.time < day.end }
            .sortedWith(compareBy({ it.time }, { it.value })).distinctBy { it.time to it.value }
        val intervalMs = INTERVAL.toMillis()
        for (i in 0 until obs.size - 1) {
            val a = obs[i]; val b = obs[i + 1]
            val dt = Duration.between(a.time, b.time).toMillis()
            if (dt <= 0 || dt > maxGap.toMillis()) continue          // simultaneous or too far apart
            val delta = b.value - a.value
            if (delta < 0) continue                                    // counter reset / invalid
            val startMs = Duration.between(day.start, a.time).toMillis()
            val endMs = Duration.between(day.start, b.time).toMillis()
            var cursor = startMs
            while (cursor < endMs) {
                val bucket = (cursor / intervalMs).toInt().coerceIn(0, n - 1)
                val bucketEnd = minOf(endMs, (bucket + 1) * intervalMs)
                val share = delta.toDouble() * (bucketEnd - cursor) / dt
                values[bucket] = (values[bucket] ?: 0.0) + share
                cursor = bucketEnd
            }
        }
        return Series(values.toList())
    }
}

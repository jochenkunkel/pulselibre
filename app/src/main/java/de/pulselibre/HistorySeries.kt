package de.pulselibre

import java.time.Duration
import java.time.Instant

/** Rolling ranges use actual elapsed time, not inferred calendar-day/week summaries. */
enum class HistoryRange(val label: String, val duration: Duration) {
    DAY("24 Stunden", Duration.ofHours(24)),
    WEEK("7 Tage", Duration.ofDays(7)),
    MONTH("30 Tage", Duration.ofDays(30));

    fun startAt(end: Instant): Instant = end.minus(duration)

    /**
     * How many whole windows one can step back and still lie inside the cached horizon (30 days):
     * 24 h → 29, 7 d → 3, 30 d → 0.
     */
    fun maxOffset(horizon: Duration = Duration.ofDays(30)): Int =
        (horizon.toMillis() / duration.toMillis() - 1).toInt().coerceAtLeast(0)

    /** End of the window shown [offset] windows before [now]. */
    fun windowEnd(now: Instant, offset: Int): Instant = now.minus(duration.multipliedBy(offset.toLong()))
}

/** One locally received original observation; timestamp is phone reception time. */
data class HistoryPoint(val receivedAt: Instant, val value: Int)

data class HistorySnapshot(
    val range: HistoryRange,
    val start: Instant,
    val end: Instant,
    val heartRate: List<HistoryPoint>,
    val oxygen: List<HistoryPoint>
)

/** Fixed display resolutions sized to keep selected ranges readable on a phone chart. */
enum class HistoryResolution(
    val label: String,
    val duration: Duration,
    val majorTickEvery: Int
) {
    FIFTEEN_MINUTES("15 min", Duration.ofMinutes(15), 16),
    TWO_HOURS("2 Std.", Duration.ofHours(2), 2),
    ONE_HOUR("1 Std.", Duration.ofHours(1), 4),
    SIX_HOURS("6 Std.", Duration.ofHours(6), 4),
    ONE_DAY("1 Tag", Duration.ofDays(1), 5);

    companion object {
        /** Keep the rendered series near 24–30 summaries for legibility on phone screens. */
        fun forRange(range: HistoryRange): HistoryResolution = when (range) {
            HistoryRange.DAY -> ONE_HOUR
            HistoryRange.WEEK -> SIX_HOURS
            HistoryRange.MONTH -> ONE_DAY
        }
    }
}

/** Actual observations summarized within one time bucket; empty buckets are omitted. */
data class HistoryBucket(
    val start: Instant,
    val end: Instant,
    val minimum: Int,
    val median: Double,
    val maximum: Int,
    val count: Int,
    val mean: Double = 0.0
)

/**
 * Groups in-range stored reception snapshots into fixed elapsed-time windows.
 * Median and min/max are calculated only from observations in each bucket; missing
 * windows are not filled, and no value is interpolated between buckets.
 */
fun aggregateHistoryPoints(
    points: List<HistoryPoint>,
    start: Instant,
    end: Instant,
    resolution: HistoryResolution
): List<HistoryBucket> {
    if (end <= start) return emptyList()
    val windowMillis = Duration.between(start, end).toMillis()
    if (windowMillis <= 0L) return emptyList()
    val bucketMillis = resolution.duration.toMillis()
    val bucketCount = ((windowMillis + bucketMillis - 1L) / bucketMillis).coerceAtLeast(1L)
    val grouped = sortedMapOf<Long, MutableList<Int>>()
    points.forEach { point ->
        if (point.receivedAt < start || point.receivedAt > end) return@forEach
        val offset = Duration.between(start, point.receivedAt).toMillis()
        val index = if (point.receivedAt == end) bucketCount - 1L
            else (offset / bucketMillis).coerceIn(0L, bucketCount - 1L)
        grouped.getOrPut(index) { mutableListOf() }.add(point.value)
    }
    return grouped.map { (index, values) ->
        val sorted = values.sorted()
        val first = (sorted.size - 1) / 2
        val middle = if (sorted.size % 2 == 1) sorted[first].toDouble()
            else (sorted[first].toDouble() + sorted[first + 1].toDouble()) / 2.0
        val bucketStart = start.plusMillis(index * bucketMillis)
        val bucketEnd = minOf(end, bucketStart.plusMillis(bucketMillis))
        HistoryBucket(bucketStart, bucketEnd, sorted.first(), middle, sorted.last(), sorted.size, sorted.sumOf { it.toLong() }.toDouble() / sorted.size)
    }
}

/** Plot position only; values outside the selected time range are excluded. */
fun historyTimeFraction(time: Instant, start: Instant, end: Instant): Float? {
    if (end <= start || time < start || time > end) return null
    val span = Duration.between(start, end).toMillis()
    if (span <= 0L) return null
    return (Duration.between(start, time).toMillis().toDouble() / span).toFloat().coerceIn(0f, 1f)
}

/** Vertical chart range chosen from the observed data; labels land on round numbers, values are never altered. */
data class AxisScale(val minimum: Int, val maximum: Int) {
    val span: Int get() = maximum - minimum

    companion object {
        private val STEPS = intArrayOf(1, 2, 5, 10, 20, 25, 50, 100, 200)

        /**
         * Smallest range containing [dataMin]..[dataMax] with a little margin, at least [minSpan] wide,
         * clamped to [hardMin]..[hardMax], divisible into 4 equal intervals of a round step.
         */
        fun forData(dataMin: Int, dataMax: Int, hardMin: Int, hardMax: Int, minSpan: Int): AxisScale {
            require(hardMax > hardMin && minSpan in 4..(hardMax - hardMin))
            val lo = dataMin.coerceIn(hardMin, hardMax)
            val hi = dataMax.coerceIn(hardMin, hardMax)
            val margin = maxOf(1, (hi - lo) / 10)
            var wantedMin = (lo - margin).coerceAtLeast(hardMin)
            var wantedMax = (hi + margin).coerceAtMost(hardMax)
            if (wantedMax - wantedMin < minSpan) {
                val extra = minSpan - (wantedMax - wantedMin)
                wantedMin = (wantedMin - extra / 2).coerceAtLeast(hardMin)
                wantedMax = (wantedMin + minSpan).coerceAtMost(hardMax)
                wantedMin = (wantedMax - minSpan).coerceAtLeast(hardMin)
            }
            val needed = wantedMax - wantedMin
            val step = STEPS.firstOrNull { it * 4 >= needed } ?: STEPS.last()
            var min = Math.floorDiv(wantedMin, step) * step
            var max = min + step * 4
            while (max < wantedMax) { min += step; max += step }   // shift up until the data fits
            if (max > hardMax) { max = hardMax; min = (max - step * 4).coerceAtLeast(hardMin) }
            if (min < hardMin) { min = hardMin; max = (min + step * 4).coerceAtMost(hardMax) }
            return AxisScale(min, max)
        }
    }
}

package de.pulselibre

import java.time.Duration
import java.time.Instant

/** Rolling ranges use actual elapsed time, not inferred calendar-day/week summaries. */
enum class HistoryRange(val label: String, val duration: Duration) {
    DAY("24 Stunden", Duration.ofHours(24)),
    WEEK("7 Tage", Duration.ofDays(7)),
    MONTH("30 Tage", Duration.ofDays(30));

    fun startAt(end: Instant): Instant = end.minus(duration)
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

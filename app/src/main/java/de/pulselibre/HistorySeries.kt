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

    /**
     * Window offset that shows the window containing the start of local day [selected], clamped to what the cache holds.
     * 24 h: whole days back; 7 d: whole weeks back; 30 d: always 0.
     */
    fun offsetForDate(selected: java.time.LocalDate, today: java.time.LocalDate): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(selected, today).coerceAtLeast(0)
        val windows = (days * 24L * 3600L * 1000L) / duration.toMillis()
        return windows.toInt().coerceIn(0, maxOffset())
    }

    /** End of the window shown [offset] windows before [now]. */
    fun windowEnd(now: Instant, offset: Int): Instant = now.minus(duration.multipliedBy(offset.toLong()))
}

/**
 * One stored observation. [receivedAt] is phone reception time for live readings; inside
 * [HistorySnapshot.fitHeartRate] it is the band's measurement time (see there). Series are never mixed.
 */
data class HistoryPoint(val receivedAt: Instant, val value: Int)

data class HistorySnapshot(
    val range: HistoryRange,
    val start: Instant,
    val end: Instant,
    val heartRate: List<HistoryPoint>,
    val oxygen: List<HistoryPoint>,
    /** Heart rate imported from band FIT files; its points carry the band's MEASUREMENT time, not phone reception (#334). */
    val fitHeartRate: List<HistoryPoint> = emptyList(),
    /** True when any FIT heart-rate observation exists at all, so the card can say "nothing in this window". */
    val fitAvailable: Boolean = false,
    /** True when reading the FIT series failed; the card then says so instead of "no data" (A6-29). */
    val fitReadFailed: Boolean = false,
    /** Newest band heart-rate measurement of any time, to explain an empty window. */
    val fitNewest: Instant? = null,
    /** Band values of the window per FIT message kind; an activity is never a normal all-day history. */
    val fitKinds: Map<BandRecordKind, Int> = emptyMap(),
    /** Per day, origin and metric summary of everything in the window: live, imported and band data in one list. */
    val allData: List<UnifiedDaySummary> = emptyList()
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
    ONE_DAY("1 Tag", Duration.ofDays(1), 5),
    /** 1 h buckets over seven days: 168 ticks, one label per day. */
    ONE_HOUR_WEEK("1 Std.", Duration.ofHours(1), 24);

    companion object {
        /** Keep the rendered series near 24–30 summaries for legibility on phone screens. */
        fun forRange(range: HistoryRange): HistoryResolution = when (range) {
            HistoryRange.DAY -> ONE_HOUR
            HistoryRange.WEEK -> SIX_HOURS
            HistoryRange.MONTH -> ONE_DAY
        }

        /**
         * Band (FIT) history is dense (up to 1 Hz), so it gets a finer grid than the sparse phone readings:
         * a single 6 h or 24 h bucket would hide most days with data behind one point.
         */
        fun forFitRange(range: HistoryRange): HistoryResolution = when (range) {
            HistoryRange.DAY -> FIFTEEN_MINUTES
            HistoryRange.WEEK -> ONE_HOUR_WEEK
            HistoryRange.MONTH -> SIX_HOURS
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

/**
 * Composable aggregate of a time bucket (#79): min, max, sum and count combine exactly; the mean is always sum/count,
 * never an unweighted mean of partial means. The median is *not* composable and is deliberately absent here.
 */
data class BucketStats(val minimum: Long, val maximum: Long, val sum: Long, val count: Int) {
    init { require(count > 0) }
    val mean: Double get() = sum.toDouble() / count

    operator fun plus(other: BucketStats) = BucketStats(
        minOf(minimum, other.minimum), maxOf(maximum, other.maximum), sum + other.sum, count + other.count)

    companion object {
        fun of(values: List<Int>): BucketStats? =
            if (values.isEmpty()) null else BucketStats(values.min().toLong(), values.max().toLong(), values.sumOf { it.toLong() }, values.size)

        /** Empty input stays empty (no invented bucket). */
        fun combine(parts: List<BucketStats>): BucketStats? = parts.reduceOrNull { a, b -> a + b }
    }
}

package de.pulselibre

import java.time.Duration
import java.time.Instant

/**
 * Pure, bounded histogram data for a history heatmap (#219).
 *
 * Each time slot is returned, including empty slots. Bin probability is
 * `binCount / sampleCount`, so color intensity describes the relative
 * distribution within a slot, not its sample count or the measurement value.
 * A single shared 95th-percentile reference normalizes all occupied slots;
 * columns are deliberately not normalized independently.
 *
 * This model does not interpolate or smooth data and is safe to compute off
 * the UI thread. It contains no medical interpretation or raw timestamps.
 */
data class HistoryDensityBin(
    val lowerInclusive: Int,
    val upperInclusive: Int,
    val count: Int,
    val probability: Double,
    val intensity: Double
)

data class HistoryDensityColumn(
    val start: Instant,
    val end: Instant,
    val sampleCount: Int,
    val excludedOutsideAxis: Int,
    val bins: List<HistoryDensityBin>
)

data class HistoryDensity(
    val columns: List<HistoryDensityColumn>,
    /** Shared p95 of positive per-column bin probabilities; zero when no samples fit. */
    val referenceProbability: Double
)

/**
 * Builds fixed elapsed-time slots and value bins from individual observations.
 *
 * The selected axis is inclusive at both ends. Interior value-bin boundaries
 * belong to the bin on their right; the axis maximum belongs to the final bin.
 * A point exactly at [end] is assigned to the final time slot, matching
 * [aggregateHistoryPoints].
 */
fun aggregateHistoryDensity(
    points: List<HistoryPoint>,
    start: Instant,
    end: Instant,
    resolution: HistoryResolution,
    axis: AxisScale,
    valueBinWidth: Int
): HistoryDensity {
    require(end > start) { "History range must be positive" }
    require(resolution.duration.toMillis() > 0L)
    require(axis.maximum > axis.minimum)
    require(valueBinWidth > 0)

    val spanMillis = Duration.between(start, end).toMillis()
    require(spanMillis > 0L) { "History range is below millisecond precision" }
    val slotMillis = resolution.duration.toMillis()
    val slotCountLong = (spanMillis + slotMillis - 1L) / slotMillis
    require(slotCountLong in 1L..MAX_TIME_SLOTS.toLong()) { "Too many history time slots" }
    val slotCount = slotCountLong.toInt()
    val valueSpan = axis.maximum.toLong() - axis.minimum.toLong()
    val binCountLong = (valueSpan + valueBinWidth - 1L) / valueBinWidth
    require(binCountLong in 1L..MAX_VALUE_BINS.toLong()) { "Too many history value bins" }
    val binCount = binCountLong.toInt()

    val counts = Array(slotCount) { IntArray(binCount) }
    val excluded = IntArray(slotCount)
    points.forEach { point ->
        if (point.receivedAt < start || point.receivedAt > end) return@forEach
        val offsetMillis = Duration.between(start, point.receivedAt).toMillis()
        val slotIndex = if (point.receivedAt == end) slotCount - 1
        else (offsetMillis / slotMillis).toInt().coerceIn(0, slotCount - 1)
        if (point.value < axis.minimum || point.value > axis.maximum) {
            excluded[slotIndex]++
        } else {
            val index = if (point.value == axis.maximum) binCount - 1
            else ((point.value.toLong() - axis.minimum.toLong()) / valueBinWidth)
                .toInt().coerceIn(0, binCount - 1)
            counts[slotIndex][index]++
        }
    }

    val sampleCounts = IntArray(slotCount) { counts[it].sum() }
    val positiveProbabilities = buildList {
        for (slot in 0 until slotCount) {
            val samples = sampleCounts[slot]
            if (samples == 0) continue
            for (count in counts[slot]) {
                if (count > 0) add(count.toDouble() / samples)
            }
        }
    }.sorted()
    val reference = if (positiveProbabilities.isEmpty()) 0.0 else {
        // Nearest-rank p95, chosen once across the entire chart.
        val index = kotlin.math.ceil(positiveProbabilities.size * 0.95).toInt()
            .coerceAtLeast(1) - 1
        positiveProbabilities[index]
    }

    val columns = List(slotCount) { slot ->
        val columnStart = start.plusMillis(slot.toLong() * slotMillis)
        val columnEnd = minOf(end, columnStart.plusMillis(slotMillis))
        val samples = sampleCounts[slot]
        val bins = List(binCount) { bin ->
            val lower = axis.minimum.toLong() + bin.toLong() * valueBinWidth
            val upper = minOf(axis.maximum.toLong(), lower + valueBinWidth)
            val count = counts[slot][bin]
            val probability = if (samples == 0) 0.0 else count.toDouble() / samples
            HistoryDensityBin(
                lowerInclusive = lower.toInt(),
                upperInclusive = upper.toInt(),
                count = count,
                probability = probability,
                intensity = if (reference == 0.0) 0.0 else (probability / reference).coerceIn(0.0, 1.0)
            )
        }
        HistoryDensityColumn(columnStart, columnEnd, samples, excluded[slot], bins)
    }
    return HistoryDensity(columns, reference)
}

private const val MAX_TIME_SLOTS = 4096
private const val MAX_VALUE_BINS = 256

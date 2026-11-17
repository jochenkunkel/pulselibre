package de.pulselibre

/**
 * Pure, side-effect-free one-minute aggregation for future continuous CIRQA recording.
 *
 * Input is a validated, timestamped sample stream. This does not initiate BLE traffic,
 * replace the canonical raw-event store, or imply that CIRQA sends continuously.
 */
object CirqaMinuteAggregation {
    data class HeartRate(val timestampMillis: Long, val bpm: Int)
    data class HeartRateMinute(
        val minuteStartMillis: Long,
        val count: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val meanBpm: Double,
        val lastBpm: Int
    )

    /** Invalid values are ignored, never silently converted into zero measurements. */
    fun heartRate(samples: Iterable<HeartRate>): List<HeartRateMinute> =
        samples.asSequence()
            .filter { it.timestampMillis >= 0 && it.bpm in 1..240 }
            .groupBy { (it.timestampMillis / 60_000L) * 60_000L }
            .toSortedMap()
            .map { (minute, readings) ->
                val ordered = readings.sortedBy { it.timestampMillis }
                HeartRateMinute(
                    minuteStartMillis = minute,
                    count = ordered.size,
                    minBpm = ordered.minOf { it.bpm },
                    maxBpm = ordered.maxOf { it.bpm },
                    meanBpm = ordered.sumOf { it.bpm.toLong() }.toDouble() / ordered.size,
                    lastBpm = ordered.last().bpm
                )
            }

    data class StepCounter(val timestampMillis: Long, val cumulativeSteps: Long)
    data class StepMinute(val minuteStartMillis: Long, val steps: Long)

    /**
     * Convert cumulative counters to non-negative increments. A decrease (reset, wrap,
     * or device/session change) is ambiguous: discard that transition, then rebase.
     * Do not attribute the first absolute reading to the current minute.
     *
     * A delta spanning more than maxGapMillis is ambiguous and is not assigned to
     * one minute. The caller should mark missing intervals separately.
     */
    fun stepDeltas(
        samples: Iterable<StepCounter>,
        maxGapMillis: Long = 120_000L
    ): List<StepMinute> {
        require(maxGapMillis > 0)
        val totals = sortedMapOf<Long, Long>()
        var previous: StepCounter? = null
        for (current in samples.asSequence()
            .filter { it.timestampMillis >= 0 && it.cumulativeSteps >= 0 }
            .sortedBy { it.timestampMillis }) {
            val before = previous
            if (before != null) {
                val elapsed = current.timestampMillis - before.timestampMillis
                if (elapsed > 0 && elapsed <= maxGapMillis &&
                    current.cumulativeSteps >= before.cumulativeSteps) {
                    val delta = current.cumulativeSteps - before.cumulativeSteps
                    val minute = (current.timestampMillis / 60_000L) * 60_000L
                    totals[minute] = Math.addExact(totals[minute] ?: 0L, delta)
                }
            }
            previous = current
        }
        return totals.map { (minute, steps) -> StepMinute(minute, steps) }
    }
}

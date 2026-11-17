package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaMinuteAggregationTest {
    @Test fun heartRateKeepsRealMinuteBoundariesAndLastTimestamp() {
        val result = CirqaMinuteAggregation.heartRate(listOf(
            CirqaMinuteAggregation.HeartRate(61_000, 90),
            CirqaMinuteAggregation.HeartRate(1_000, 60),
            CirqaMinuteAggregation.HeartRate(60_000, 80),
            CirqaMinuteAggregation.HeartRate(62_000, 0),
            CirqaMinuteAggregation.HeartRate(-1, 100)
        ))
        assertEquals(2, result.size)
        assertEquals(0L, result[0].minuteStartMillis)
        assertEquals(60, result[0].lastBpm)
        assertEquals(60_000L, result[1].minuteStartMillis)
        assertEquals(2, result[1].count)
        assertEquals(80, result[1].minBpm)
        assertEquals(90, result[1].maxBpm)
        assertEquals(85.0, result[1].meanBpm, 0.0001)
        assertEquals(90, result[1].lastBpm)
    }

    @Test fun stepsUseDeltasNotAbsoluteCountersAndDoNotInventGaps() {
        val result = CirqaMinuteAggregation.stepDeltas(listOf(
            CirqaMinuteAggregation.StepCounter(0, 1000),
            CirqaMinuteAggregation.StepCounter(30_000, 1010),
            CirqaMinuteAggregation.StepCounter(70_000, 1025),
            CirqaMinuteAggregation.StepCounter(400_000, 2000),
            CirqaMinuteAggregation.StepCounter(410_000, 2003)
        ))
        assertEquals(listOf(
            CirqaMinuteAggregation.StepMinute(0, 10),
            CirqaMinuteAggregation.StepMinute(60_000, 15),
            CirqaMinuteAggregation.StepMinute(360_000, 3)
        ), result)
    }

    @Test fun counterResetAndDuplicateTimestampDoNotCreateSteps() {
        val result = CirqaMinuteAggregation.stepDeltas(listOf(
            CirqaMinuteAggregation.StepCounter(0, 500),
            CirqaMinuteAggregation.StepCounter(10_000, 5),
            CirqaMinuteAggregation.StepCounter(10_000, 999),
            CirqaMinuteAggregation.StepCounter(20_000, 8),
            CirqaMinuteAggregation.StepCounter(30_000, 12)
        ))
        assertEquals(listOf(CirqaMinuteAggregation.StepMinute(0, 4)), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidGapIsRejected() {
        CirqaMinuteAggregation.stepDeltas(emptyList(), 0)
    }
}

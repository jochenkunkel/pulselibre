package de.pulselibre

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class HistorySeriesTest {
    private val end = Instant.parse("2026-11-17T12:00:00Z")

    @Test fun rangesAreRollingAndExplicit() {
        assertEquals(24 * 60 * 60, HistoryRange.DAY.duration.seconds)
        assertEquals(7 * 24 * 60 * 60, HistoryRange.WEEK.duration.seconds)
        assertEquals(30L * 24 * 60 * 60, HistoryRange.MONTH.duration.seconds)
        assertEquals(end.minusSeconds(3600), HistoryRange.DAY.startAt(end).plusSeconds(23 * 60 * 60))
    }

    @Test fun timeCoordinatesUseExactRangeAndExcludeOutOfRangeSamples() {
        val start = HistoryRange.DAY.startAt(end)
        assertEquals(0f, historyTimeFraction(start, start, end)!!, 0f)
        assertEquals(0.5f, historyTimeFraction(start.plusSeconds(12 * 3600), start, end)!!, 0.00001f)
        assertEquals(1f, historyTimeFraction(end, start, end)!!, 0f)
        assertNull(historyTimeFraction(start.minusSeconds(1), start, end))
        assertNull(historyTimeFraction(end.plusSeconds(1), start, end))
        assertNull(historyTimeFraction(start, end, start))
    }

    @Test fun phoneChartUsesAboutOneSummaryPerHourOrDay() {
        assertEquals(60 * 60, HistoryResolution.forRange(HistoryRange.DAY).duration.seconds)
        assertEquals(6 * 60 * 60, HistoryResolution.forRange(HistoryRange.WEEK).duration.seconds)
        assertEquals(24 * 60 * 60, HistoryResolution.forRange(HistoryRange.MONTH).duration.seconds)
    }

    @Test fun minuteSamplesAggregateToTwentyFourReadableDailyBuckets() {
        val start = HistoryRange.DAY.startAt(end)
        val points = (0 until 24 * 60).map { minute ->
            HistoryPoint(start.plusSeconds(minute * 60L), 70 + minute % 10)
        }

        val buckets = aggregateHistoryPoints(points, start, end, HistoryResolution.forRange(HistoryRange.DAY))

        assertEquals(24, buckets.size)
        assertTrue(buckets.all { it.count == 60 })
        assertEquals(70, buckets.first().minimum)
        assertEquals(79, buckets.first().maximum)
        assertEquals(74.5, buckets.first().median, 0.0)
    }

    @Test fun aggregationKeepsObservedRangeAndMedianWithoutCreatingEmptyWindows() {
        val start = end.minusSeconds(3600)
        val points = listOf(
            HistoryPoint(start, 60),
            HistoryPoint(start.plusSeconds(30), 100),
            HistoryPoint(start.plusSeconds(60), 80),
            HistoryPoint(start.plusSeconds(900), 70),
            HistoryPoint(start.minusSeconds(1), 250),
            HistoryPoint(end.plusSeconds(1), 250)
        )

        val buckets = aggregateHistoryPoints(points, start, end, HistoryResolution.FIFTEEN_MINUTES)

        assertEquals(2, buckets.size)
        assertEquals(3, buckets[0].count)
        assertEquals(60, buckets[0].minimum)
        assertEquals(80.0, buckets[0].median, 0.0)
        assertEquals(100, buckets[0].maximum)
        assertEquals(1, buckets[1].count)
        assertEquals(70.0, buckets[1].median, 0.0)
        assertEquals(start, buckets[0].start)
        assertEquals(start.plusSeconds(900), buckets[1].start)
    }

    @Test fun aSampleExactlyAtRangeEndIsKeptInTheLastWindow() {
        val start = end.minusSeconds(3600)
        val buckets = aggregateHistoryPoints(
            listOf(HistoryPoint(end, 75)), start, end, HistoryResolution.FIFTEEN_MINUTES
        )
        assertEquals(1, buckets.size)
        assertEquals(start.plusSeconds(2700), buckets.single().start)
        assertEquals(end, buckets.single().end)
        assertEquals(75.0, buckets.single().median, 0.0)
    }
}

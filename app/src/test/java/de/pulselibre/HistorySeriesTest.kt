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
}

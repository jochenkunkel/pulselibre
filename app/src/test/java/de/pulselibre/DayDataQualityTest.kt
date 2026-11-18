package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayDataQualityTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 18), zone)
    private fun at(h: Int, m: Int) = day.date.atTime(h, m).atZone(zone).toInstant()

    @Test fun noObservationsIsOneGapNotZeroValues() {
        val q = DayDataQuality.assess(emptyList(), day, at(10, 0))
        assertEquals(0, q.observedMinutes); assertEquals(1, q.gapCount)
        assertEquals(Duration.ofHours(10), q.longestGap); assertEquals(0.0, q.coveragePercent!!, 0.0)
    }
    @Test fun gapsAreCountedAndDuplicatesIgnored() {
        val times = listOf(at(8, 0), at(8, 0), at(8, 1), at(8, 2), at(9, 0), at(9, 1))
        val q = DayDataQuality.assess(times, day, at(9, 2))
        assertEquals(5, q.observedMinutes)
        assertEquals(2, q.gapCount) // 00:00-08:00 and 08:02-09:00
        assertEquals(Duration.ofHours(8), q.longestGap)
    }
    @Test fun futureAndForeignDayTimesAreExcluded() {
        val times = listOf(at(11, 0), day.start.minusSeconds(60), day.end.plusSeconds(1))
        assertEquals(0, DayDataQuality.assess(times, day, at(10, 0)).observedMinutes)
    }
    @Test fun dayNotStartedHasNoCoverage() {
        val q = DayDataQuality.assess(emptyList(), day, day.start.minusSeconds(5))
        assertNull(q.coveragePercent); assertEquals("Tag noch nicht begonnen", q.summary())
    }
    @Test fun dstDayUsesRealElapsedMinutes() {
        val spring = DashboardDay(LocalDate.of(2026, 3, 29), zone)
        val q = DayDataQuality.assess(emptyList(), spring, spring.end)
        assertEquals(23 * 60, q.availableMinutes)
    }
    @Test fun summaryIsGerman() {
        val q = DayDataQuality.assess(listOf(at(0, 0), at(0, 1)), day, at(0, 9))
        assertTrue(q.summary().startsWith("Abdeckung 20 %"))
    }
}

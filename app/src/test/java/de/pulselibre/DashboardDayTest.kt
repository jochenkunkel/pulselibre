package de.pulselibre

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DashboardDayTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test fun springAndAutumnUseActualElapsedTime() {
        val spring = DashboardDay(LocalDate.of(2026, 3, 29), berlin)
        val autumn = DashboardDay(LocalDate.of(2026, 10, 25), berlin)
        assertEquals(23L, Duration.between(spring.start, spring.end).toHours())
        assertEquals(25L, Duration.between(autumn.start, autumn.end).toHours())
        assertEquals(0f, spring.fraction(spring.start), 0f)
        assertEquals(1f, spring.fraction(spring.end), 0f)
        val first = autumn.fraction(Instant.parse("2026-10-25T00:30:00Z"))
        val second = autumn.fraction(Instant.parse("2026-10-25T01:30:00Z"))
        assertEquals(1f / 25, second - first, 0.00001f)
    }

    @Test fun axisTicksKeepLocalClockLabelsAcrossTimeChanges() {
        val spring = DashboardDay(LocalDate.of(2026, 3, 29), berlin)
        val autumn = DashboardDay(LocalDate.of(2026, 10, 25), berlin)
        val normal = DashboardDay(LocalDate.of(2026, 11, 18), berlin)
        assertEquals(listOf(0f, 5f / 23, 11f / 23, 17f / 23, 1f), spring.axisFractions())
        assertEquals(listOf(0f, 7f / 25, 13f / 25, 19f / 25, 1f), autumn.axisFractions())
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), normal.axisFractions())
    }

    @Test fun availableMinutesRespectDaylightSavingAndDoNotIncludeFutureBuckets() {
        val spring = DashboardDay(LocalDate.of(2026, 3, 29), berlin)
        val autumn = DashboardDay(LocalDate.of(2026, 10, 25), berlin)
        assertEquals(0, spring.availableMinutes(spring.start.minusNanos(1)))
        assertEquals(1, spring.availableMinutes(spring.start))
        assertEquals(1, spring.availableMinutes(spring.start.plusSeconds(59)))
        assertEquals(2, spring.availableMinutes(spring.start.plusSeconds(60)))
        assertEquals(1380, spring.availableMinutes(spring.end))
        assertEquals(1500, autumn.availableMinutes(autumn.end.plusSeconds(60)))
    }

    @Test fun localDayExcludesFutureAndNextMidnightAndDependsOnZone() {
        val date = LocalDate.of(2026, 11, 18)
        val local = DashboardDay(date, berlin)
        val utc = DashboardDay(date, ZoneId.of("UTC"))
        assertTrue(local.includes(local.start, local.start))
        assertFalse(utc.includes(local.start, utc.end))
        assertFalse(local.includes(local.start.plusSeconds(1), local.start))
        assertFalse(local.includes(local.end, local.end))
    }
}

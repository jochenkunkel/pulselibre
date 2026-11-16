package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExportDateRangeTest {
    @Test fun sameDayIncludesEntireDay() {
        val window = exportWindowForDates(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15), ZoneId.of("UTC"))
        assertEquals(86_400_000L, window.endExclusiveEpochMillis - window.startInclusiveEpochMillis)
    }

    @Test fun daylightSavingSpringDayIs23Hours() {
        val window = exportWindowForDates(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 3, 29), ZoneId.of("Europe/Berlin"))
        assertEquals(23L * 60 * 60 * 1000, window.endExclusiveEpochMillis - window.startInclusiveEpochMillis)
    }

    @Test fun daylightSavingAutumnDayIs25Hours() {
        val window = exportWindowForDates(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25), ZoneId.of("Europe/Berlin"))
        assertEquals(25L * 60 * 60 * 1000, window.endExclusiveEpochMillis - window.startInclusiveEpochMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedDatesRejected() {
        exportWindowForDates(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 15), ZoneId.of("UTC"))
    }
}

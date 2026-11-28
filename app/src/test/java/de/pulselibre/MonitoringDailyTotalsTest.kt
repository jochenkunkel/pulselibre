package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonitoringDailyTotalsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(d: Int, h: Int) = LocalDate.of(2026, 11, d).atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
    private fun s(d: Int, h: Int, type: Int, v: Double) = MonitoringDailyTotals.Sample(at(d, h), type, v)

    @Test fun lastValuePerTypeIsSummedNeverAllValues() {
        // generic (0) 603 -> 717, walking (6) 369 -> 761, as in the real file; repeated carry-over value at the file start.
        val t = MonitoringDailyTotals.totals(listOf(s(27, 6, 0, 603.0), s(27, 9, 0, 717.0), s(27, 9, 0, 717.0),
            s(27, 6, 6, 369.0), s(27, 14, 6, 761.0)), zone, MonitoringDailyTotals.Reset.LOCAL_MIDNIGHT)
        val day = t.getValue(LocalDate.of(2026, 11, 27))
        assertEquals(717.0 + 761.0, day.total, 0.0)
        assertEquals(2, day.activityTypes)
        assertTrue(day.monotonic)
    }

    @Test fun daysAreSeparatedAndDecreaseIsFlagged() {
        val t = MonitoringDailyTotals.totals(listOf(s(26, 22, 0, 900.0), s(27, 1, 0, 10.0), s(27, 3, 0, 5.0)),
            zone, MonitoringDailyTotals.Reset.LOCAL_MIDNIGHT)
        assertEquals(900.0, t.getValue(LocalDate.of(2026, 11, 26)).total, 0.0)
        assertFalse(t.getValue(LocalDate.of(2026, 11, 27)).monotonic) // counter fell within the day: do not show
    }

    @Test fun invalidValuesAreIgnored() {
        val t = MonitoringDailyTotals.totals(listOf(s(27, 8, 0, Double.NaN), s(27, 9, 0, -1.0), s(27, 10, 0, 50.0)),
            zone, MonitoringDailyTotals.Reset.LOCAL_MIDNIGHT)
        assertEquals(50.0, t.getValue(LocalDate.of(2026, 11, 27)).total, 0.0)
    }

    @Test fun recordAtMidnightClosesThePreviousDay() {
        // S10 28.11.: walking 1427 at 23:49, 1427 again stamped 00:00 (closing record), then 3 at 07:34
        val midnight = LocalDate.of(2026, 11, 28).atStartOfDay(zone).toInstant().toEpochMilli()
        val t = MonitoringDailyTotals.totals(listOf(s(27, 23, 6, 1427.0), MonitoringDailyTotals.Sample(midnight, 6, 1427.0),
            MonitoringDailyTotals.Sample(midnight, 0, 193.0), s(28, 7, 6, 3.0)), zone, MonitoringDailyTotals.Reset.LOCAL_MIDNIGHT)
        assertEquals(1427.0 + 193.0, t.getValue(LocalDate.of(2026, 11, 27)).total, 0.0)
        val today = t.getValue(LocalDate.of(2026, 11, 28))
        assertEquals(3.0, today.total, 0.0)
        assertTrue(today.monotonic)
    }
}

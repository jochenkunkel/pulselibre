package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StepIntervalsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 18), zone)
    private fun at(h: Int, m: Int, s: Int = 0) = day.date.atTime(h, m, s).atZone(zone).toInstant()
    private fun o(h: Int, m: Int, v: Long, s: Int = 0) = DayObservation(at(h, m, s), v)
    private fun bucket(h: Int, m: Int) = (h * 60 + m) / 15

    @Test fun twelveHundredStepsInSeparateWindowsKeepTheirTotalAndPosition() {
        val obs = listOf(o(8, 0, 100), o(8, 5, 500), o(8, 10, 600),   // 500 steps in 08:00–08:15
            o(12, 0, 600), o(12, 5, 900), o(12, 10, 1000),               // 400 in 12:00–12:15 (counter standing still before)
            o(18, 0, 1000), o(18, 5, 1300))                              // 300 in 18:00–18:15
        val s = StepIntervals.compute(obs, day)
        assertEquals(96, s.values.size)
        assertEquals(500.0, s.values[bucket(8, 0)]!!, 1e-6)
        assertEquals(400.0, s.values[bucket(12, 0)]!!, 1e-6)
        assertEquals(300.0, s.values[bucket(18, 0)]!!, 1e-6)
        assertEquals(1200.0, s.total, 1e-6)
    }
    @Test fun zeroActivityIsDistinctFromMissingData() {
        val s = StepIntervals.compute(listOf(o(9, 0, 50), o(9, 5, 50), o(9, 10, 50)), day)
        assertEquals(0.0, s.values[bucket(9, 0)]!!, 0.0)
        assertNull(s.values[bucket(9, 15)]); assertNull(s.values[bucket(3, 0)])
    }
    @Test fun longSilenceIsAGapNotAGuess() {
        val s = StepIntervals.compute(listOf(o(8, 0, 100), o(11, 0, 2100)), day)
        assertEquals(0, s.observed); assertEquals(0.0, s.total, 0.0)
    }
    @Test fun counterResetAndDuplicatesAreIgnored() {
        val s = StepIntervals.compute(listOf(o(8, 0, 900), o(8, 5, 900), o(8, 5, 900), o(8, 6, 20), o(8, 8, 80)), day)
        // 900→900 is zero activity, 900→20 is a reset and skipped, only 20→80 (60 steps) counts
        assertEquals(60.0, s.values[bucket(8, 0)]!!, 1e-6)
        assertEquals(60.0, s.total, 1e-6)
    }
    @Test fun incrementSpanningABoundaryIsSplitByTime() {
        val s = StepIntervals.compute(listOf(o(8, 10, 0), o(8, 20, 100)), day) // 10 min across 08:15 boundary, 5/5
        assertEquals(50.0, s.values[bucket(8, 0)]!!, 1e-6); assertEquals(50.0, s.values[bucket(8, 15)]!!, 1e-6)
    }
    @Test fun emptyInputAndForeignDayObservationsGiveNoBars() {
        assertEquals(0, StepIntervals.compute(emptyList(), day).observed)
        val other = DayObservation(day.start.minus(Duration.ofHours(1)), 5)
        assertEquals(0, StepIntervals.compute(listOf(other, DayObservation(day.end.plusSeconds(1), 9)), day).observed)
    }
    @Test fun dstDaysHaveRealIntervalCounts() {
        assertEquals(92, StepIntervals.bucketCount(DashboardDay(LocalDate.of(2026, 3, 29), zone)))
        assertEquals(100, StepIntervals.bucketCount(DashboardDay(LocalDate.of(2026, 10, 25), zone)))
        assertEquals(96, StepIntervals.bucketCount(day))
    }
    @Test fun unsortedInputGivesTheSameResult() {
        val a = listOf(o(8, 0, 0), o(8, 5, 120), o(8, 10, 200))
        assertEquals(StepIntervals.compute(a, day), StepIntervals.compute(a.reversed(), day))
    }
}

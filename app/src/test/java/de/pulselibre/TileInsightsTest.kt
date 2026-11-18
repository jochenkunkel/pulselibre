package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TileInsightsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 18), zone)
    private val now = day.date.atTime(12, 0).atZone(zone).toInstant()
    private fun at(h: Int, m: Int = 0) = day.date.atTime(h, m).atZone(zone).toInstant()
    private fun o(h: Int, v: Long) = DayObservation(at(h), v)

    @Test fun instantaneousMetricGivesRangeAverageAndHourlyMeans() {
        val i = TileInsightCalc.summarize(DashboardMeasurement.STRESS, listOf(o(8, 20), o(8, 40), o(10, 60)), day, now, emptyList())
        assertEquals(20.0, i.min!!, 0.0); assertEquals(60.0, i.max!!, 0.0); assertEquals(40.0, i.average!!, 1e-9)
        assertEquals(30.0, i.hourly[8]!!, 1e-9); assertEquals(60.0, i.hourly[10]!!, 1e-9); assertNull(i.hourly[9])
        assertEquals(60.0, i.current!!, 0.0)
    }
    @Test fun cumulativeMetricUsesCounterValuesNotMeans() {
        val i = TileInsightCalc.summarize(DashboardMeasurement.CALORIES_TOTAL, listOf(o(8, 500), o(8, 700), o(11, 1500)), day, now, emptyList())
        assertTrue(i.cumulative); assertEquals(700.0, i.hourly[8]!!, 0.0); assertEquals(1500.0, i.average!!, 0.0)
    }
    @Test fun futureAndOtherDayObservationsAreIgnored() {
        val future = DayObservation(at(13), 99); val other = DayObservation(day.start.minus(Duration.ofHours(1)), 5)
        val i = TileInsightCalc.summarize(DashboardMeasurement.STRESS, listOf(o(9, 10), future, other), day, now, emptyList())
        assertEquals(1, i.count)
    }
    @Test fun weekAverageNeedsAtLeastThreeDaysAndComparesToToday() {
        assertNull(TileInsightCalc.summarize(DashboardMeasurement.STRESS, listOf(o(9, 50)), day, now, listOf(40.0, 44.0)).weekAtThisTime)
        val i = TileInsightCalc.summarize(DashboardMeasurement.STRESS, listOf(o(9, 50)), day, now, listOf(40.0, 44.0, 36.0))
        assertEquals(40.0, i.weekAtThisTime!!, 1e-9); assertEquals(10.0, i.deltaToWeek!!, 1e-9); assertEquals(3, i.weekDays)
    }
    @Test fun spo2BandsSumToHundredAndUseOrientationThresholds() {
        val i = TileInsightCalc.summarize(DashboardMeasurement.OXYGEN, listOf(o(8, 99), o(8, 95), o(9, 92), o(9, 89)), day, now, emptyList())
        val b = i.bands!!
        assertEquals(50.0, b.first, 1e-9); assertEquals(25.0, b.second, 1e-9); assertEquals(25.0, b.third, 1e-9)
        assertNull(TileInsightCalc.summarize(DashboardMeasurement.STRESS, listOf(o(8, 50)), day, now, emptyList()).bands)
    }
    @Test fun noDataGivesNullsNotZeros() {
        val i = TileInsightCalc.summarize(DashboardMeasurement.STRESS, emptyList(), day, now, emptyList())
        assertNull(i.min); assertNull(i.average); assertNull(i.current); assertTrue(i.hourly.all { it == null })
    }
    @Test fun pastWindowsAreSevenDaysAroundTheSameLocalTime() {
        val w = TileInsightCalc.pastWindows(now, zone)
        assertEquals(7, w.size); assertEquals(now.atZone(zone).minusDays(1).toInstant().minus(Duration.ofMinutes(30)), w[0].first)
        assertEquals(Duration.ofMinutes(60), Duration.between(w[3].first, w[3].second))
    }
    @Test fun formatUsesGermanSeparators() {
        assertEquals("1.867", TileInsightCalc.format(1867.0, "")); assertEquals("27,5 %", TileInsightCalc.format(27.5, "%"))
    }
}

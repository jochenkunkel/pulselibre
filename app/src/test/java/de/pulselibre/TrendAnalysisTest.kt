package de.pulselibre

import de.pulselibre.TrendAnalysis.Direction
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TrendAnalysisTest {
    private val today = LocalDate.of(2026, 11, 18)
    private fun series(from: Int, to: Int, value: (Int) -> Double?) =
        (from..to).mapNotNull { d -> value(d)?.let { today.minusDays(d.toLong()) to it } }.toMap()

    @Test fun noDataIsUnknownNotStable() {
        val r = TrendAnalysis.analyze(emptyMap(), today, 7, 1.0)
        assertEquals(Direction.UNKNOWN, r.direction); assertNull(r.delta); assertEquals(0, r.current.validDays)
    }
    @Test fun singleDayIsInsufficient() {
        val r = TrendAnalysis.analyze(mapOf(today.minusDays(1) to 60.0), today, 7, 1.0)
        assertEquals(Direction.UNKNOWN, r.direction); assertTrue(r.reason.contains("mindestens 4"))
    }
    @Test fun missingBaselineIsReportedSeparately() {
        val r = TrendAnalysis.analyze(series(1, 7) { 60.0 }, today, 7, 1.0)
        assertEquals(Direction.UNKNOWN, r.direction); assertTrue(r.reason.startsWith("Vergleichszeitraum"))
    }
    @Test fun clearDecreaseIsDownWithDelta() {
        val v = series(1, 14) { if (it <= 7) 56.0 else 62.0 }
        val r = TrendAnalysis.analyze(v, today, 7, 1.0)
        assertEquals(Direction.DOWN, r.direction); assertEquals(-6.0, r.delta!!, 1e-9)
    }
    @Test fun smallDifferenceBelowFloorIsStable() {
        val v = series(1, 14) { if (it <= 7) 60.5 else 60.0 }
        assertEquals(Direction.STABLE, TrendAnalysis.analyze(v, today, 7, 1.0).direction)
    }
    @Test fun medianIgnoresSingleOutlier() {
        val v = series(1, 14) { if (it == 3) 150.0 else if (it <= 7) 60.0 else 60.0 }
        val r = TrendAnalysis.analyze(v, today, 7, 1.0)
        assertEquals(60.0, r.current.median!!, 1e-9); assertEquals(Direction.STABLE, r.direction)
    }
    @Test fun gapsAreCountedAndMinimumDaysEnforced() {
        val v = series(1, 14) { if (it <= 7 && it % 2 == 0) null else 60.0 } // 4 valid of 7 in current
        val r = TrendAnalysis.analyze(v, today, 7, 1.0)
        assertEquals(4, r.current.validDays); assertEquals(3, r.current.missingDays)
        assertNotEquals(Direction.UNKNOWN, r.direction)
        val fewer = series(1, 14) { if (it <= 7 && it > 3) null else 60.0 } // 3 valid
        assertEquals(Direction.UNKNOWN, TrendAnalysis.analyze(fewer, today, 7, 1.0).direction)
    }
    @Test fun todayIsExcludedAndNonFiniteIgnored() {
        val v = series(1, 14) { 60.0 } + (today to 200.0) + (today.minusDays(2) to Double.NaN)
        val r = TrendAnalysis.analyze(v, today, 7, 1.0)
        assertEquals(6, r.current.validDays); assertEquals(60.0, r.current.median!!, 1e-9)
    }
    @Test fun minimumDaysScaleWithWindow() {
        assertEquals(4, TrendAnalysis.minValidDays(7)); assertEquals(14, TrendAnalysis.minValidDays(28))
        assertEquals(45, TrendAnalysis.minValidDays(90))
    }
}

class DailyAggregationTest {
    private val berlin = java.time.ZoneId.of("Europe/Berlin")
    @Test fun groupsByLocalDayNotUtcDay() {
        val rows = listOf(
            java.time.Instant.parse("2026-11-17T23:30:00Z") to 60.0, // 00:30 Berlin on the 18th
            java.time.Instant.parse("2026-11-18T10:00:00Z") to 64.0
        )
        val g = DailyAggregation.group(rows, berlin, steps = false)
        assertEquals(1, g.size); assertEquals(62.0, g[LocalDate.of(2026, 11, 18)]!!, 1e-9)
    }
    @Test fun stepsUseHighestCounterOfTheDay() {
        val rows = listOf(java.time.Instant.parse("2026-11-18T08:00:00Z") to 1000.0,
            java.time.Instant.parse("2026-11-18T18:00:00Z") to 7000.0)
        assertEquals(7000.0, DailyAggregation.group(rows, berlin, steps = true).values.single(), 0.0)
    }
    @Test fun exactRepeatsOfOneReadingCountOnce() {
        val t = java.time.Instant.parse("2026-11-18T08:00:00Z")
        val rows = listOf(t to 50.0, t to 50.0, t to 50.0, t.plusSeconds(60) to 70.0, t.plusSeconds(120) to 72.0)
        // Without de-duplication the median would be 50; the three distinct readings give 70.
        assertEquals(70.0, DailyAggregation.group(rows, berlin, steps = false).values.single(), 0.0)
        // Same time, different value is no repeat: both stay.
        assertEquals(55.0, DailyAggregation.group(listOf(t to 50.0, t to 60.0), berlin, steps = false).values.single(), 0.0)
    }
    @Test fun daylightSavingDaysKeepTheirLocalBoundaries() {
        // 29.03.2026 has 23 h, 25.10.2026 has 25 h in Berlin: values just before local midnight stay on their day.
        val springLate = java.time.Instant.parse("2026-03-29T21:59:00Z") // 23:59 CEST
        val autumnLate = java.time.Instant.parse("2026-10-25T22:59:00Z") // 23:59 CET
        val g = DailyAggregation.group(listOf(springLate to 60.0, autumnLate to 62.0), berlin, steps = false)
        assertEquals(60.0, g[LocalDate.of(2026, 3, 29)]!!, 0.0)
        assertEquals(62.0, g[LocalDate.of(2026, 10, 25)]!!, 0.0)
        assertEquals(2, g.size)
    }
}

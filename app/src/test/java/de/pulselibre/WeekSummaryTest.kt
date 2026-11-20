package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeekSummaryTest {
    private val today = LocalDate.of(2026, 11, 20)
    private fun d(back: Long) = today.minusDays(back)
    private val fmt: (Double) -> String = { Math.round(it).toString() }

    @Test fun averagesOnlyCoverDaysWithDataAndMissingDaysAreNeverZero() {
        val steps = mapOf(d(1) to 8000.0, d(2) to 6000.0, d(4) to 10000.0)
        val r = WeekSummary.build(steps, emptyMap(), today, null, fmt)
        assertEquals("8000", r.rows[0].value)
        assertEquals(3, r.rows[0].validDays)
        assertEquals("3 von 7 Tagen", r.rows[0].coverage())
        assertTrue(r.rows[1].value!!.startsWith("24000"))
    }

    @Test fun todayAndOlderThanSevenDaysAreExcluded() {
        val steps = mapOf(today to 99999.0, d(8) to 99999.0, d(7) to 1000.0)
        val r = WeekSummary.build(steps, emptyMap(), today, null, fmt)
        assertEquals("1000", r.rows[0].value)
        assertEquals(1, r.rows[0].validDays)
    }

    @Test fun noDataIsStatedNotShownAsZero() {
        val r = WeekSummary.build(emptyMap(), emptyMap(), today, 8000, fmt)
        assertTrue(r.rows.all { it.value == null && it.validDays == 0 })
        assertEquals("Ø Schritte pro Tag: keine Daten", r.rows[0].text())
        assertNull(r.rows[0].detail)
    }

    @Test fun restingPulseUsesOnlyReportedPositiveValues() {
        val rest = mapOf(d(1) to 58.0, d(2) to 0.0, d(3) to Double.NaN, d(5) to 62.0)
        val r = WeekSummary.build(emptyMap(), rest, today, null, fmt)
        assertEquals("60 bpm", r.rows[2].value)
        assertEquals(2, r.rows[2].validDays)
    }

    @Test fun goalStatementCountsDaysAtOrAboveGoalOverDaysWithData() {
        val steps = mapOf(d(1) to 8100.0, d(2) to 8099.0, d(3) to 9000.0)
        val r = WeekSummary.build(steps, emptyMap(), today, 8100, fmt)
        assertEquals("Ziel (8100) erreicht an 2 von 3 Tagen mit Daten.", r.rows[0].detail)
        assertNull(WeekSummary.build(steps, emptyMap(), today, null, fmt).rows[0].detail)
    }

    @Test fun unavailableMetricsAreNamedWithReason() {
        assertTrue(WeekSummary.UNAVAILABLE.contains("Schlafdauer") && WeekSummary.UNAVAILABLE.contains("keine belegte Quelle"))
    }
}

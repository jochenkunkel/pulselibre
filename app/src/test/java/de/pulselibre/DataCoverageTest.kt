package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DataCoverageTest {
    private val today = LocalDate.of(2026, 11, 19)
    private fun days(vararg offsets: Int, v: Double = 1.0) = offsets.associate { today.minusDays(it.toLong()) to v }

    @Test fun countsOnlyCompleteDaysBeforeTodayInsideTheWindow() {
        val daily = days(0, 1, 2, 28, 29) // today (0) and day 29 are outside the 28-day window of complete days
        val line = DataCoverage.line("Ruhepuls", daily, today, 28)
        assertEquals(3, line.validDays)
        assertEquals("Ruhepuls: 3 von 28 Tagen", line.text())
    }

    @Test fun seriesAreReportedSeparately() {
        val rest = DataCoverage.line("Ruhepuls", days(1, 2, 3), today, 28)
        val steps = DataCoverage.line("Schritte", (1..20).associate { today.minusDays(it.toLong()) to 4000.0 }, today, 28)
        assertEquals(3, rest.validDays); assertEquals(20, steps.validDays)
        assertFalse(rest.sufficient); assertTrue(steps.sufficient)
    }

    @Test fun insufficientCoverageExplainsWhatIsMissing() {
        val line = DataCoverage.line("Ruhepuls", days(1, 2, 3), today, 28)
        assertTrue(line.hint()!!.contains("mindestens 14"))
        assertTrue(line.description().contains("3 von 28"))
        assertNull(DataCoverage.line("Schritte", (1..14).associate { today.minusDays(it.toLong()) to 1.0 }, today, 28).hint())
    }

    @Test fun nonFiniteValuesDoNotCount() {
        val line = DataCoverage.line("Ruhepuls", mapOf(today.minusDays(1) to Double.NaN, today.minusDays(2) to 60.0), today, 7)
        assertEquals(1, line.validDays)
        assertNotNull(line.hint())
    }
}

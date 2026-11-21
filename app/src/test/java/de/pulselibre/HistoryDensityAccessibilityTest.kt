package de.pulselibre

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HistoryDensityAccessibilityTest {
    private val start = Instant.parse("2026-11-19T00:00:00Z")
    private val end = start.plus(Duration.ofHours(4))
    private val axis = AxisScale(50, 110)
    private fun density(vararg minuteValue: Pair<Int, Int>) = aggregateHistoryDensity(
        minuteValue.map { (m, v) -> HistoryPoint(start.plusSeconds(m * 60L), v) },
        start, end, HistoryResolution.ONE_HOUR, axis, 10
    )

    @Test fun summaryNamesCountsRangeAndInteraction() {
        val text = HistoryDensityAccessibility.summary(GermanStrings, density(5 to 60, 10 to 70, 70 to 80), axis)
        assertTrue(text, text.contains("3 Messwerte in 2 von 4 Zeitfenstern"))
        assertTrue(text, text.contains("50 bis 110"))
        assertTrue(text, text.contains("Doppeltippen"))
    }

    @Test fun emptyDensityIsSaidPlainly() {
        val text = HistoryDensityAccessibility.summary(GermanStrings, density(), axis)
        assertTrue(text, text.contains("keine Messwerte"))
    }
}

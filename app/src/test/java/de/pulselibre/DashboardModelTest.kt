package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class DashboardModelTest {
    @Test fun emptySnapshotShowsNoInventedMeasurements() {
        val metrics = dashboardMetrics(HealthSnapshot())
        assertEquals(6, metrics.size)
        assertEquals("Keine Messdaten", metrics[0].value)
        assertEquals("Keine Messdaten", metrics[1].value)
        assertEquals("Keine Messdaten", metrics[2].value)
        assertEquals("Keine Messdaten", metrics[3].value)
        assertEquals("Noch nicht verfügbar", metrics[5].value)
    }
    @Test fun validatedReadingsAreFormatted() {
        val metrics = dashboardMetrics(HealthSnapshot(68, 97, 4200, 485))
        assertEquals("68 bpm", metrics[0].value)
        assertEquals("97 %", metrics[1].value)
        assertEquals("4200", metrics[2].value)
        assertEquals("8 h 5 min", metrics[3].value)
    }
    @Test fun implausibleOrNegativeReadingsAreNotShown() {
        val metrics = dashboardMetrics(HealthSnapshot(-1, 101, -3, -10))
        assertTrue(metrics.take(4).all { it.value == "Keine Messdaten" })
    }
    @Test fun oxygenZeroIsNotSilentlyReplacedWithNormalValue() {
        assertEquals("0 %", dashboardMetrics(HealthSnapshot(oxygenPercent = 0))[1].value)
    }
}

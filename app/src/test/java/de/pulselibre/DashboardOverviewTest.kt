package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class DashboardOverviewTest {
    @Test fun emptySourceHasTruthfulEmptyStates() {
        val overview = dashboardOverview(emptyList())
        assertTrue((overview.vitalSigns + overview.sleep + overview.today).all { it.value == "Keine Messdaten" })
        assertTrue(overview.details.isEmpty())
    }

    @Test fun shuffledSourceKeepsReceivedValuesNotesAndAllOtherMetrics() {
        val pulse = DashboardMetric("Herzfrequenz", "71 bpm", "Letzter Empfang: 17.11. 08:00 · älter als 15 min")
        val oxygen = DashboardMetric("Sauerstoffsättigung · SpO₂", "97 %", "Letzter Empfang: 17.11. 08:02")
        val steps = DashboardMetric("Schritte (Tageszähler)", "1234", "Heute empfangen")
        val goal = DashboardMetric("Schrittziel", "Keine Messdaten", "Nicht gemessen")
        val sleep = DashboardMetric("Schlaf", "Keine Messdaten", "Keine validierte Historie")
        val rr = DashboardMetric("RR-Intervall (kein RMSSD)", "800 ms", "Keine Zeitreihenanalyse")
        val overview = dashboardOverview(listOf(rr, goal, oxygen, steps, sleep, pulse))
        assertEquals(listOf(pulse, oxygen), overview.vitalSigns)
        assertEquals(sleep, overview.sleep)
        assertEquals(listOf(steps, goal), overview.today.take(2))
        assertEquals("Keine Messdaten", overview.today[2].value)
        assertEquals(listOf(rr), overview.details)
    }

    @Test fun missingOxygenNeverUsesAnotherServiceValue() {
        val pulse = DashboardMetric("Herzfrequenz", "71 bpm", "Vorläufige Beobachtung")
        val overview = dashboardOverview(listOf(pulse))
        assertEquals(pulse, overview.vitalSigns[0])
        assertEquals("Keine Messdaten", overview.vitalSigns[1].value)
    }
    @Test fun stepGoalProgressUsesDeviceGoalAndRetainsOverachievement() {
        val steps = DashboardMetric("Schritte (Tageszähler)", "12000", "Tageszähler")
        val goal = DashboardMetric("Schrittziel", "10000", "Geräteziel")
        val metric = stepGoalProgressMetric(steps, goal)
        assertEquals("120 %", metric.value)
        assertTrue(metric.note.contains("keine Gesundheitsbewertung"))
    }

    @Test fun stepGoalProgressRequiresValidStepCountAndPositiveDeviceGoal() {
        assertEquals("Keine Messdaten", stepGoalProgressMetric(
            DashboardMetric("Schritte (Tageszähler)", "Keine Messdaten", ""),
            DashboardMetric("Schrittziel", "10000", "")
        ).value)
        assertEquals("Keine Messdaten", stepGoalProgressMetric(
            DashboardMetric("Schritte (Tageszähler)", "1000", ""),
            DashboardMetric("Schrittziel", "0", "")
        ).value)
    }

}

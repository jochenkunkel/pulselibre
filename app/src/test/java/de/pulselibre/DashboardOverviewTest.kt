package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class DashboardOverviewTest {
    @Test fun emptySourceHasTruthfulEmptyStates() {
        val overview = dashboardOverview(emptyList())
        assertTrue((overview.vitalSigns + overview.sleep + overview.steps + overview.stepGoal).all { it.value == "Keine Messdaten" })
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
        assertEquals(steps, overview.steps)
        assertEquals(goal, overview.stepGoal)
        assertEquals(listOf(rr), overview.details)
    }

    @Test fun missingOxygenNeverUsesAnotherServiceValue() {
        val pulse = DashboardMetric("Herzfrequenz", "71 bpm", "Vorläufige Beobachtung")
        val overview = dashboardOverview(listOf(pulse))
        assertEquals(pulse, overview.vitalSigns[0])
        assertEquals("Keine Messdaten", overview.vitalSigns[1].value)
    }
    @Test fun stepGoalProgressUsesValidDeviceValuesAndRetainsOverachievement() {
        val goal = DashboardMetric("Schrittziel", "9330", "")
        fun progress(count: String, target: DashboardMetric = goal) =
            stepGoalProgress(DashboardMetric("Schritte (Tageszähler)", count, ""), target)
        assertEquals(100.0 * 136 / 9330, progress("136")!!, 0.0001)
        assertEquals(0.0, progress("0")!!, 0.0)
        assertEquals(100.0, progress("9330")!!, 0.0)
        assertEquals(200.0, progress("18660")!!, 0.0)
        assertNull(progress("Keine Messdaten"))
        assertNull(progress("-1"))
        assertNull(progress("136", goal.copy(value = "0")))
    }
}

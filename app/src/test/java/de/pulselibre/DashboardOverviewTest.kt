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

    @Test fun localStepGoalOverridesDeviceGoalAndIsLabelled() {
        val device = DashboardMetric("Schrittziel", "9330", "Gerät")
        val other = DashboardMetric("Schlaf", "Keine Messdaten", "")
        val out = applyLocalStepGoal(listOf(other, device), " 8100 ")
        assertEquals(DashboardMetric("Schrittziel", "8100", LOCAL_STEP_GOAL_NOTE), out.single { it.title == "Schrittziel" })
        assertEquals(other, out[0])
    }

    @Test fun localStepGoalIsAddedWhenDeviceSentNone() {
        val out = applyLocalStepGoal(emptyList(), "5000")
        assertEquals("5000", out.single().value)
    }

    @Test fun invalidLocalStepGoalKeepsDeviceGoal() {
        val device = DashboardMetric("Schrittziel", "9330", "Gerät")
        for (bad in listOf(null, "", "abc", "99", "100001", "8,5", "-5000")) {
            assertEquals("input=$bad", listOf(device), applyLocalStepGoal(listOf(device), bad))
        }
    }

    @Test fun goalReachedOnlyAtOrAboveTargetAndNeverWithoutData() {
        val goal = DashboardMetric("Schrittziel", "8100", "")
        fun status(count: String, g: DashboardMetric = goal) =
            stepGoalStatus(DashboardMetric("Schritte (Tageszähler)", count, ""), g)
        assertNull(status("8099"))
        assertEquals("Tagesziel erreicht", status("8100"))
        assertEquals("Tagesziel erreicht", status("8155"))
        assertNull(status("Keine Messdaten"))
        assertNull(status("9000", DashboardMetric("Schrittziel", "Keine Messdaten", "")))
    }

    @Test fun effectiveGoalPrefersValidOwnGoalOverDeviceGoal() {
        assertEquals(8100L, effectiveStepGoal("8100", 9330L))
        assertEquals(9330L, effectiveStepGoal("abc", 9330L))
        assertEquals(9330L, effectiveStepGoal(null, 9330L))
        assertNull(effectiveStepGoal(null, null))
        assertNull(effectiveStepGoal("50", 0L))
    }

    @Test fun goalRegularityCountsOnlyDaysWithDataAndNeverTodayOrMissingDays() {
        val today = java.time.LocalDate.of(2026, 11, 20)
        val daily = mapOf(today to 99999.0, today.minusDays(1) to 8100.0, today.minusDays(2) to 8099.0,
            today.minusDays(3) to 12000.0, today.minusDays(40) to 20000.0)
        val r = GoalRegularity.build(daily, today, 28, 8100)
        assertEquals(2, r.reached); assertEquals(3, r.validDays)
        assertEquals("Eigenes Ziel (8.100) erreicht an 2 von 3 Tagen mit Daten (28-Tage-Fenster)", r.text())
        assertNull(GoalRegularity.build(emptyMap(), today, 28, 8100).text())
    }
}

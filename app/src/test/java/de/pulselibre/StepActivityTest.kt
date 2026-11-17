package de.pulselibre

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class StepActivityTest {
    private val now = Instant.parse("2026-11-17T12:00:00Z")
    private fun at(secondsAgo: Long, count: Long) = StepObservation(now.minusSeconds(secondsAgo).toEpochMilli(), count)

    private fun estimate(delta: Long, settings: ActivityDetectionSettings = ActivityDetectionSettings()) =
        estimateStepActivity(listOf(at(300, 1_000), at(150, 1_000 + delta / 2), at(0, 1_000 + delta)),
            now, ZoneId.of("UTC"), settings)

    @Test fun strictUserThresholdsClassifyFiveMinuteAverage() {
        assertEquals(PulseContext.REST, estimate(49).context)
        assertEquals(PulseContext.MOVEMENT, estimate(50).context) // exactly 10 steps/min
        assertEquals(PulseContext.MOVEMENT, estimate(500).context) // exactly 100 steps/min
        assertEquals(PulseContext.SPORT, estimate(505).context)
        assertEquals(101.0, estimate(505).cadenceStepsPerMinute!!, 0.001)
    }

    @Test fun stableCounterMeansNoStepsObservedNotMissingOrInventedSteps() {
        val value = estimate(0)
        assertEquals(PulseContext.REST, value.context)
        assertEquals(0.0, value.cadenceStepsPerMinute!!, 0.0)
        assertEquals(3, value.sampleCount)
    }

    @Test fun insufficientStaleFutureAndInvalidObservationsFailClosed() {
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(emptyList(), now).context)
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(listOf(at(0, 1_000)), now).context)
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(
            listOf(at(600, 1_000), at(240, 1_100)), now, ZoneId.of("UTC")).context)
        val future = StepObservation(now.plusSeconds(60).toEpochMilli(), 1_500)
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(listOf(at(60, 1_000), future), now, ZoneId.of("UTC")).context)
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(
            listOf(at(300, 1_000), at(0, 0x1_0000_0000L)), now, ZoneId.of("UTC")).context)
    }

    @Test fun resetBreaksTheWindowAndAValidSuffixCanRecover() {
        val resetOnly = listOf(at(120, 1_000), at(0, 20))
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(resetOnly, now, ZoneId.of("UTC")).context)
        val recovered = listOf(at(240, 1_000), at(120, 20), at(0, 25))
        assertEquals(PulseContext.REST, estimateStepActivity(recovered, now, ZoneId.of("UTC")).context)
    }

    @Test fun duplicatesAndLocalDayBoundaryDoNotCreateFalseCadence() {
        val conflict = listOf(at(300, 1_000), at(0, 1_100), at(0, 1_200))
        assertEquals(PulseContext.UNKNOWN, estimateStepActivity(conflict, now, ZoneId.of("UTC")).context)
        val midnight = Instant.parse("2026-11-18T00:02:00+01:00").atZone(ZoneId.of("Europe/Berlin")).toInstant()
        val yesterday = midnight.minusSeconds(180)
        val today = midnight.minusSeconds(60)
        val boundary = listOf(
            StepObservation(yesterday.toEpochMilli(), 1_000),
            StepObservation(today.toEpochMilli(), 1_050)
        )
        assertEquals(PulseContext.UNKNOWN,
            estimateStepActivity(boundary, midnight, ZoneId.of("Europe/Berlin")).context)
    }

    @Test fun settingsCanChangeCutoffsAndInvalidSettingsAreRejected() {
        val settings = ActivityDetectionSettings(restBelowStepsPerMinute = 5, sportAboveStepsPerMinute = 80,
            windowMinutes = 4, maxGapMinutes = 3, minimumSpanSeconds = 30)
        assertTrue(settings.isValid())
        assertEquals(PulseContext.SPORT, estimateStepActivity(
            listOf(at(180, 1_000), at(0, 1_300)), now, ZoneId.of("UTC"), settings).context)
        assertFalse(settings.copy(restBelowStepsPerMinute = 90, sportAboveStepsPerMinute = 80).isValid())
        assertFalse(settings.copy(windowMinutes = 1).isValid())
    }
}

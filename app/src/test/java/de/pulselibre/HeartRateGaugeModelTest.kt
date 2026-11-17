package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class HeartRateGaugeModelTest {
    private fun reading(value: String) = HeartRateGaugeModel.from(DashboardMetric("Herzfrequenz", value, "Empfangszeit"))

    @Test fun missingAndInvalidReadingsNeverBecomeDemoPulse() {
        for (value in listOf("Keine Messdaten", "", "71", "71.5 bpm", "0 bpm", "241 bpm", "-1 bpm")) {
            assertNull(reading(value).bpm)
            assertNull(reading(value).fraction)
        }
    }
    @Test fun scaleEndpointsAreDisplayBoundsAndOutOfRangeValueIsPreserved() {
        assertEquals(0f, reading("40 bpm").fraction!!, 0f)
        assertEquals(0.5f, reading("110 bpm").fraction!!, 0.0001f)
        assertEquals(1f, reading("180 bpm").fraction!!, 0f)
        assertEquals(210, reading("210 bpm").bpm)
        assertEquals("Oberhalb der Anzeigeskala", reading("210 bpm").scaleNote)
        assertEquals("Unterhalb der Anzeigeskala", reading("35 bpm").scaleNote)
    }
    @Test fun gaugeContextComesFromStepEstimateAndDefaultsToUnknown() {
        assertEquals(PulseContext.UNKNOWN, StepActivityEstimate.UNKNOWN.context)
        assertEquals(PulseContext.SPORT, PulseActivityFixture.sport.context)
    }
}

private object PulseActivityFixture {
    val sport = StepActivityEstimate(PulseContext.SPORT, 101.0, 3, 180, "Schrittbasierte Einordnung")
}

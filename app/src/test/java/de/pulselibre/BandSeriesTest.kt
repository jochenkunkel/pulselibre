package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #696 follow-up: day dialogs of stress, SpO2 and respiration read the canonical band metrics, never live keys. */
class BandSeriesTest {
    @Test fun tilesMapToTheCanonicalFitMetricContract() {
        assertEquals("heart_rate" to "bpm", BandSeries.of(DashboardMeasurement.PULSE)!!.let { it.metric to it.unit })
        assertEquals("spo2" to "percent", BandSeries.of(DashboardMeasurement.OXYGEN)!!.let { it.metric to it.unit })
        assertEquals("stress" to "device_scale", BandSeries.of(DashboardMeasurement.STRESS)!!.let { it.metric to it.unit })
        assertEquals("respiration" to "breaths/min", BandSeries.of(DashboardMeasurement.RESPIRATION)!!.let { it.metric to it.unit })
        // Stress starts at 1: 0 is a status code, not a measurement (#696).
        assertEquals(1..100, BandSeries.STRESS.range)
    }

    @Test fun measurementsWithoutDecodedBandDataHaveNoBandSection() {
        listOf(DashboardMeasurement.STEPS, DashboardMeasurement.RESTING_PULSE, DashboardMeasurement.BODY_BATTERY,
            DashboardMeasurement.RR, DashboardMeasurement.CALORIES_TOTAL).forEach { assertNull(it.name, BandSeries.of(it)) }
    }
}

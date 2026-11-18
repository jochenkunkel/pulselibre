package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class MetricAvailabilityTest {
    private fun q(observed: Int, available: Int) = DayDataQuality(observed, available, null, 0)

    @Test fun noObservationsIsNotAvailableEvenIfQualityExists() {
        assertEquals(MetricAvailability.NOT_AVAILABLE, MetricAvailability.of(DashboardMeasurement.PULSE, q(0, 600), 0))
    }
    @Test fun lowCoverageIsInsufficientNotZero() {
        assertEquals(MetricAvailability.INSUFFICIENT_DATA, MetricAvailability.of(DashboardMeasurement.PULSE, q(100, 600), 100))
        assertEquals(MetricAvailability.INSUFFICIENT_DATA, MetricAvailability.of(DashboardMeasurement.PULSE, null, 5))
    }
    @Test fun sufficientCoverageIsAvailableOrExperimental() {
        assertEquals(MetricAvailability.AVAILABLE, MetricAvailability.of(DashboardMeasurement.PULSE, q(300, 600), 300))
        assertEquals(MetricAvailability.EXPERIMENTAL, MetricAvailability.of(DashboardMeasurement.OXYGEN, q(600, 600), 600))
    }
}

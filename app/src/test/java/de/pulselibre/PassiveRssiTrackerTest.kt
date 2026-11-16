package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class PassiveRssiTrackerTest {
    private val uuid = "3e1d50cd-7e3e-427d-8e1c-b78aa87fe624"

    @Test fun recordsOnlyCandidateAdvertisements() {
        val tracker = PassiveRssiTracker()
        tracker.observe("unknown", -30, emptyList())
        tracker.observe("candidate", -50, listOf(uuid))
        tracker.observe("candidate", -70, listOf(uuid.uppercase()))
        val report = tracker.summary()
        assertTrue(report.contains("RSSI-Messpunkte: 2"))
        assertTrue(report.contains("RSSI min/max: -70 / -50 dBm"))
        assertFalse(report.contains("candidate"))
    }

    @Test fun resetDropsPreviousMeasurements() {
        val tracker = PassiveRssiTracker()
        tracker.observe("a", -55, listOf(uuid))
        tracker.reset()
        assertTrue(tracker.summary().contains("RSSI-Messpunkte: 0"))
    }

    @Test fun ignoresInvalidRssi() {
        val tracker = PassiveRssiTracker()
        tracker.observe("a", 127, listOf(uuid))
        tracker.observe("a", -128, listOf(uuid))
        assertTrue(tracker.summary().contains("RSSI-Messpunkte: 0"))
    }

    @Test fun boundsStoredSamples() {
        val tracker = PassiveRssiTracker()
        repeat(800) { tracker.observe("a", -60, listOf(uuid)) }
        assertTrue(tracker.summary().contains("RSSI-Messpunkte: 600"))
    }
}

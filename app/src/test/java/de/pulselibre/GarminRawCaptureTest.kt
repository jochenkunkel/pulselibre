package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminRawCaptureTest {
    @Test fun captureDisabledByDefault() {
        val probe = GarminFileListProbe()
        probe.observeMlPayload(GarminFileListRequest.cobs(GarminDeviceInformationAckCandidate.frame()))
        assertTrue(probe.rawCaptureReport().contains("Frames=0"))
    }

    @Test fun capturesOnlyVerifiedFramesWhenEnabled() {
        val probe = GarminFileListProbe()
        probe.captureRawFrames = true
        val frame = GarminDeviceInformationAckCandidate.frame()
        probe.observeMlPayload(GarminFileListRequest.cobs(frame))
        val report = probe.rawCaptureReport()
        assertTrue(report.contains("Frames=1"))
        assertTrue(report.contains(frame.joinToString("") { "%02x".format(it.toInt() and 255) }))
    }
}

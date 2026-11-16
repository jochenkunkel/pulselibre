package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminDeviceInformationAckCandidateTest {
    @Test fun minimalCandidateHasValidCrcAndNoDeviceIdentity() {
        val bytes = GarminDeviceInformationAckCandidate.frame()
        assertEquals(9, bytes.size)
        assertTrue(GarminDeviceInformationAckCandidate.validCandidate())
        assertNotNull(GarminFrameValidator.inspectVerified(bytes))
        assertEquals(5000, (bytes[2].toInt() and 255) or ((bytes[3].toInt() and 255) shl 8))
        assertEquals(5024, (bytes[4].toInt() and 255) or ((bytes[5].toInt() and 255) shl 8))
        assertEquals(0, bytes[6].toInt())
    }

    @Test fun candidateCanBeCobsFramedAndDecodedPassively() {
        val probe = GarminFileListProbe()
        probe.observeMlPayload(GarminFileListRequest.cobs(GarminDeviceInformationAckCandidate.frame()))
        assertEquals(1, probe.frames)
        assertEquals(0, probe.validLists)
        assertEquals(1, probe.bootstrapDiagnosis.statusFrames)
    }
}

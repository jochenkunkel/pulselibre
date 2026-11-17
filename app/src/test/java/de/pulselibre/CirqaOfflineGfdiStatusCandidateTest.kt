package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaOfflineGfdiStatusCandidateTest {
    @Test fun encodesReferenceFormat5050AckOffline() {
        val frame = CirqaOfflineGfdiStatusCandidate.ackFrame(5050)
        assertEquals(9, frame.size)
        assertTrue(CirqaOfflineGfdiStatusCandidate.isWellFormedAck(frame, 5050))
        assertFalse(CirqaOfflineGfdiStatusCandidate.isWellFormedAck(frame, 5024))
        assertEquals(0xBA, frame[4].toInt() and 255)
        assertEquals(0x13, frame[5].toInt() and 255)
    }

    @Test fun matchesExisting5024MinimalCandidateByteForByte() {
        assertArrayEquals(GarminDeviceInformationAckCandidate.frame(),
            CirqaOfflineGfdiStatusCandidate.ackFrame(5024))
    }

    @Test fun detectsCorruptionAndRejectsInvalidTypes() {
        val frame = CirqaOfflineGfdiStatusCandidate.ackFrame(5050)
        frame[6] = 1
        assertFalse(CirqaOfflineGfdiStatusCandidate.isWellFormedAck(frame, 5050))
        try {
            CirqaOfflineGfdiStatusCandidate.ackFrame(5000)
            fail("Expected invalid message type to be rejected")
        } catch (_: IllegalArgumentException) { }
    }
}

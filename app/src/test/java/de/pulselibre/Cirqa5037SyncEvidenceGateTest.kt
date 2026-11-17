package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5037SyncEvidenceGateTest {
    @Test fun freshValid5037AfterCompletedHandshakeCounts() {
        assertTrue(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(0, 1, true, true))
        assertTrue(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(2, 3, true, true))
    }

    @Test fun preexisting5037DoesNotCount() {
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(1, 1, true, true))
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(2, 1, true, true))
    }

    @Test fun missingWriteOrAckBlocksSyncClaim() {
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(0, 1, false, true))
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(0, 1, true, false))
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(0, 1, false, false))
    }

    @Test fun invalidBaselineFailsClosed() {
        assertFalse(Cirqa5037SyncEvidenceGate.isPostHandshakeSignal(-1, 1, true, true))
    }
}

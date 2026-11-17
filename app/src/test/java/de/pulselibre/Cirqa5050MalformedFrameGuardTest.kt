package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050MalformedFrameGuardTest {
    @Test fun ignoresMalformedFramesBeforeResponseWasQueued() {
        assertFalse(Cirqa5050MalformedFrameGuard.shouldAbort(8, 8))
        assertFalse(Cirqa5050MalformedFrameGuard.shouldAbort(8, 10))
    }

    @Test fun abortsAtThirdNewRejection() {
        assertTrue(Cirqa5050MalformedFrameGuard.shouldAbort(8, 11))
        assertTrue(Cirqa5050MalformedFrameGuard.shouldAbort(0, 5))
    }

    @Test fun failsClosedOnInvalidCounters() {
        assertTrue(Cirqa5050MalformedFrameGuard.shouldAbort(-1, 0))
        assertTrue(Cirqa5050MalformedFrameGuard.shouldAbort(3, 2))
    }
}

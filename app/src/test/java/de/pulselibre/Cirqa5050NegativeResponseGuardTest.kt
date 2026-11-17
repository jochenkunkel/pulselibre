package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050NegativeResponseGuardTest {
    @Test fun historicalErrorsAreNotAttributedToCurrent5050() {
        assertFalse(Cirqa5050NegativeResponseGuard.shouldAbort(2, 2))
        assertFalse(Cirqa5050NegativeResponseGuard.shouldAbort(0, 0))
    }

    @Test fun firstNewNegativeStatusImmediatelyAborts() {
        assertTrue(Cirqa5050NegativeResponseGuard.shouldAbort(0, 1))
        assertTrue(Cirqa5050NegativeResponseGuard.shouldAbort(2, 3))
    }

    @Test fun invalidCountersFailClosed() {
        assertTrue(Cirqa5050NegativeResponseGuard.shouldAbort(-1, 0))
        assertTrue(Cirqa5050NegativeResponseGuard.shouldAbort(3, 2))
    }
}

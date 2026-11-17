package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050Malformed5037GuardTest {
    @Test fun ignoresExistingMalformed5037Before5050() {
        assertFalse(Cirqa5050Malformed5037Guard.shouldAbort(2, 2))
    }

    @Test fun stopsOnFirstNewMalformed5037() {
        assertTrue(Cirqa5050Malformed5037Guard.shouldAbort(2, 3))
    }

    @Test fun invalidCountersFailClosed() {
        assertTrue(Cirqa5050Malformed5037Guard.shouldAbort(-1, 0))
        assertTrue(Cirqa5050Malformed5037Guard.shouldAbort(4, 3))
    }
}

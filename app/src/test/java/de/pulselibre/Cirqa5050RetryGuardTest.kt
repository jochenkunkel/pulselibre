package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050RetryGuardTest {
    @Test fun doesNotTreatFirstConfigurationAsRetry() {
        assertFalse(Cirqa5050RetryGuard.hasRetry(1, 1))
        assertFalse(Cirqa5050RetryGuard.hasRetry(0, 0))
    }

    @Test fun detectsAdditionalConfigurationFrame() {
        assertTrue(Cirqa5050RetryGuard.hasRetry(1, 2))
        assertTrue(Cirqa5050RetryGuard.hasRetry(3, 5))
    }

    @Test fun rejectsNegativeCounters() {
        for ((baseline, current) in listOf(-1 to 0, 0 to -1)) {
            try {
                Cirqa5050RetryGuard.hasRetry(baseline, current)
                fail("Negative counter accepted")
            } catch (_: IllegalArgumentException) { }
        }
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050NotificationFloodGuardTest {
    @Test fun onlyCountsNotificationsAfterQueue() {
        assertFalse(Cirqa5050NotificationFloodGuard.shouldAbort(400, 400))
        assertFalse(Cirqa5050NotificationFloodGuard.shouldAbort(400, 519))
        assertTrue(Cirqa5050NotificationFloodGuard.shouldAbort(400, 520))
    }

    @Test fun rejectsInvalidCountersAndHandlesLargeValues() {
        assertTrue(Cirqa5050NotificationFloodGuard.shouldAbort(-1, 0))
        assertTrue(Cirqa5050NotificationFloodGuard.shouldAbort(10, 9))
        assertTrue(Cirqa5050NotificationFloodGuard.shouldAbort(0, Int.MAX_VALUE))
        assertFalse(Cirqa5050NotificationFloodGuard.shouldAbort(Int.MAX_VALUE, Int.MAX_VALUE))
    }
}

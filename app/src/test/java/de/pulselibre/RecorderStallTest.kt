package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorderStallTest {
    private val minute = 60_000L

    @Test fun freshAttemptIsNotStalled() = assertFalse(RecorderStall.isStalled(1_000, 0, 1_000 + 9 * minute))

    @Test fun attemptWithoutAnyReadingStallsAfterTheLimit() = assertTrue(RecorderStall.isStalled(1_000, 0, 1_000 + 10 * minute))

    @Test fun aRecentReadingResetsTheClock() {
        assertFalse(RecorderStall.isStalled(1_000, 1_000 + 8 * minute, 1_000 + 15 * minute))
        assertTrue(RecorderStall.isStalled(1_000, 1_000 + 8 * minute, 1_000 + 18 * minute))
    }

    @Test fun aReadingFromAnEarlierAttemptIsIgnored() = assertTrue(RecorderStall.isStalled(20 * minute, 5 * minute, 31 * minute))

    @Test fun unknownStartOrBackwardsClockNeverStalls() {
        assertFalse(RecorderStall.isStalled(0, 0, 99 * minute))
        assertFalse(RecorderStall.isStalled(50 * minute, 0, 10 * minute))
    }
}

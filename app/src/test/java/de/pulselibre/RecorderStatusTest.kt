package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class RecorderStatusTest {
    private val now = 1_000_000L
    @Test fun freshHeartbeatWithFlagIsRunning() = assertTrue(RecorderStatus.isRunning(true, now - 10_000, now))
    @Test fun flagWithoutHeartbeatIsNotRunning() {
        assertFalse(RecorderStatus.isRunning(true, null, now)); assertFalse(RecorderStatus.isRunning(true, -1, now))
    }
    @Test fun staleHeartbeatAfterProcessKillIsNotRunning() =
        assertFalse(RecorderStatus.isRunning(true, now - RecorderStatus.STALE_AFTER_MS - 1, now))
    @Test fun heartbeatAtTheLimitStillCountsAndFutureStampsDoNot() {
        assertTrue(RecorderStatus.isRunning(true, now - RecorderStatus.STALE_AFTER_MS, now))
        assertFalse(RecorderStatus.isRunning(true, now + 5_000, now))
    }
    @Test fun clearedFlagIsNeverRunning() = assertFalse(RecorderStatus.isRunning(false, now, now))
}

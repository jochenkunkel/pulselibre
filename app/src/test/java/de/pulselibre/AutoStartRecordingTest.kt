package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class AutoStartRecordingTest {
    private fun decide(enabled: Boolean = true, fresh: Boolean = true, perms: Boolean = true, bt: Boolean = true, running: Boolean = false) =
        AutoStartRecording.shouldStart(enabled, fresh, perms, bt, running)

    @Test fun startsWhenEnabledAndEverythingIsReady() = assertTrue(decide())
    @Test fun neverStartsWhenDisabled() = assertFalse(decide(enabled = false))
    @Test fun doesNotRestartOnRecreateOrRotation() = assertFalse(decide(fresh = false))
    @Test fun doesNotStartWithoutPermissions() = assertFalse(decide(perms = false))
    @Test fun doesNotStartWithBluetoothOff() = assertFalse(decide(bt = false))
    @Test fun doesNotStartTwiceWhenAlreadyRunning() = assertFalse(decide(running = true))

    @Test fun staleRecorderFlagWithoutAliveServiceDoesNotBlockAutostart() {
        // Android force-stopped the service (app update): flag + heartbeat still look fresh, service is gone (#212).
        val alreadyRunning = AutoStartRecording.isAlreadyRunning(recorderFlagFresh = true, serviceAlive = false)
        assertFalse(alreadyRunning)
        assertTrue(AutoStartRecording.shouldStart(true, true, true, true, alreadyRunning))
    }

    @Test fun aliveServiceWithFreshFlagStillBlocksDoubleStart() {
        val alreadyRunning = AutoStartRecording.isAlreadyRunning(recorderFlagFresh = true, serviceAlive = true)
        assertTrue(alreadyRunning)
        assertFalse(AutoStartRecording.shouldStart(true, true, true, true, alreadyRunning))
    }
}

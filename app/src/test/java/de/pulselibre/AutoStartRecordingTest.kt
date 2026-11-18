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
}

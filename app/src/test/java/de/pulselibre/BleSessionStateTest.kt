package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class BleSessionStateTest {
    @Test fun normalConnection() {
        var state = BleSessionState.IDLE
        state = transition(state, BleEvent.START_SCAN)
        assertEquals(BleSessionState.SCANNING, state)
        state = transition(state, BleEvent.CONNECT)
        assertEquals(BleSessionState.CONNECTING, state)
        state = transition(state, BleEvent.CONNECTED)
        assertEquals(BleSessionState.CONNECTED, state)
        assertEquals(BleSessionState.DISCONNECTED, transition(state, BleEvent.DISCONNECTED))
    }
    @Test fun cannotConnectTwice() {
        assertEquals(BleSessionState.CONNECTED, transition(BleSessionState.CONNECTED, BleEvent.CONNECT))
    }
    @Test fun cannotCompleteConnectionAfterReset() {
        assertEquals(BleSessionState.IDLE, transition(BleSessionState.IDLE, BleEvent.CONNECTED))
    }
    @Test fun failureAndRecovery() {
        val error = transition(BleSessionState.CONNECTING, BleEvent.FAILED)
        assertEquals(BleSessionState.ERROR, error)
        assertEquals(BleSessionState.SCANNING, transition(error, BleEvent.START_SCAN))
    }
    @Test fun scanStopIsIdempotent() {
        assertEquals(BleSessionState.IDLE, transition(BleSessionState.IDLE, BleEvent.STOP_SCAN))
    }
    @Test fun disconnectWhileScanning() {
        assertEquals(BleSessionState.DISCONNECTED, transition(BleSessionState.SCANNING, BleEvent.DISCONNECTED))
    }
}

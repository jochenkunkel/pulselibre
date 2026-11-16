package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaStateMappingTest {
    @Test fun androidStatesMapExactly() {
        assertEquals(CirqaConnectionHistory.State.DISCONNECTED, CirqaStateMapping.fromAndroidState(0))
        assertEquals(CirqaConnectionHistory.State.CONNECTING, CirqaStateMapping.fromAndroidState(1))
        assertEquals(CirqaConnectionHistory.State.CONNECTED, CirqaStateMapping.fromAndroidState(2))
        assertEquals(CirqaConnectionHistory.State.DISCONNECTING, CirqaStateMapping.fromAndroidState(3))
        assertEquals(CirqaConnectionHistory.State.UNKNOWN, CirqaStateMapping.fromAndroidState(99))
    }

    @Test fun connectedDoesNotClaimGarminOwnership() {
        val label = CirqaStateMapping.label(CirqaConnectionHistory.State.CONNECTED)
        assertTrue(label.contains("App unbekannt"))
        assertFalse(label.contains("Garmin Connect"))
    }

    @Test fun deniedPermissionCannotBeReportedAsConnected() {
        assertTrue(CirqaStateMapping.label(CirqaConnectionHistory.State.PERMISSION_DENIED).contains("Berechtigung fehlt"))
    }
}

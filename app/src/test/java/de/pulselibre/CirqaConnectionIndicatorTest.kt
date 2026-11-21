package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State
import org.junit.Assert.*
import org.junit.Test

class CirqaConnectionIndicatorTest {
    @Test fun onlyAConnectedGattStateShowsGreen() {
        for (state in State.entries) {
            assertEquals(state == State.CONNECTED, CirqaConnectionIndicator.isConnected(state))
        }
    }

    @Test fun accessibleLabelsDistinguishTransitionsAndUnknownOwnership() {
        assertTrue(CirqaConnectionIndicator.contentDescription(GermanStrings, State.CONNECTED).contains("App unbekannt"))
        assertTrue(CirqaConnectionIndicator.contentDescription(GermanStrings, State.CONNECTING).contains("GATT: verbindet"))
        assertTrue(CirqaConnectionIndicator.contentDescription(GermanStrings, State.DISCONNECTING).contains("GATT: trennt"))
        assertTrue(CirqaConnectionIndicator.contentDescription(GermanStrings, State.PERMISSION_DENIED).contains("Berechtigung fehlt"))
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnectionIndicatorTest {
    @Test fun greenStateRequiresBluetoothPermissionBondAndLiveGattConnection() {
        assertTrue(CirqaConnectionIndicator.isConnected(true, true, true, true))
        assertFalse(CirqaConnectionIndicator.isConnected(false, true, true, true))
        assertFalse(CirqaConnectionIndicator.isConnected(true, false, true, true))
        assertFalse(CirqaConnectionIndicator.isConnected(true, true, false, true))
        assertFalse(CirqaConnectionIndicator.isConnected(true, true, true, false))
    }

    @Test fun accessibleGreenLabelDoesNotClaimPulsLibreOwnsTheLink() {
        val label = CirqaConnectionIndicator.contentDescription(true, true)
        assertTrue(label.contains("Android meldet GATT verbunden"))
        assertTrue(label.contains("verbundene App unbekannt"))
        assertFalse(label.contains("PulsLibre verbunden"))
    }

    @Test fun missingPermissionStaysOfflineAndExplainsWhyStatusCannotBeRead() {
        assertTrue(CirqaConnectionIndicator.contentDescription(false, false).contains("mangels Berechtigung"))
        assertFalse(CirqaConnectionIndicator.isConnected(true, false, true, true))
    }
}

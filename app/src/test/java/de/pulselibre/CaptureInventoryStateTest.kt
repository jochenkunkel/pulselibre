package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Capture card states: unreadable counters are reported as such, never shown as 0 (P0, FIT/GUI agreement 24.11.). */
class CaptureInventoryStateTest {
    @Test fun nothingReadableIsUnknownNotPartial() {
        assertTrue(CaptureInventory.EMPTY.isUnknown)
        assertFalse(CaptureInventory.EMPTY.hasUnreadable)
    }

    @Test fun someUnreadableCountersAreMarkedPartial() {
        val inv = CaptureInventory(archivedFiles = 3L, archivedBytes = 10L, importedFitFiles = null,
            regularObservations = 5L, candidatesByMetric = emptyMap())
        assertFalse(inv.isUnknown)
        assertTrue(inv.hasUnreadable)
    }

    @Test fun allCountersReadableIsNotPartial() {
        val inv = CaptureInventory(1L, 1L, 1L, 1L, emptyMap(), rawNotifications = 1L, rawFitFields = 1L, undecodedFieldKinds = 0L)
        assertFalse(inv.hasUnreadable)
    }
}

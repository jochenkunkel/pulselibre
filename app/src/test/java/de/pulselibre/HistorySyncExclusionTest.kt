package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySyncExclusionTest {
    @Test fun manualStartIsRejectedWhileAutomaticRuns() {
        assertFalse(HistorySyncExclusion.canStartManual(automaticActive = true))
        assertTrue(HistorySyncExclusion.canStartManual(automaticActive = false))
    }

    @Test fun automaticStartIsRejectedWhileManualRuns() {
        assertFalse(HistorySyncExclusion.canStartAutomatic(manualActive = true))
        assertTrue(HistorySyncExclusion.canStartAutomatic(manualActive = false))
    }
}

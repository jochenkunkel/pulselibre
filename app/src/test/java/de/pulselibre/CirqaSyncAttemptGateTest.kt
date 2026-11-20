package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncAttemptGateTest {
    @Test
    fun v27UsesANewOneShotPreferenceWithoutRearmingTheV25Attempt() {
        assertEquals("cirqaSyncAttemptedV27", CirqaSyncAttemptGate.PREFERENCE_KEY)
        assertEquals("cirqaSyncSummaryV27", CirqaSyncAttemptGate.SUMMARY_KEY)
        assertTrue(CirqaSyncAttemptGate.available(alreadyAttempted = false))
        assertFalse(CirqaSyncAttemptGate.available(alreadyAttempted = true))
    }
}

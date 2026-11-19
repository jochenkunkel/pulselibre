package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncItemListNoPullAttemptGateTest {
    @Test
    fun confirmsAtMostFifteenRunsWithoutAllowingNegativeOrOverflowCounts() {
        assertEquals(0, CirqaSyncItemListNoPullAttemptGate.attemptsUsed(-1))
        assertEquals(15, CirqaSyncItemListNoPullAttemptGate.attemptsUsed(16))
        assertTrue(CirqaSyncItemListNoPullAttemptGate.hasAttemptsRemaining(14))
        assertFalse(CirqaSyncItemListNoPullAttemptGate.hasAttemptsRemaining(15))
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncItemListNoPullAttemptGateTest {
    @Test
    fun confirmsAtMostFortyRunsWithoutAllowingNegativeOrOverflowCounts() {
        assertEquals(0, CirqaSyncItemListNoPullAttemptGate.attemptsUsed(-1))
        assertEquals(40, CirqaSyncItemListNoPullAttemptGate.attemptsUsed(41))
        assertTrue(CirqaSyncItemListNoPullAttemptGate.hasAttemptsRemaining(39))
        assertFalse(CirqaSyncItemListNoPullAttemptGate.hasAttemptsRemaining(40))
    }
}

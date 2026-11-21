package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaNextTransactionProbeAttemptGateTest {
    @Test
    fun gateAllowsOneAdditionalAttemptWithoutResettingSharedListBudget() {
        assertEquals(0, CirqaNextTransactionProbeAttemptGate.attemptsUsed(-1))
        assertEquals(2, CirqaNextTransactionProbeAttemptGate.attemptsUsed(2))
        assertTrue(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(0))
        assertTrue(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(1))
        assertFalse(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(2))
    }
}

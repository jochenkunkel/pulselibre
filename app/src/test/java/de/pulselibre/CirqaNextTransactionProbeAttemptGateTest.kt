package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaNextTransactionProbeAttemptGateTest {
    @Test
    fun gateAllowsOneThirdAttemptWithoutResettingSharedListBudget() {
        assertEquals(0, CirqaNextTransactionProbeAttemptGate.attemptsUsed(-1))
        assertEquals(2, CirqaNextTransactionProbeAttemptGate.attemptsUsed(2))
        assertTrue(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(0))
        assertTrue(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(1))
        assertTrue(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(2))
        assertFalse(CirqaNextTransactionProbeAttemptGate.hasAttemptsRemaining(3))
        assertEquals(3, CirqaNextTransactionProbeAttemptGate.attemptsUsed(999))
    }
}

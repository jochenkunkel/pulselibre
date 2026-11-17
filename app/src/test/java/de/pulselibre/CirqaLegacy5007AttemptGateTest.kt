package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaLegacy5007AttemptGateTest {
    @Test fun oldConsumedBooleanMigratesToOneRemainingAuthorizedAttempt() {
        assertEquals(1, CirqaLegacy5007AttemptGate.consumedAttempts(0, legacyAttempted = true))
        assertTrue(CirqaLegacy5007AttemptGate.canAttempt(0, legacyAttempted = true))
        assertEquals(2, CirqaLegacy5007AttemptGate.nextAttempt(0, legacyAttempted = true))
    }

    @Test fun freshInstallationAllowsTwoAttemptsButNeverAThird() {
        assertEquals(0, CirqaLegacy5007AttemptGate.consumedAttempts(0, legacyAttempted = false))
        assertEquals(1, CirqaLegacy5007AttemptGate.nextAttempt(0, legacyAttempted = false))
        assertTrue(CirqaLegacy5007AttemptGate.canAttempt(1, legacyAttempted = false))
        assertEquals(2, CirqaLegacy5007AttemptGate.nextAttempt(1, legacyAttempted = false))
        assertFalse(CirqaLegacy5007AttemptGate.canAttempt(2, legacyAttempted = false))
        assertEquals(null, CirqaLegacy5007AttemptGate.nextAttempt(2, legacyAttempted = false))
    }

    @Test fun legacyBooleanCannotLowerANewerStoredCount() {
        assertEquals(2, CirqaLegacy5007AttemptGate.consumedAttempts(2, legacyAttempted = false))
        assertEquals(2, CirqaLegacy5007AttemptGate.consumedAttempts(9, legacyAttempted = false))
        assertEquals(2, CirqaLegacy5007AttemptGate.consumedAttempts(0, legacyAttempted = true).let {
            CirqaLegacy5007AttemptGate.consumedAttempts(it + 1, legacyAttempted = true)
        })
    }
}

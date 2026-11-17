package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnect531HandshakePreflightTest {
    @Test fun defaultsToBlocked() {
        val blockers = CirqaConnect531HandshakePreflight.blockers(
            CirqaConnect531HandshakePreflight.Evidence())
        assertEquals(7, blockers.size)
        assertTrue(blockers.contains(
            CirqaConnect531HandshakePreflight.Blocker.EXPLICIT_TRIAL_AUTHORIZATION_MISSING))
    }

    @Test fun onlyAllExplicitEvidenceUnblocks() {
        val complete = CirqaConnect531HandshakePreflight.Evidence(
            device5024Verified = true,
            featureFlagVerified = true,
            appVersionVerified = true,
            friendlyNameVerified = true,
            manufacturerVerified = true,
            modelVerified = true,
            explicitTrialAuthorized = true)
        assertTrue(CirqaConnect531HandshakePreflight.blockers(complete).isEmpty())
        assertEquals(setOf(CirqaConnect531HandshakePreflight.Blocker.MODEL_NOT_VERIFIED),
            CirqaConnect531HandshakePreflight.blockers(
                complete.copy(modelVerified = false)))
    }
}

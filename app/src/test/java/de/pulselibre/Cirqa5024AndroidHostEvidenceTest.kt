package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5024AndroidHostEvidenceTest {
    @Test fun androidBuildValuesDoNotUnblockGarminConfiguration() {
        val snapshot = Cirqa5024AndroidHostEvidence.Snapshot(true, true)
        val blockers = Cirqa5024AndroidHostEvidence.preflight(snapshot)
        assertFalse(blockers.contains(CirqaConnect531HandshakePreflight.Blocker.MANUFACTURER_NOT_VERIFIED))
        assertFalse(blockers.contains(CirqaConnect531HandshakePreflight.Blocker.MODEL_NOT_VERIFIED))
        assertTrue(blockers.contains(CirqaConnect531HandshakePreflight.Blocker.APP_VERSION_NOT_VERIFIED))
        assertTrue(blockers.contains(CirqaConnect531HandshakePreflight.Blocker.FEATURE_FLAG_NOT_VERIFIED))
        assertTrue(blockers.contains(CirqaConnect531HandshakePreflight.Blocker.EXPLICIT_TRIAL_AUTHORIZATION_MISSING))
    }

    @Test fun diagnosticDoesNotRevealBuildStrings() {
        val report = Cirqa5024AndroidHostEvidence.Snapshot(false, true).summary()
        assertTrue(report.contains("Senden=GESPERRT"))
        assertTrue(report.contains("Garmin-Appversion=UNGEKLÄRT"))
        assertFalse(report.contains("null"))
    }
}

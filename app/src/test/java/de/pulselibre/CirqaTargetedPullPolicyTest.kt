package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaTargetedPullPolicyTest {
    private val identity = CirqaTargetedPullPolicy.ItemIdentity("uid-hash", "fit", 128L, 0L)
    private val stableEvidence = CirqaUidStabilityEvidence.from(
        listOf("s1", "s2", "s3").map {
            CirqaUidStabilityEvidence.SessionObservation(
                it, true, true, CirqaTargetedPullPolicy.stabilityKey(identity)
            )
        }
    )

    @Test fun unknownIdentityIsPulled() {
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, emptyList()))
    }

    @Test fun successfulImportIsSkippedOnlyForVerifiedStableIdentity() {
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity, "a".repeat(64), importSucceeded = true, uidStabilityEvidence = stableEvidence
        )
        assertFalse(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
    }

    @Test fun insufficientStabilityEvidenceStillPullsAfterSuccessfulImport() {
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity, "a".repeat(64), importSucceeded = true, uidStabilityEvidence = null
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
    }

    @Test fun incompleteOrInvalidImportRecordStillPulls() {
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity, "not-a-hash", importSucceeded = false, uidStabilityEvidence = stableEvidence
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
    }

    @Test fun mismatchedMetadataStillPulls() {
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity.copy(dataSize = 127L), "a".repeat(64),
            importSucceeded = true, uidStabilityEvidence = stableEvidence
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
    }

    @Test fun malformedRemoteIdentityIsNeverSkipped() {
        val invalid = identity.copy(uidKey = "")
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            invalid, "a".repeat(64), importSucceeded = true, uidStabilityEvidence = stableEvidence
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(invalid, listOf(entry)))
    }

    @Test fun evidenceForAnotherIdentityDoesNotAllowSkip() {
        val otherIdentity = identity.copy(uidKey = "other-uid")
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity, "a".repeat(64), importSucceeded = true,
            uidStabilityEvidence = CirqaUidStabilityEvidence.from(
                listOf("s1", "s2", "s3").map {
                    CirqaUidStabilityEvidence.SessionObservation(
                        it, true, true, CirqaTargetedPullPolicy.stabilityKey(otherIdentity)
                    )
                }
            )
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
    }
}

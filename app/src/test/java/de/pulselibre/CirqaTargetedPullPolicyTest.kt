package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test fun successfulLocalImportAndRepeatedMetadataDoNotProveRemoteFreshness() {
        val entry = CirqaTargetedPullPolicy.CompletedImport(
            identity, "a".repeat(64), importSucceeded = true, uidStabilityEvidence = stableEvidence
        )
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(entry)))
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

    // #148: proven identity = UID + type + size + flags + band transaction ID, plus a verified local original.
    private val withTr = identity.copy(transactionId = 958L)
    private fun done(id: CirqaTargetedPullPolicy.ItemIdentity, local: Boolean = true, ok: Boolean = true, sha: String = "a".repeat(64)) =
        CirqaTargetedPullPolicy.CompletedImport(id, sha, ok, null, local)

    @Test fun verifiedImportWithSameTransactionAndLocalOriginalIsSkipped() {
        assertFalse(CirqaTargetedPullPolicy.shouldPull(withTr, listOf(done(withTr))))
    }

    @Test fun missingLocalOriginalForcesPull() {
        assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr, listOf(done(withTr, local = false))))
    }

    @Test fun localOriginalMustHashToManifestRatherThanOnlyMatchingItsLengthAndName() {
        val directory = java.nio.file.Files.createTempDirectory("targeted-pull-hash").toFile()
        val file = java.io.File(directory, "${"a".repeat(64)}.fit")
        val original = byteArrayOf(1, 2, 3, 4)
        val sha = java.security.MessageDigest.getInstance("SHA-256")
            .digest(original).lowerHex()
        try {
            file.writeBytes(original)
            assertTrue(CirqaTargetedPullPolicy.localCopyMatches(file, original.size.toLong(), sha))

            file.writeBytes(byteArrayOf(4, 3, 2, 1)) // same size, different content
            val verified = CirqaTargetedPullPolicy.localCopyMatches(
                file, original.size.toLong(), sha
            )
            assertFalse(verified)
            assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr, listOf(done(withTr, local = verified))))
        } finally {
            file.delete()
            directory.delete()
        }
    }

    @Test fun symlinkedLocalOriginalCannotAuthorizeSkip() {
        val directory = java.nio.file.Files.createTempDirectory("targeted-pull-link").toFile()
        val target = java.io.File(directory, "target.fit")
        val link = java.io.File(directory, "${"b".repeat(64)}.fit")
        val content = byteArrayOf(7, 8, 9)
        val sha = java.security.MessageDigest.getInstance("SHA-256")
            .digest(content).lowerHex()
        try {
            target.writeBytes(content)
            java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath())
            assertFalse(CirqaTargetedPullPolicy.localCopyMatches(link, content.size.toLong(), sha))
        } finally {
            java.nio.file.Files.deleteIfExists(link.toPath())
            target.delete()
            directory.delete()
        }
    }

    @Test fun changedTransactionOrSizeForcesPull() {
        assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr.copy(transactionId = 959L), listOf(done(withTr))))
        assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr.copy(dataSize = 129L), listOf(done(withTr))))
    }

    @Test fun noTransactionIdMeansNoProofAndAlwaysPulls() {
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(done(identity))))
    }

    @Test fun failedImportOrBadHashForcesPull() {
        assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr, listOf(done(withTr, ok = false))))
        assertTrue(CirqaTargetedPullPolicy.shouldPull(withTr, listOf(done(withTr, sha = "bad"))))
    }

    @Test fun transactionIdIsPartOfTheStabilityKey() {
        assertNotEquals(CirqaTargetedPullPolicy.stabilityKey(identity), CirqaTargetedPullPolicy.stabilityKey(withTr))
        assertEquals(CirqaTargetedPullPolicy.stabilityKey(identity),
            CirqaTargetedPullPolicy.stabilityKey(identity.copy(transactionId = null)))
    }
}

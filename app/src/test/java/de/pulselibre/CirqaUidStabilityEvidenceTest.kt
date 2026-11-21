package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CirqaUidStabilityEvidenceTest {
    private fun observation(session: String, fingerprint: String = "a".repeat(64)) =
        CirqaUidStabilityEvidence.SessionObservation(
            sessionId = session,
            accepted = true,
            itemListReceived = true,
            identityFingerprint = fingerprint
        )

    @Test fun requiresThreeDistinctSuccessfulMatchingLists() {
        val evidence = CirqaUidStabilityEvidence.from(
            listOf(observation("one"), observation("two"), observation("three"))
        )
        assertEquals(3, evidence?.matchingSuccessfulSessions)
        assertEquals("a".repeat(64), evidence?.identityFingerprint)
    }

    @Test fun twoMatchesAreNotSufficient() {
        assertNull(CirqaUidStabilityEvidence.from(listOf(observation("one"), observation("two"))))
    }

    @Test fun mismatchedListDoesNotCreateEvidence() {
        assertNull(
            CirqaUidStabilityEvidence.from(
                listOf(observation("one"), observation("two"), observation("three", "b".repeat(64)))
            )
        )
    }

    @Test fun failedOrDuplicateSessionsDoNotCreateEvidence() {
        val failed = observation("two").copy(accepted = false)
        assertNull(
            CirqaUidStabilityEvidence.from(listOf(observation("one"), failed, observation("three")))
        )
        assertNull(
            CirqaUidStabilityEvidence.from(
                listOf(observation("one"), observation("one"), observation("three"))
            )
        )
    }

    @Test fun malformedDigestDoesNotCreateEvidence() {
        assertNull(
            CirqaUidStabilityEvidence.from(
                listOf(observation("one"), observation("two"), observation("three", "not-a-digest"))
            )
        )
    }
}

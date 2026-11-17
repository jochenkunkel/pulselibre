package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050SyncHypothesisTest {
    @Test fun candidateDiffersOnlyBySyncBitThree() {
        val comparison = Cirqa5050SyncHypothesis.compare()
        assertEquals(15, comparison.baseline.size)
        assertEquals(15, comparison.syncCandidate.size)
        assertEquals(listOf(3), comparison.changedBitPositions)
        assertTrue(comparison.baseline.all { it == 0.toByte() })
        assertEquals(8, comparison.syncCandidate[0].toInt() and 255)
        assertTrue(comparison.syncCandidate.drop(1).all { it == 0.toByte() })
    }

    @Test fun bothCandidatesHaveValidOfflineGfdiEnvelope() {
        val comparison = Cirqa5050SyncHypothesis.compare()
        for (mask in listOf(comparison.baseline, comparison.syncCandidate)) {
            val shape = Cirqa5050OfflineCapabilities.inspect(
                Cirqa5050OfflineResponse.frame(mask)
            )!!
            assertTrue(shape.lengthMatches)
            assertEquals(15, shape.declaredLength)
        }
    }
}

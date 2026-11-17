package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050AckEvidenceTest {
    @Test fun countsOnly5050Acknowledgements() {
        val statuses = mapOf(
            "request=5050/status=ACK/seq=-1" to 2,
            "request=5050/status=ACK/seq=4" to 1,
            "request=5050/status=NAK/seq=-1" to 3,
            "request=5024/status=ACK/seq=-1" to 5,
            "request=5050/status=ACKNOWLEDGED/seq=-1" to 9
        )
        assertEquals(3, Cirqa5050AckEvidence.count(statuses))
    }

    @Test fun oldAckIsNotEvidenceForNewResponse() {
        val previous = mapOf("request=5050/status=ACK/seq=-1" to 1)
        assertFalse(Cirqa5050AckEvidence.isNewAck(1, previous))
        assertTrue(Cirqa5050AckEvidence.isNewAck(1,
            mapOf("request=5050/status=ACK/seq=-1" to 2)))
    }

    @Test fun noAckAndInvalidBaselineFailClosed() {
        assertFalse(Cirqa5050AckEvidence.isNewAck(0,
            mapOf("request=5050/status=NAK/seq=-1" to 1)))
        assertFalse(Cirqa5050AckEvidence.isNewAck(-1,
            mapOf("request=5050/status=ACK/seq=-1" to 1)))
    }
}

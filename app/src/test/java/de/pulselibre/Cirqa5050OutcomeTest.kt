package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050OutcomeTest {
    @Test fun noResponseQueuedIsNotProgress() {
        assertEquals(Cirqa5050Outcome.Result.NO_RESPONSE_QUEUED,
            Cirqa5050Outcome.classify(false, true, true, true))
    }

    @Test fun partialWritesCannotClaimAckOrSync() {
        assertEquals(Cirqa5050Outcome.Result.WRITE_INCOMPLETE,
            Cirqa5050Outcome.classify(true, false, true, true))
    }

    @Test fun ackMustBeFresh() {
        assertEquals(Cirqa5050Outcome.Result.NO_FRESH_5050_ACK,
            Cirqa5050Outcome.classify(true, true, false, true))
    }

    @Test fun ackWithout5037IsNotHistoricalSync() {
        assertEquals(Cirqa5050Outcome.Result.ACK_WITHOUT_5037,
            Cirqa5050Outcome.classify(true, true, true, false))
    }

    @Test fun valid5037OnlyMeansSignalObserved() {
        assertEquals(Cirqa5050Outcome.Result.VALID_5037_SIGNAL,
            Cirqa5050Outcome.classify(true, true, true, true))
    }
}

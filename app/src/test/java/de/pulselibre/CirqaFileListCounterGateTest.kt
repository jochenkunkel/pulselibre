package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaFileListCounterGateTest {
    private fun valid() = CirqaFileListCounterGate.Evidence(
        explicitlyAuthorized = true, configurationWriteConfirmed = true,
        fresh5050Ack = true, sessionId = "session-a",
        counterEvidenceSessionId = "session-a", sequence = 11, counter = 495,
        alreadySent = false, negativeResponseObserved = false
    )
    private fun check(e: CirqaFileListCounterGate.Evidence, expected: CirqaFileListCounterGate.Decision) =
        assertEquals(expected, CirqaFileListCounterGate.decide(e))

    @Test fun permitsOnlyFullyProvenOneShot() =
        check(valid(), CirqaFileListCounterGate.Decision.ALLOW_ONE_SHOT)

    @Test fun deniesMissingAuthorization() =
        check(valid().copy(explicitlyAuthorized = false), CirqaFileListCounterGate.Decision.NOT_AUTHORIZED)

    @Test fun deniesMissingFreshAck() =
        check(valid().copy(fresh5050Ack = false), CirqaFileListCounterGate.Decision.NO_5050_ACK)

    @Test fun deniesUnknownCounters() =
        check(valid().copy(counter = null), CirqaFileListCounterGate.Decision.NO_SESSION_EVIDENCE)

    @Test fun deniesCountersFromAnotherSession() =
        check(valid().copy(counterEvidenceSessionId = "old-session"), CirqaFileListCounterGate.Decision.WRONG_SESSION)

    @Test fun deniesInvalidSequence() =
        check(valid().copy(sequence = 128), CirqaFileListCounterGate.Decision.INVALID_COUNTERS)

    @Test fun deniesRepeatedRequest() =
        check(valid().copy(alreadySent = true), CirqaFileListCounterGate.Decision.ALREADY_SENT)

    @Test fun deniesAfterNegativeResponse() =
        check(valid().copy(negativeResponseObserved = true), CirqaFileListCounterGate.Decision.NEGATIVE_RESPONSE)
}

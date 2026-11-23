package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic, offline tests for sync terminal/callback accounting. */
class CirqaSyncInstrumentationTest {
    @Test fun timeoutWithQueuedFileDoesNotChangeContinuationState() {
        val ledger = CirqaSyncPipelineLedger()
        val id = ledger.select()
        ledger.record(id, CirqaSyncPipelineLedger.Stage.QUEUED)
        val continuation = CirqaSyncContinuationRequest()

        // A timeout is a terminal observation, not a protocol response.
        assertEquals("none", continuation.terminalLabel())
        assertEquals(1, ledger.count(CirqaSyncPipelineLedger.Stage.QUEUED))
        assertEquals(0, ledger.count(CirqaSyncPipelineLedger.Stage.SUCCESSFUL))
    }

    @Test fun timeoutDuringTransferLeavesSelectedItemIncomplete() {
        val ledger = CirqaSyncPipelineLedger()
        val id = ledger.select()
        ledger.record(id, CirqaSyncPipelineLedger.Stage.QUEUED)
        ledger.record(id, CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED)
        ledger.record(id, CirqaSyncPipelineLedger.Stage.RESPONSE_RECEIVED)

        assertEquals(0, ledger.count(CirqaSyncPipelineLedger.Stage.TRANSFER_COMPLETED))
        assertEquals(0, ledger.count(CirqaSyncPipelineLedger.Stage.SUCCESSFUL))
    }

    @Test fun timeoutDuringSessionPagePreservesPendingState() {
        val continuation = CirqaSyncContinuationRequest()
        continuation.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 7)

        assertEquals("session", continuation.terminalLabel())
        assertEquals("session", continuation.terminalLabel())
    }

    @Test fun timeoutDuringTransactionFollowupPreservesDifferentPendingState() {
        val continuation = CirqaSyncContinuationRequest()
        continuation.enqueue(CirqaSyncContinuationRequest.Kind.TRANSACTION_FOLLOWUP, 8)

        assertEquals("transaction", continuation.terminalLabel())
    }

    @Test fun rejectedBleWriteIsNotCountedAcceptedAndCorrelationBecomesUnknown() {
        val continuation = CirqaSyncContinuationRequest()
        continuation.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 9)
        continuation.observeWriteRejected(9)

        assertEquals("unknown", continuation.terminalLabel())
    }

    @Test fun duplicateCallbacksAreIdempotentAndTimingUsesMonotonicElapsedTime() {
        val ledger = CirqaSyncPipelineLedger()
        val id = ledger.select()
        assertTrue(ledger.record(id, CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED))
        assertFalse(ledger.record(id, CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED))
        assertEquals(1, ledger.count(CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED))

        var now = 100L
        val timing = CirqaSyncTiming { now }
        assertTrue(timing.start("transfer", CirqaSyncTiming.Stage.FILE_TRANSFER))
        now = 250L
        assertEquals(150L, timing.stop("transfer"))
        assertNull(timing.stop("transfer"))
        assertEquals(
            CirqaSyncTiming.Measurement(count = 1, totalNanos = 150L),
            timing.snapshot().getValue(CirqaSyncTiming.Stage.FILE_TRANSFER)
        )
    }

    @Test fun matchingAcceptedResponseClearsContinuationButUnmatchedDoesNot() {
        val continuation = CirqaSyncContinuationRequest()
        continuation.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 11)
        continuation.observeResponse(12, accepted = true)
        assertEquals("session", continuation.terminalLabel())
        continuation.observeResponse(11, accepted = true)
        assertEquals("none", continuation.terminalLabel())
    }

    @Test fun missingResponseCorrelationIsUnknown() {
        val continuation = CirqaSyncContinuationRequest()
        continuation.enqueue(CirqaSyncContinuationRequest.Kind.TRANSACTION_FOLLOWUP, 13)
        continuation.observeResponse(null, accepted = true)
        assertEquals("unknown", continuation.terminalLabel())
    }
}

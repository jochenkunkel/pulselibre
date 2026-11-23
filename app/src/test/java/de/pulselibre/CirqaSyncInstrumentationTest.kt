package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic, offline tests against the same transition helpers used in production. */
class CirqaSyncInstrumentationTest {
    @Test fun sessionPageTimeoutAndLateResponseKeepTimeoutTerminal() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 7)
        assertEquals("session-queued", state.terminalLabel())
        state.writeAccepted(7)
        assertEquals("session-write-accepted", state.terminalLabel())
        state.timeout()

        // A response racing with terminal finish must not rewrite the snapshot.
        state.observeResponse(7, accepted = true)
        state.complete(7)
        assertEquals("session-timeout", state.terminalLabel())
        assertFalse(state.isNormallyResolved())
    }

    @Test fun transactionFollowupTimeoutRetainsKindAndPhase() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.TRANSACTION_FOLLOWUP, 8)
        state.writeAccepted(8)
        state.timeout()

        assertEquals("transaction-timeout", state.terminalLabel())
        assertFalse(state.isOutstanding())
        assertFalse(state.isNormallyResolved())
    }

    @Test fun timeoutDuringFileTransferClosesMeasuredIntervalWithoutCallingItSuccessful() {
        var now = 1_000L
        val timing = CirqaSyncTiming { now }
        val pipeline = CirqaSyncPipelineLedger()
        val id = pipeline.select()
        pipeline.record(id, CirqaSyncPipelineLedger.Stage.QUEUED)
        pipeline.record(id, CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED)
        timing.start("file-transfer", CirqaSyncTiming.Stage.FILE_TRANSFER)

        now += 120_000_000_000L
        timing.stopAll()
        assertEquals(0, pipeline.count(CirqaSyncPipelineLedger.Stage.SUCCESSFUL))
        assertEquals(
            CirqaSyncTiming.Measurement(count = 1, totalNanos = 120_000_000_000L),
            timing.snapshot().getValue(CirqaSyncTiming.Stage.FILE_TRANSFER)
        )
    }

    @Test fun matchingResponseHasDistinctReceivedAndCompletedTransitions() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 9)
        state.writeAccepted(9)
        state.observeResponse(9, accepted = true)
        assertEquals("session-response-received", state.terminalLabel())
        assertTrue(state.isOutstanding())
        state.complete(9)
        assertEquals("session-completed", state.terminalLabel())
        assertFalse(state.isOutstanding())
    }

    @Test fun unmatchedAndDuplicateResponsesCannotCompleteAnotherRequest() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.TRANSACTION_FOLLOWUP, 10)
        state.writeAccepted(10)
        state.observeResponse(11, accepted = true)
        assertEquals("transaction-write-accepted", state.terminalLabel())
        state.observeResponse(10, accepted = true)
        state.complete(10)
        // A duplicate accepted response after completion is idempotent.
        state.observeResponse(10, accepted = true)
        state.complete(10)
        assertEquals("transaction-completed", state.terminalLabel())
    }

    @Test fun failedBleWriteIsUnknownNotAccepted() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.SESSION_PAGE, 12)
        state.writeRejected(12)

        assertEquals("unknown", state.terminalLabel())
        assertTrue(state.isOutstanding())
    }

    @Test fun asynchronousWriteFailureBecomesUnknownAndDuplicateFinishPreservesFirstReport() {
        val state = CirqaSyncContinuationRequest()
        state.enqueue(CirqaSyncContinuationRequest.Kind.TRANSACTION_FOLLOWUP, 13)
        state.writeAccepted(13)
        state.writeFailed(13)
        assertEquals("unknown", state.terminalLabel())

        val finalizer = CirqaSyncTerminalFinalizer()
        val first = finalizer.finish({ "first-terminal-snapshot" }, { "fallback" })
        val repeated = finalizer.finish({ error("must not rebuild") }, { "must not fallback" })
        assertEquals("first-terminal-snapshot", first)
        assertEquals(first, repeated)
    }

    @Test fun diagnosticFailureFallsBackAndUnknownCauseIsSanitized() {
        val finalizer = CirqaSyncTerminalFinalizer()
        val report = finalizer.finish(
            { error("diagnostic failure") },
            { "terminalCause=${CirqaSyncTerminalEvidence.cause("unknown", "unsafe reason !", false)}" }
        )
        assertEquals("terminalCause=unknown", report)
        assertEquals("unknown", CirqaSyncTerminalEvidence.cause("x", "unsafe reason !", false))
        assertEquals("session-time-limit", CirqaSyncTerminalEvidence.cause(
            "120-s-Zeitlimit", "unsafe reason !", false
        ))
    }

    @Test fun monotonicTimingIsIdempotentAndClosesOpenPhasesAtFinish() {
        var now = 100L
        val timing = CirqaSyncTiming { now }
        assertTrue(timing.start("bootstrap", CirqaSyncTiming.Stage.BOOTSTRAP))
        now = 150
        assertEquals(50L, timing.stop("bootstrap"))
        assertNull(timing.stop("bootstrap"))

        timing.start("item-list", CirqaSyncTiming.Stage.ITEM_LIST_RESPONSE)
        now = 225
        timing.stopAll()
        assertEquals(
            CirqaSyncTiming.Measurement(count = 1, totalNanos = 75L),
            timing.snapshot().getValue(CirqaSyncTiming.Stage.ITEM_LIST_RESPONSE)
        )
    }

    @Test fun orderedPipelineAndPerItemTimingExposeOnlyLocalOrdinals() {
        val pipeline = CirqaSyncPipelineLedger()
        val ordinal = pipeline.select()
        pipeline.record(ordinal, CirqaSyncPipelineLedger.Stage.QUEUED)
        assertEquals("1:3", pipeline.orderedSummary())

        var now = 0L
        val timing = CirqaSyncTiming { now }
        timing.start("item:$ordinal:transfer", CirqaSyncTiming.Stage.FILE_TRANSFER)
        now = 2_500_000
        timing.stop("item:$ordinal:transfer")
        timing.start("list:4242", CirqaSyncTiming.Stage.SESSION_PAGE)
        now = 7_500_000
        timing.stopAll()

        assertEquals("item1[transfer:2ms]", timing.perItemSummary())
        assertFalse(timing.perItemSummary().contains("4242"))
    }

    @Test fun lateBleFailureCannotOverwriteCorrelatedApplicationResponse() {
        val pipeline = CirqaSyncPipelineLedger()
        val responded = pipeline.select()
        pipeline.record(responded, CirqaSyncPipelineLedger.Stage.BLE_WRITE_ACCEPTED)
        pipeline.record(responded, CirqaSyncPipelineLedger.Stage.RESPONSE_RECEIVED)
        assertFalse(pipeline.failWriteIfNoResponse(responded))
        assertEquals(0, pipeline.count(CirqaSyncPipelineLedger.Stage.FAILED))

        val unanswered = pipeline.select()
        assertTrue(pipeline.failWriteIfNoResponse(unanswered))
        assertFalse(pipeline.failWriteIfNoResponse(unanswered))
        assertEquals(1, pipeline.count(CirqaSyncPipelineLedger.Stage.FAILED))
    }

    @Test fun archiveOutcomeDistinguishesPreExistingContentWithoutExposingIdentity() {
        val pipeline = CirqaSyncPipelineLedger()
        val old = pipeline.select()
        val fresh = pipeline.select()
        pipeline.record(old, CirqaSyncPipelineLedger.Stage.ARCHIVED)
        pipeline.record(old, CirqaSyncPipelineLedger.Stage.ARCHIVE_CONTENT_PREEXISTED)
        pipeline.record(fresh, CirqaSyncPipelineLedger.Stage.ARCHIVED)
        pipeline.record(fresh, CirqaSyncPipelineLedger.Stage.ARCHIVE_CONTENT_NEW)

        assertEquals(1, pipeline.count(CirqaSyncPipelineLedger.Stage.ARCHIVE_CONTENT_PREEXISTED))
        assertEquals(1, pipeline.count(CirqaSyncPipelineLedger.Stage.ARCHIVE_CONTENT_NEW))
        assertTrue(pipeline.orderedSummary().contains("1:"))
        assertFalse(pipeline.orderedSummary().contains("sha"))
    }
}

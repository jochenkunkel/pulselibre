package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncTimeoutFinalizerTest {
    @Test fun deadlineMarksOnlyTheCurrentItemFailedAndIsIdempotent() {
        val ledger = CirqaSyncPipelineLedger()
        val current = ledger.select()
        val pending = ledger.select()
        ledger.record(current, CirqaSyncPipelineLedger.Stage.QUEUED)
        ledger.record(current, CirqaSyncPipelineLedger.Stage.RESPONSE_RECEIVED)
        ledger.record(pending, CirqaSyncPipelineLedger.Stage.QUEUED)

        assertTrue(CirqaSyncTimeoutFinalizer.failCurrentItem(current, ledger))
        assertFalse(CirqaSyncTimeoutFinalizer.failCurrentItem(current, ledger))
        assertFalse(CirqaSyncTimeoutFinalizer.failCurrentItem(null, ledger))
        assertEquals(1, ledger.count(CirqaSyncPipelineLedger.Stage.FAILED))
        assertEquals(0, ledger.count(CirqaSyncPipelineLedger.Stage.TRANSFER_COMPLETED))
    }
}

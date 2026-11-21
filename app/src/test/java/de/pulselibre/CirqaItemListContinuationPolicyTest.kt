package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaItemListContinuationPolicyTest {
    @Test fun sessionCursorTakesPrecedenceOverTransactionContext() {
        val policy = CirqaItemListContinuationPolicy()

        assertEquals(
            CirqaItemListContinuationPolicy.Decision.SessionPage(9),
            policy.next(sessionId = 9, nextTransactionId = 42)
        )
        assertEquals(0, policy.transactionFollowups)
    }

    @Test fun allowsExactlyOneSeparateTransactionFollowup() {
        val policy = CirqaItemListContinuationPolicy()

        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(true, policy.recordTransactionFollowupSent())
        assertEquals(1, policy.transactionFollowups)
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            policy.next(sessionId = null, nextTransactionId = 43)
        )
        assertEquals(1, policy.transactionFollowups)
    }

    @Test fun followupResponseWithNoTransactionCompletesBoundedListing() {
        val policy = CirqaItemListContinuationPolicy()
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(true, policy.recordTransactionFollowupSent())

        assertEquals(
            CirqaItemListContinuationPolicy.Decision.Complete,
            policy.next(sessionId = null, nextTransactionId = null)
        )
        assertEquals(1, policy.transactionFollowups)
    }

    @Test fun emptyFollowupStillAdvertisingTransactionDoesNotLoop() {
        val policy = CirqaItemListContinuationPolicy()
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(true, policy.recordTransactionFollowupSent())

        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(1, policy.transactionFollowups)
    }

    @Test fun resetStartsANewBoundedProductRun() {
        val policy = CirqaItemListContinuationPolicy()
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(true, policy.recordTransactionFollowupSent())
        policy.reset()

        assertEquals(0, policy.transactionFollowups)
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(43),
            policy.next(sessionId = null, nextTransactionId = 43)
        )
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test fun sessionPageAndTransactionFollowupAreSequentialDifferentRequests() {
        val policy = CirqaItemListContinuationPolicy()

        // Even when both fields occur together, session_id continues this list first.
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.SessionPage(9),
            policy.next(sessionId = 9, nextTransactionId = 42)
        )
        assertEquals(0, policy.transactionFollowups)

        // Once the session cursor disappears, next_transaction_id is a separate
        // transaction context; an empty response does not make it a session page.
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertTrue(policy.recordTransactionFollowupSent())
        assertEquals(1, policy.transactionFollowups)
    }

    @Test fun allowsBoundedSeparateTransactionFollowups() {
        val policy = CirqaItemListContinuationPolicy()
        val limit = CirqaItemListContinuationPolicy.CONNECT_OBSERVED_TRANSACTION_FOLLOWUPS

        repeat(limit) { index ->
            assertEquals(
                CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42 + index.toLong()),
                policy.next(sessionId = null, nextTransactionId = 42 + index.toLong())
            )
            assertEquals(true, policy.recordTransactionFollowupSent())
            assertEquals(index + 1, policy.transactionFollowups)
        }
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            policy.next(sessionId = null, nextTransactionId = 99)
        )
        assertEquals(limit, policy.transactionFollowups)
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

    @Test fun emptyFollowupsStillAdvertisingTransactionDoNotLoop() {
        val policy = CirqaItemListContinuationPolicy()
        val limit = CirqaItemListContinuationPolicy.CONNECT_OBSERVED_TRANSACTION_FOLLOWUPS

        repeat(limit) {
            assertEquals(
                CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
                policy.next(sessionId = null, nextTransactionId = 42)
            )
            assertEquals(true, policy.recordTransactionFollowupSent())
        }

        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            policy.next(sessionId = null, nextTransactionId = 42)
        )
        assertEquals(limit, policy.transactionFollowups)
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

    /**
     * Offline replay of the structure observed in the redacted S10 Garmin
     * Connect HCI (2026-11-22): one populated page followed by three empty
     * pages that repeat the same next_transaction token. All values below are
     * synthetic placeholders — the public fixture reproduces only the wire
     * shape, never real tokens, UIDs, sizes, device IDs or health values.
     */
    @Test fun replayObservedConnectPatternStopsIncompleteAtBoundedFollowupLimit() {
        val token = 4242L
        val pages = listOf(1, 0, 0, 0).map { itemCount -> syntheticPage(itemCount, token) }
        val policy = CirqaItemListContinuationPolicy()
        val progress = mutableListOf<CirqaItemListProgress.Next>()
        val sentFollowups = mutableListOf<Long>()

        progress.add(CirqaItemListProgress.next(pages[0]))
        for (page in pages.drop(1)) {
            val decision = policy.next(page.sessionId, page.nextTransactionId)
            check(decision is CirqaItemListContinuationPolicy.Decision.TransactionFollowup)
            assertTrue(policy.recordTransactionFollowupSent())
            sentFollowups.add(decision.transactionId)
            progress.add(CirqaItemListProgress.next(page))
        }
        val finalDecision = policy.next(pages.last().sessionId, pages.last().nextTransactionId)

        assertEquals(3, sentFollowups.size)
        assertEquals(listOf(token, token, token), sentFollowups)
        assertEquals(
            List(4) { CirqaItemListProgress.Next.TRANSACTION_PENDING },
            progress
        )
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            finalDecision
        )
        assertEquals(1, pages[0].items.size)
        assertEquals(0, pages[1].items.size)
        assertEquals(null, pages[1].sessionId)
        assertEquals(0L, pages[1].status)
    }

    @Test fun emptySessionPageAndEmptyTransactionFollowupRemainDifferent() {
        val policy = CirqaItemListContinuationPolicy()
        val emptySessionPage = syntheticPage(itemCount = 0, token = 42, sessionId = 9)
        assertEquals(CirqaItemListProgress.Next.SESSION_PAGE, CirqaItemListProgress.next(emptySessionPage))
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.SessionPage(9),
            policy.next(emptySessionPage.sessionId, emptySessionPage.nextTransactionId)
        )
        assertEquals(0, policy.transactionFollowups)

        val emptyTransactionPage = syntheticPage(itemCount = 0, token = 42, sessionId = null)
        assertEquals(
            CirqaItemListProgress.Next.TRANSACTION_PENDING,
            CirqaItemListProgress.next(emptyTransactionPage)
        )
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionFollowup(42),
            policy.next(emptyTransactionPage.sessionId, emptyTransactionPage.nextTransactionId)
        )
        assertEquals(0, policy.transactionFollowups)
    }

    /** Smart{43 = Service{10 = ItemListResponse{2 = session, 3 = token, 4 = refs}}} */
    private fun syntheticPage(
        itemCount: Int,
        token: Long,
        sessionId: Long? = null
    ): CirqaFileAccessItemListParser.ItemListPage {
        var items = byteArrayOf()
        repeat(itemCount) {
            val ref = bytesField(1, ByteArray(18) { (it + 1).toByte() }) +
                bytesField(2, byteArrayOf(0x08, 0x01)) +
                varintField(3, 4096)
            items += bytesField(4, ref)
        }
        // Garmin Connect sends status=0 explicitly on the wire (verified in HCI);
        // proto3 would otherwise omit the default and the parser fails closed.
        val response = varintField(1, 0) +
            (sessionId?.let { varintField(2, it) } ?: byteArrayOf()) +
            varintField(3, token) + items
        val smart = bytesField(43, bytesField(10, response))
        val page = CirqaFileAccessItemListParser.parsePage(smart)
        assertNotNull(page)
        return page!!
    }

    private fun varintField(number: Int, value: Long): ByteArray = varint((number shl 3).toLong()) + varint(value)

    private fun bytesField(number: Int, value: ByteArray): ByteArray =
        varint(((number shl 3) or 2).toLong()) + varint(value.size.toLong()) + value

    private fun varint(value: Long): ByteArray {
        require(value >= 0)
        var remaining = value
        val out = ArrayList<Byte>()
        do {
            val b = (remaining and 127L).toInt()
            remaining = remaining ushr 7
            out.add((if (remaining == 0L) b else b or 128).toByte())
        } while (remaining != 0L)
        return out.toByteArray()
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline end-to-end replay of the S10 (2026-11-22) FileAccess transaction shape.
 *
 * This is a pure in-memory model: parse a page -> decide continuation -> build
 * the next read-only pull request -> parse the pull result -> repeat the bounded
 * follow-ups -> stop. No BLE, no device, no raw capture. All wire values are
 * synthetic placeholders; only the structure mirrors the observed HCI.
 */
class CirqaS10OfflineTransactionE2ETest {
    private val token = 4242L
    private val syntheticSize = 4096L

    @Test fun parsesPageBuildsPullRequestAndDrainsBoundedFollowups() {
        val policy = CirqaItemListContinuationPolicy()
        val firstPage = page(itemCount = 1, nextTransaction = token)
        val parsed = CirqaFileAccessItemListParser.parsePage(firstPage)
        assertNotNull(parsed)
        parsed!!

        // 1. Page structure and progress as observed: status=0, one item, token pending.
        assertEquals(0L, parsed.status)
        assertEquals(1, parsed.items.size)
        assertEquals(token, parsed.nextTransactionId)
        assertEquals(CirqaItemListProgress.Next.TRANSACTION_PENDING, CirqaItemListProgress.next(parsed))

        // 2. The item from the page drives a read-only pull request with a valid
        //    GFDI 5043 frame; the exact item reference is preserved on the wire.
        val pullFrame = CirqaFileAccessPullRequest.gfdiFrame(1, CirqaFileAccessPullRequest.smart(parsed.items.first()))
        val inspected = GarminFrameValidator.inspectVerified(pullFrame)
        assertNotNull(inspected)
        assertEquals(5043, GarminGfdiHeader.inspect(pullFrame)!!.type)
        val body = pullFrame.copyOfRange(4, pullFrame.size - 2)
        val smart = body.copyOfRange(14, body.size)
        assertEquals(CirqaFileAccessPullRequest.smart(parsed.items.first()).size, smart.size)

        // 3. Reference-client-shaped pull result: success, default transport,
        //    compression window 11 (all protocol codes, no health data).
        val response = varintField(1, 0) + varintField(2, 0) + varintField(7, 11)
        val pullResult = CirqaFileAccessItemListParser.parsePullResult(
            bytesField(43, bytesField(2, response))
        )!!
        assertEquals(0L, pullResult.resultCode)
        assertEquals(0L, pullResult.transport)
        assertEquals(11L, pullResult.compressionWindow)

        // 4. Three empty follow-up pages repeat the unchanged token. The policy
        //    follows all three and then stops, still treating the run incomplete.
        val followups = List(3) { page(itemCount = 0, nextTransaction = token) }
        val issued = mutableListOf<Long>()
        for (followup in followups) {
            val followupPage = CirqaFileAccessItemListParser.parsePage(followup)!!
            assertEquals(0, followupPage.items.size)
            assertEquals(CirqaItemListProgress.Next.TRANSACTION_PENDING, CirqaItemListProgress.next(followupPage))
            val decision = policy.next(followupPage.sessionId, followupPage.nextTransactionId)
            check(decision is CirqaItemListContinuationPolicy.Decision.TransactionFollowup)
            assertTrue(policy.recordTransactionFollowupSent())
            issued.add(decision.transactionId)
        }
        assertEquals(listOf(token, token, token), issued)
        assertEquals(3, policy.transactionFollowups)
        val last = CirqaFileAccessItemListParser.parsePage(followups.last())!!
        assertEquals(
            CirqaItemListContinuationPolicy.Decision.TransactionLimitReached,
            policy.next(last.sessionId, last.nextTransactionId)
        )
    }

    /** Smart{43 = Service{10 = ItemListResponse{1 = 0, 3 = token, 4 = ItemReference...}}} */
    private fun page(itemCount: Int, nextTransaction: Long): ByteArray {
        var items = byteArrayOf()
        repeat(itemCount) {
            val ref = bytesField(1, ByteArray(16) { (it + 1).toByte() }) +
                bytesField(2, byteArrayOf(0x08, 0x01)) +
                varintField(3, syntheticSize) +
                varintField(4, 0)
            items += bytesField(4, ref)
        }
        val response = varintField(1, 0) + varintField(3, nextTransaction) + items
        return bytesField(43, bytesField(10, response))
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

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaItemListProgressTest {
    private fun page(
        status: Long? = CirqaFileAccessListRequest.ITEM_LIST_SUCCESS_STATUS,
        sessionId: Long? = null,
        nextTransactionId: Long? = null
    ) = CirqaFileAccessItemListParser.ItemListPage(
        items = emptyList(),
        status = status,
        sessionId = sessionId,
        nextTransactionId = nextTransactionId,
        gdxmlStrings = emptyMap()
    )

    @Test fun responseWithSessionIdContinuesCurrentSession() {
        assertEquals(CirqaItemListProgress.Next.SESSION_PAGE, CirqaItemListProgress.next(page(sessionId = 9)))
    }

    @Test fun sessionPaginationTakesPrecedenceOverTransactionToken() {
        assertEquals(
            CirqaItemListProgress.Next.SESSION_PAGE,
            CirqaItemListProgress.next(page(sessionId = 9, nextTransactionId = 12))
        )
    }

    @Test fun missingSessionWithTransactionTokenIsNotComplete() {
        assertEquals(
            CirqaItemListProgress.Next.TRANSACTION_PENDING,
            CirqaItemListProgress.next(page(nextTransactionId = 12))
        )
    }

    @Test fun missingBothContextsIsComplete() {
        assertEquals(CirqaItemListProgress.Next.COMPLETE, CirqaItemListProgress.next(page()))
    }

    @Test fun nonSuccessOrMissingStatusFailsClosed() {
        assertEquals(
            CirqaItemListProgress.Next.FAILED,
            CirqaItemListProgress.next(page(status = 1, sessionId = 9))
        )
        assertEquals(
            CirqaItemListProgress.Next.FAILED,
            CirqaItemListProgress.next(page(status = null))
        )
    }
}

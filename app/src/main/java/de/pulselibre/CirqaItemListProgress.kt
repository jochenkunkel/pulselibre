package de.pulselibre

/**
 * Interprets a FileAccess ItemList response without conflating session
 * pagination with a separate transaction token. This model never emits BLE.
 */
internal object CirqaItemListProgress {
    enum class Next {
        FAILED,
        SESSION_PAGE,
        TRANSACTION_PENDING,
        COMPLETE
    }

    fun next(page: CirqaFileAccessItemListParser.ItemListPage): Next {
        if (page.status != CirqaFileAccessListRequest.ITEM_LIST_SUCCESS_STATUS) {
            return Next.FAILED
        }
        return when {
            page.sessionId != null -> Next.SESSION_PAGE
            page.nextTransactionId != null -> Next.TRANSACTION_PENDING
            else -> Next.COMPLETE
        }
    }
}

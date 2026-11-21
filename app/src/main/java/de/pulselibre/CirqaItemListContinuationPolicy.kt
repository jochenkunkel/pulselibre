package de.pulselibre

/**
 * Bounds product ItemList continuation: follow session cursors normally and
 * follow a repeated separate transaction token only a small, fixed number of
 * times per user sync.
 *
 * The limit is derived from the redacted S10 Garmin Connect HCI (2026-11-22):
 * after one populated page the reference client issued three further
 * `item_list_request`s carrying the unchanged `next_transaction_id`, all of
 * which returned `status=0` with zero items, and then stopped. Reproducing
 * that bounded traversal closes the gap where PulsLibre stopped after a
 * single empty follow-up and could miss items on a later transaction page.
 *
 * This policy does not imply complete device-history coverage. A pending
 * transaction after the bounded follow-ups remains explicitly incomplete.
 */
internal class CirqaItemListContinuationPolicy(
    private val maxTransactionFollowups: Int = CONNECT_OBSERVED_TRANSACTION_FOLLOWUPS
) {
    sealed interface Decision {
        data class SessionPage(val sessionId: Long) : Decision
        data class TransactionFollowup(val transactionId: Long) : Decision
        data object Complete : Decision
        data object TransactionLimitReached : Decision
    }

    var transactionFollowups: Int = 0
        private set

    init {
        require(maxTransactionFollowups >= 0)
    }

    fun next(sessionId: Long?, nextTransactionId: Long?): Decision {
        if (sessionId != null) return Decision.SessionPage(sessionId)
        if (nextTransactionId == null) return Decision.Complete
        if (transactionFollowups >= maxTransactionFollowups) {
            return Decision.TransactionLimitReached
        }
        return Decision.TransactionFollowup(nextTransactionId)
    }

    fun recordTransactionFollowupSent(): Boolean {
        if (transactionFollowups >= maxTransactionFollowups) return false
        transactionFollowups++
        return true
    }

    fun reset() {
        transactionFollowups = 0
    }

    companion object {
        /** Observed reference-client follow-up count on the S10 HCI, not a guess. */
        const val CONNECT_OBSERVED_TRANSACTION_FOLLOWUPS = 3
    }
}

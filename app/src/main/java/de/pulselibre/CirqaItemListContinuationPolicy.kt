package de.pulselibre

/**
 * Bounds product ItemList continuation: follow session cursors normally and
 * allow at most one separate transaction-context request per user sync.
 *
 * This policy does not imply complete device-history coverage. A pending
 * transaction after the bounded follow-up remains explicitly incomplete.
 */
internal class CirqaItemListContinuationPolicy(
    private val maxTransactionFollowups: Int = 1
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
}

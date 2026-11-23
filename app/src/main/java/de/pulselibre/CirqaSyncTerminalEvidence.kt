package de.pulselibre

/**
 * Privacy-redacted terminal snapshot. It distinguishes selected work from
 * completed work and from an outstanding list continuation, without exposing
 * file IDs, transaction/session tokens, hashes, or payload data.
 */
internal object CirqaSyncTerminalEvidence {
    data class Snapshot(
        val terminalCause: String,
        val listResponses: Int,
        val listedItems: Int,
        val sessionPageRequestsQueued: Int,
        val transactionFollowupsQueued: Int,
        val lastTransactionResponseItems: Int?,
        val selectedItems: Int,
        val pullRequestsQueued: Int,
        val completedItems: Int,
        val failedItems: Int,
        val pendingQueueItems: Int,
        val currentItemPresent: Boolean,
        val currentExpectedBytes: Long,
        val currentReceivedBytes: Int,
        val sessionCursorPresent: Boolean,
        val transactionTokenPresent: Boolean,
        val continuationRequestState: String,
        val currentItemOrdinal: Long? = null
    ) {
        init {
            require(listResponses >= 0 && listedItems >= 0 && sessionPageRequestsQueued >= 0)
            require(transactionFollowupsQueued >= 0 && (lastTransactionResponseItems == null || lastTransactionResponseItems >= 0))
            require(selectedItems >= 0 && pullRequestsQueued >= 0 && completedItems >= 0 && failedItems >= 0)
            require(pendingQueueItems >= 0 && currentExpectedBytes >= 0 && currentReceivedBytes >= 0)
            require(currentItemOrdinal == null || currentItemOrdinal >= 1)
            require(continuationRequestState in setOf(
                "none", "unknown",
                "session-queued", "session-write-accepted", "session-response-received",
                "session-completed", "session-timeout", "session-aborted",
                "transaction-queued", "transaction-write-accepted", "transaction-response-received",
                "transaction-completed", "transaction-timeout", "transaction-aborted"
            ))
            require(terminalCause.matches(Regex("[a-z0-9-]{1,48}")))
        }

        fun format(): String =
            "cause=$terminalCause,listResponses=$listResponses,listed=$listedItems," +
                "sessionPageRequestsQueued=$sessionPageRequestsQueued," +
                "transactionFollowupsQueued=$transactionFollowupsQueued," +
                "lastTransactionResponseItems=${lastTransactionResponseItems ?: "none"}," +
                "selected=$selectedItems,pullRequestsQueued=$pullRequestsQueued,completed=$completedItems," +
                "failed=$failedItems,pendingQueue=$pendingQueueItems,currentItem=$currentItemPresent," +
                "currentExpectedBytes=$currentExpectedBytes,currentReceivedBytes=$currentReceivedBytes," +
                "sessionCursorPresent=$sessionCursorPresent,transactionTokenPresent=$transactionTokenPresent," +
                "continuationRequestState=$continuationRequestState," +
                "currentItemOrdinal=${currentItemOrdinal ?: "none"}"
    }

    fun cause(message: String, incompleteReason: String, runDrained: Boolean): String {
        val proposed = when {
            message.startsWith("120-s-Zeitlimit") -> "session-time-limit"
            message.startsWith("40-s-Zeitlimit") -> "diagnostic-time-limit"
            incompleteReason != "none" -> incompleteReason
            runDrained -> "drained"
            else -> "not-drained"
        }
        return proposed.takeIf { it.matches(Regex("[a-z0-9-]{1,48}")) } ?: "unknown"
    }
}

package de.pulselibre

/**
 * Correlates one queued list continuation with its application-level response.
 * Request IDs stay in memory only and are never included in reports.
 */
internal class CirqaSyncContinuationRequest {
    enum class Kind { SESSION_PAGE, TRANSACTION_FOLLOWUP }

    sealed interface State {
        data object None : State
        data class Pending(val kind: Kind, val requestId: Int) : State
        data class Unknown(val reason: String) : State
    }

    @Volatile
    private var state: State = State.None

    @Synchronized
    fun enqueue(kind: Kind, requestId: Int): State {
        require(requestId in 0..0xffff)
        state = when (val current = state) {
            State.None -> State.Pending(kind, requestId)
            is State.Pending -> if (current.kind == kind && current.requestId == requestId) current
                else State.Unknown("multiple-continuations")
            is State.Unknown -> current
        }
        return state
    }

    /**
     * Only a unique, matching, successful ItemList response discharges pending
     * state. Unrelated response IDs do not clear it. Missing/malformed matching
     * correlation is made explicitly unknown.
     */
    @Synchronized
    fun observeResponse(requestId: Int?, accepted: Boolean): State {
        val pending = state as? State.Pending ?: return state
        if (requestId == null) {
            state = State.Unknown("response-id-missing")
        } else if (requestId == pending.requestId) {
            state = if (accepted) State.None else State.Unknown("matching-response-rejected")
        }
        return state
    }

    /** An API-rejected write cannot be called pending-on-wire or accepted. */
    @Synchronized
    fun observeWriteRejected(requestId: Int): State {
        val pending = state as? State.Pending ?: return state
        if (pending.requestId == requestId) state = State.Unknown("ble-write-not-accepted")
        return state
    }

    @Synchronized
    fun observeWriteFailed(requestId: Int): State {
        val pending = state as? State.Pending ?: return state
        if (pending.requestId == requestId) state = State.Unknown("gatt-write-failed")
        return state
    }

    @Synchronized
    fun terminalLabel(): String = when (val current = state) {
        State.None -> "none"
        is State.Pending -> when (current.kind) {
            Kind.SESSION_PAGE -> "session"
            Kind.TRANSACTION_FOLLOWUP -> "transaction"
        }
        is State.Unknown -> "unknown"
    }

    @Synchronized
    fun reset() {
        state = State.None
    }
}

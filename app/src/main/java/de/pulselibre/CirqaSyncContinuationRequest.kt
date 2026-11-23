package de.pulselibre

/**
 * Production state machine for one in-flight ItemList continuation.
 *
 * The local 16-bit transport request ID is retained only in memory and is
 * matched against the request ID on a fully reassembled 5044 response. A
 * terminal timeout/abort is preserved, so a late callback cannot rewrite the
 * historical terminal state as success.
 */
internal class CirqaSyncContinuationRequest {
    enum class Kind { SESSION_PAGE, TRANSACTION_FOLLOWUP }
    enum class Phase {
        QUEUED,
        WRITE_ACCEPTED,
        RESPONSE_RECEIVED,
        COMPLETED,
        TIMED_OUT,
        ABORTED
    }

    sealed interface State {
        data object None : State
        data class Tracked(val kind: Kind, val requestId: Int, val phase: Phase) : State
        data class Unknown(val reason: String) : State
    }

    @Volatile
    private var state: State = State.None

    @Synchronized
    fun enqueue(kind: Kind, requestId: Int): State {
        require(requestId in 0..0xffff)
        state = when (val current = state) {
            State.None -> State.Tracked(kind, requestId, Phase.QUEUED)
            is State.Tracked -> when {
                current.kind == kind && current.requestId == requestId -> current
                current.phase == Phase.COMPLETED ||
                    current.phase == Phase.TIMED_OUT ||
                    current.phase == Phase.ABORTED -> State.Tracked(kind, requestId, Phase.QUEUED)
                else -> State.Unknown("multiple-continuations")
            }
            is State.Unknown -> current
        }
        return state
    }

    /** API acceptance is distinct from queuing and from the GATT callback. */
    @Synchronized
    fun writeAccepted(requestId: Int): State = transition(requestId, Phase.QUEUED, Phase.WRITE_ACCEPTED)

    /**
     * Only a fully reassembled response with matching request ID is correlated.
     * A null ID while this request is outstanding is explicitly unknown.
     */
    @Synchronized
    fun observeResponse(requestId: Int?, accepted: Boolean): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.phase in TERMINAL_PHASES) return tracked
        if (requestId == null) {
            state = State.Unknown("response-id-missing")
        } else if (requestId == tracked.requestId) {
            state = if (accepted) tracked.copy(phase = Phase.RESPONSE_RECEIVED)
            else State.Unknown("matching-response-rejected")
        }
        return state
    }

    /** Mark processing complete only after the matching page has been applied. */
    @Synchronized
    fun complete(requestId: Int): State = transition(
        requestId,
        Phase.RESPONSE_RECEIVED,
        Phase.COMPLETED
    )

    @Synchronized
    fun timeout(): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.phase in OUTSTANDING_PHASES) state = tracked.copy(phase = Phase.TIMED_OUT)
        return state
    }

    @Synchronized
    fun abort(): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.phase == Phase.QUEUED || tracked.phase == Phase.WRITE_ACCEPTED) {
            state = tracked.copy(phase = Phase.ABORTED)
        }
        return state
    }

    /** A synchronous GATT API rejection is not a write-accepted request. */
    @Synchronized
    fun writeRejected(requestId: Int): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.requestId == requestId && tracked.phase == Phase.QUEUED) {
            state = State.Unknown("ble-write-not-accepted")
        }
        return state
    }

    /** A failed asynchronous GATT callback invalidates accepted-write certainty. */
    @Synchronized
    fun writeFailed(requestId: Int): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.requestId == requestId && tracked.phase == Phase.WRITE_ACCEPTED) {
            state = State.Unknown("gatt-write-failed")
        }
        return state
    }

    @Synchronized
    fun terminalLabel(): String = when (val current = state) {
        State.None -> "none"
        is State.Unknown -> "unknown"
        is State.Tracked -> when (current.phase) {
            Phase.QUEUED -> "${current.kind.label()}-queued"
            Phase.WRITE_ACCEPTED -> "${current.kind.label()}-write-accepted"
            Phase.RESPONSE_RECEIVED -> "${current.kind.label()}-response-received"
            Phase.COMPLETED -> "${current.kind.label()}-completed"
            Phase.TIMED_OUT -> "${current.kind.label()}-timeout"
            Phase.ABORTED -> "${current.kind.label()}-aborted"
        }
    }

    @Synchronized
    fun isOutstanding(): Boolean =
        (state as? State.Tracked)?.phase in OUTSTANDING_PHASES || state is State.Unknown

    @Synchronized
    fun isNormallyResolved(): Boolean = when (val current = state) {
        State.None -> true
        is State.Unknown -> false
        is State.Tracked -> current.phase == Phase.COMPLETED
    }

    @Synchronized
    fun reset() {
        state = State.None
    }

    private fun transition(requestId: Int, from: Phase, to: Phase): State {
        val tracked = state as? State.Tracked ?: return state
        if (tracked.requestId == requestId && tracked.phase == from) {
            state = tracked.copy(phase = to)
        }
        return state
    }

    private fun Kind.label(): String = when (this) {
        Kind.SESSION_PAGE -> "session"
        Kind.TRANSACTION_FOLLOWUP -> "transaction"
    }

    private companion object {
        val OUTSTANDING_PHASES = setOf(Phase.QUEUED, Phase.WRITE_ACCEPTED, Phase.RESPONSE_RECEIVED)
        val TERMINAL_PHASES = setOf(Phase.COMPLETED, Phase.TIMED_OUT, Phase.ABORTED)
    }
}

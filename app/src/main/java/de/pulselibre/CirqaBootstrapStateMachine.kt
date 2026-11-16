package de.pulselibre

/** Passive handshake tracker: never sends unknown ACKs. */
class CirqaBootstrapStateMachine {
    enum class State { WAITING_DEVICE_INFORMATION, DEVICE_INFORMATION_SEEN, ACK_ATTEMPTED, WAITING_5050_RESPONSE, RETRY_5050_OBSERVED, POST_RETRY_5011_SEEN }
    var state = State.WAITING_DEVICE_INFORMATION
        private set
    var retries = 0
        private set
    var changed5050Payloads = 0
        private set
    var ackAttempted = false
        private set
    private var last5050: ByteArray? = null
    private var last5050AtMs: Long? = null
    // Keep only recent timing evidence; repeated notifications must not grow memory forever.
    private val intervals = ArrayDeque<Long>()
    private val maxIntervals = 32

    fun onAckAttempt() {
        ackAttempted = true
        if (state == State.DEVICE_INFORMATION_SEEN) state = State.ACK_ATTEMPTED
    }

    fun observe(frame: ByteArray, elapsedMs: Long) {
        if (GarminFrameValidator.inspectVerified(frame) == null) return
        val type = (frame[2].toInt() and 255) or ((frame[3].toInt() and 255) shl 8)
        when (type) {
            5024 -> if (state == State.WAITING_DEVICE_INFORMATION) state = State.DEVICE_INFORMATION_SEEN
            5050 -> if (ackAttempted) {
                val body = frame.copyOfRange(4, frame.size - 2)
                if (last5050?.contentEquals(body) == true) {
                    if (retries < Int.MAX_VALUE) retries++
                    last5050AtMs?.let { previous ->
                        if (elapsedMs >= previous) {
                            if (intervals.size == maxIntervals) intervals.removeFirst()
                            intervals.addLast(elapsedMs - previous)
                        }
                    }
                    state = State.RETRY_5050_OBSERVED
                } else {
                    if (last5050 != null && changed5050Payloads < Int.MAX_VALUE) changed5050Payloads++
                    state = State.WAITING_5050_RESPONSE
                }
                last5050 = body
                // Ignore regressing clocks instead of poisoning the next retry interval.
                if (last5050AtMs == null || elapsedMs >= last5050AtMs!!) last5050AtMs = elapsedMs
            }
            5011 -> if (ackAttempted && last5050 != null) state = State.POST_RETRY_5011_SEEN
        }
    }

    fun summary(): String =
        "Handshake-Zustand=" + state +
        "; 5024-ACK versucht=" + ackAttempted +
        "; Anwendungs-ACK nachgewiesen=false" +
        "; 5050-Wiederholungen=" + retries +
        "; 5050-Payloadwechsel=" + changed5050Payloads +
        "; 5s-Retry-Kandidaten=" + intervals.count { it in 4500L..5500L } +
        "; Intervalle-ms=" + intervals.toList() +
        "; 5050-Antwort=NICHT_GESENDET"
}

package de.pulselibre

/**
 * One isolated diagnostic: claim one 5030, inspect one response, then stop.
 * No continuation to 5007/5031/5002 exists. All callbacks carry an identity token;
 * stale callbacks are ignored and can never change the new session's state.
 */
class CirqaLegacy5030OnlySession(val token: Any = Any()) {
    enum class Phase { NEW, BOOTSTRAP, READY, WAITING, OBSERVED, STOPPED }
    var phase = Phase.NEW
        private set
    var reason = "not-started"
        private set
    var metadata: CirqaLegacy5030Response.Metadata? = null
        private set
    private var lastMillis = 0L
    private var sessionDeadline = 0L
    private var responseDeadline = 0L
    private var writeConfirmed = false
    private var collecting = false
    private val encoded = ArrayList<Byte>()
    private var responseSeen = false
    private var totalBytes = 0

    @Synchronized fun begin(gatePersisted: Boolean, now: Long): Boolean {
        if (phase != Phase.NEW || !gatePersisted || now < 0 || now > Long.MAX_VALUE - SESSION_MILLIS) {
            return stop("not-authorized-or-invalid-start")
        }
        lastMillis = now
        sessionDeadline = now + SESSION_MILLIS
        phase = Phase.BOOTSTRAP
        reason = "bootstrap"
        return true
    }

    @Synchronized fun bootstrapReady(callbackToken: Any, confirmed: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.BOOTSTRAP) return stop("unexpected-bootstrap")
        if (!confirmed) return false
        phase = Phase.READY
        return true
    }

    @Synchronized fun claimRequest(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.READY) return stop("duplicate-or-premature-request")
        phase = Phase.WAITING
        responseDeadline = now + minOf(RESPONSE_MILLIS, sessionDeadline - now)
        reason = "awaiting-write-and-response"
        return true
    }

    @Synchronized fun writeCompleted(callbackToken: Any, success: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING || writeConfirmed) return stop("unexpected-write")
        if (!success) return stop("write-failed")
        writeConfirmed = true
        completeIfReady()
        return true
    }

    /** ML handle already stripped. Inspect the entire notification before completing. */
    @Synchronized fun receive(callbackToken: Any, payload: ByteArray, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING) return stop("unexpected-data")
        if (payload.size > MAX_TOTAL_BYTES - totalBytes) return stop("rx-budget")
        totalBytes += payload.size
        for (b in payload) {
            if (b == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    if (responseSeen) return stop("multiple-responses")
                    val frame = decode(encoded.toByteArray()) ?: return stop("bad-cobs")
                    encoded.clear()
                    val m = CirqaLegacy5030Response.inspect(frame) ?: return stop("bad-or-unexpected-frame")
                    metadata = m
                    responseSeen = true
                    // A negative outer status is diagnostic evidence, never a success.
                    if (m.outerStatus != 0) return stop("outer-rejection")
                }
                collecting = true
            } else {
                if (!collecting) return stop("missing-delimiter")
                if (responseSeen) return stop("trailing-frame")
                if (encoded.size >= MAX_ENCODED_BYTES) return stop("frame-budget")
                encoded += b
            }
        }
        completeIfReady()
        return true
    }

    @Synchronized fun tick(callbackToken: Any, now: Long): Boolean = current(callbackToken, now)

    @Synchronized fun abort(callbackToken: Any): Boolean =
        if (callbackToken !== token) false else stop("aborted-or-disconnected")

    @Synchronized fun summary(): String =
        "5030-only: phase=$phase; reason=$reason; outerStatus=${metadata?.outerStatus}; " +
            "payloadLength=${metadata?.payloadLength}; resultUnsigned=${metadata?.firstResultUnsigned}; " +
            "connectResult=${metadata?.connectResult}; innerMeaning=unproven; no-follow-up=true."

    private fun completeIfReady() {
        if (writeConfirmed && responseSeen) {
            phase = Phase.OBSERVED
            reason = "response-observed-not-semantic-success"
        }
    }

    private fun current(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token) return false
        if (phase == Phase.STOPPED || phase == Phase.OBSERVED) return false
        if (phase == Phase.NEW) return stop("not-started")
        if (now < lastMillis) return stop("clock-regressed")
        lastMillis = now
        if (now >= sessionDeadline || (phase == Phase.WAITING && now >= responseDeadline)) {
            return stop("timeout")
        }
        return true
    }

    private fun stop(value: String): Boolean {
        if (phase != Phase.STOPPED && phase != Phase.OBSERVED) {
            reason = value
            phase = Phase.STOPPED
            encoded.clear()
        }
        return false
    }

    private fun decode(bytes: ByteArray): ByteArray? {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < bytes.size) {
            val n = bytes[i++].toInt() and 255
            if (n == 0 || i + n - 1 > bytes.size) return null
            repeat(n - 1) { out += bytes[i++] }
            if (n != 255 && i < bytes.size) out += 0
        }
        return out.toByteArray()
    }

    companion object {
        const val SESSION_MILLIS = 30_000L
        const val RESPONSE_MILLIS = 5_000L
        const val MAX_ENCODED_BYTES = 66
        const val MAX_TOTAL_BYTES = 256
    }
}

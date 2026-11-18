package de.pulselibre

/**
 * Isolated state machine for Bootstrap -> 5030 -> 5007.
 * Stops immediately after 5007 response; never proceeds to 5031 or 5002.
 */
class CirqaLegacy5030To5007Session(val token: Any = Any()) {
    enum class Phase {
        NEW, BOOTSTRAP, 
        WAITING_5030_WRITE, WAITING_5030_RESPONSE,
        WAITING_5007_WRITE, WAITING_5007_RESPONSE,
        OBSERVED, STOPPED
    }
    @Volatile var phase = Phase.NEW
        private set
    var reason = "not-started"
        private set
    var s5030OuterStatus: Int? = null
        private set
    var s5030ResultByte: Int? = null
        private set
    var s5007Status: Int? = null
        private set
    private var lastMillis = 0L
    private var deadline = 0L
    private var writeConfirmed = false
    private var collecting = false
    private val encoded = ArrayList<Byte>()
    private var totalBytes = 0

    @Synchronized fun begin(gatePersisted: Boolean, now: Long): Boolean {
        if (phase != Phase.NEW || !gatePersisted || now < 0 || now > Long.MAX_VALUE - SESSION_MILLIS) {
            return stop("not-authorized-or-invalid-start")
        }
        lastMillis = now
        deadline = now + SESSION_MILLIS
        phase = Phase.BOOTSTRAP
        reason = "bootstrap"
        return true
    }

    @Synchronized fun bootstrapReady(callbackToken: Any, confirmed: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.BOOTSTRAP || !confirmed) return false
        phase = Phase.WAITING_5030_WRITE
        reason = "ready-for-5030"
        return true
    }

    @Synchronized fun claim5030(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5030_WRITE) return stop("unexpected-5030-claim")
        writeConfirmed = false
        phase = Phase.WAITING_5030_RESPONSE
        reason = "awaiting-5030"
        return true
    }

    @Synchronized fun writeCompleted(callbackToken: Any, type: Int, success: Boolean, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (!success) return stop("write-failed-$type")
        if (type == CirqaConnect531SystemEvent.TYPE && phase == Phase.WAITING_5030_RESPONSE) {
            writeConfirmed = true
            return true
        }
        if (type == CirqaLegacy5007RequestCandidate.MESSAGE_TYPE && phase == Phase.WAITING_5007_RESPONSE) {
            writeConfirmed = true
            return true
        }
        return stop("unexpected-write-$type")
    }

    @Synchronized fun receive(callbackToken: Any, payload: ByteArray, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5030_RESPONSE && phase != Phase.WAITING_5007_RESPONSE) {
            return stop("unexpected-data")
        }
        if (payload.size > MAX_TOTAL_BYTES - totalBytes) return stop("rx-budget")
        totalBytes += payload.size
        for (b in payload) {
            if (b == 0.toByte()) {
                if (collecting && encoded.isNotEmpty()) {
                    val frame = decode(encoded.toByteArray()) ?: return stop("bad-cobs")
                    encoded.clear()
                    val h = GarminGfdiHeader.inspect(frame) ?: return stop("bad-envelope")
                    if (h.compact || h.type != 5000 || h.responseStatus == null) return stop("bad-frame")
                    if (phase == Phase.WAITING_5030_RESPONSE && h.responseTo == CirqaConnect531SystemEvent.TYPE) {
                        if (h.responseStatus != 0) return stop("5030-outer-rejection")
                        s5030OuterStatus = h.responseStatus
                        val len = frame.size - 9
                        s5030ResultByte = if (len > 0) frame[7].toInt() and 255 else null
                        if (s5030ResultByte != 0) return stop("5030-inner-result-nonzero")
                        phase = Phase.WAITING_5007_WRITE
                        reason = "5030-observed-ready-for-5007"
                        return true
                    }
                    if (phase == Phase.WAITING_5007_RESPONSE && h.responseTo == CirqaLegacy5007RequestCandidate.MESSAGE_TYPE) {
                        s5007Status = h.responseStatus
                        phase = Phase.OBSERVED
                        reason = "5030-and-5007-observed-stopping"
                        return true
                    }
                }
                collecting = true
            } else {
                if (!collecting) return stop("missing-delimiter")
                if (encoded.size >= MAX_ENCODED_BYTES) return stop("frame-budget")
                encoded += b
            }
        }
        return true
    }

    @Synchronized fun claim5007(callbackToken: Any, now: Long): Boolean {
        if (!current(callbackToken, now)) return false
        if (phase != Phase.WAITING_5007_WRITE) return stop("unexpected-5007-claim")
        writeConfirmed = false
        phase = Phase.WAITING_5007_RESPONSE
        reason = "awaiting-5007"
        return true
    }

    @Synchronized fun tick(callbackToken: Any, now: Long): Boolean = current(callbackToken, now)
    @Synchronized fun abort(callbackToken: Any): Boolean = if (callbackToken !== token) false else stop("aborted")

    @Synchronized fun summary(): String =
        "5030->5007: phase=$phase; reason=$reason; s5030OuterStatus=$s5030OuterStatus; " +
            "s5030ResultByte=$s5030ResultByte; s5007Status=$s5007Status; stop-before-5031=true."

    private fun current(callbackToken: Any, now: Long): Boolean {
        if (callbackToken !== token || phase == Phase.STOPPED || phase == Phase.OBSERVED) return false
        if (phase == Phase.NEW) return stop("not-started")
        if (now < lastMillis) return stop("clock-regressed")
        lastMillis = now
        if (now >= deadline) return stop("timeout")
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
        const val SESSION_MILLIS = 35_000L
        const val MAX_ENCODED_BYTES = 66
        const val MAX_TOTAL_BYTES = 256
    }
}

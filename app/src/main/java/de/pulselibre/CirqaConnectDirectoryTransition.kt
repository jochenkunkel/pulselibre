package de.pulselibre

/**
 * Offline-only, fail-closed model for a separately authorized future experiment.
 * It never writes preferences, rearms existing gates, sends bytes or opens a DB.
 * A caller must atomically persist its NEW one-shot gate before starting.
 *
 * Unlike Connect's completion/finally path, errors never release the next step.
 * No live caller is attached until response semantics and transport are reviewed.
 */
class CirqaConnectDirectoryTransition {
    enum class Stage {
        NEW, BOOTSTRAP, SYSTEM_EVENT_READY, SYSTEM_EVENT_PENDING,
        FILTER_READY, FILTER_PENDING, DIRECTORY_READY, DIRECTORY_CLAIMED, STOPPED
    }

    var stage = Stage.NEW
        private set
    var stopReason: String? = null
        private set
    private var startedAtMillis = 0L
    private var deadlineMillis = 0L
    private var expectedType: Int? = null
    private var writeConfirmed = false
    private var acceptedResponse = false

    fun begin(oneShotPersisted: Boolean, supportedTypesPresent: Boolean, nowMillis: Long): Boolean {
        if (stage != Stage.NEW) return stop("duplicate-start")
        if (!oneShotPersisted || !supportedTypesPresent) return stop("missing-authorization-or-types")
        if (nowMillis < 0 || nowMillis > Long.MAX_VALUE - MAX_SESSION_MILLIS) {
            return stop("invalid-clock")
        }
        startedAtMillis = nowMillis
        deadlineMillis = nowMillis + MAX_SESSION_MILLIS
        stage = Stage.BOOTSTRAP
        return true
    }

    fun bootstrapCompleted(
        deviceInformationAckSent: Boolean,
        deviceInformationAckGattConfirmed: Boolean,
        hostConfigurationQueued: Boolean,
        hostConfigurationGattConfirmed: Boolean,
        deviceAckForHostConfigurationReceived: Boolean,
        gattWritePending: Boolean,
        queuedBootstrapChunksRemain: Boolean,
        nowMillis: Long
    ): Boolean {
        if (!withinDeadline(nowMillis)) return false
        if (stage != Stage.BOOTSTRAP) return stop("unexpected-bootstrap")
        if (!CirqaLegacy5007BootstrapGate.maySend5007(
                deviceInformationAckSent, deviceInformationAckGattConfirmed,
                hostConfigurationQueued, hostConfigurationGattConfirmed,
                deviceAckForHostConfigurationReceived, gattWritePending,
                queuedBootstrapChunksRemain
            )) return false
        stage = Stage.SYSTEM_EVENT_READY
        return true
    }

    /** Claim before I/O. A failed/missing write must stop, never reclaim. */
    fun claimSystemEvent(nowMillis: Long): Boolean =
        claim(Stage.SYSTEM_EVENT_READY, Stage.SYSTEM_EVENT_PENDING,
            CirqaConnect531SystemEvent.TYPE, nowMillis)

    fun claimFilter(nowMillis: Long): Boolean =
        claim(Stage.FILTER_READY, Stage.FILTER_PENDING,
            CirqaLegacy5007RequestCandidate.MESSAGE_TYPE, nowMillis)

    fun writeCompleted(type: Int, success: Boolean, nowMillis: Long): Boolean {
        if (!withinDeadline(nowMillis)) return false
        if (!pending() || expectedType != type || writeConfirmed) return stop("unexpected-write-callback")
        if (!success) return stop("write-error")
        writeConfirmed = true
        advanceIfConfirmed()
        return true
    }

    /**
     * Takes a complete decoded frame, not an ML characteristic value.
     * Requires CRC, noncompact response-to correlation, outer SUCCESS and exactly
     * one inner result byte. Status-only ACKs deliberately do not release a step.
     * This strict shape is a test hypothesis, not proven CIRQA response semantics.
     */
    fun response(frame: ByteArray, nowMillis: Long): Boolean {
        if (!withinDeadline(nowMillis)) return false
        if (!pending() || acceptedResponse) return stop("unexpected-response")
        val header = GarminGfdiHeader.inspect(frame) ?: return stop("invalid-frame")
        if (header.compact || header.type != 5000 || header.responseTo != expectedType) {
            return stop("uncorrelated-response")
        }
        if (header.responseStatus != 0) return stop("outer-status-error")
        if (frame.size != 10) return stop("unknown-result-shape")
        if (frame[7] != 0.toByte()) return stop("inner-result-error")
        acceptedResponse = true
        advanceIfConfirmed()
        return true
    }

    /** Directory index 0 only; the existing bounded reader owns all later limits. */
    fun claimDirectory(nowMillis: Long): Boolean {
        if (!withinDeadline(nowMillis)) return false
        if (stage != Stage.DIRECTORY_READY) return stop("unexpected-directory")
        stage = Stage.DIRECTORY_CLAIMED
        return true
    }

    fun abort(): Boolean = stop("aborted")

    fun checkDeadline(nowMillis: Long): Boolean = withinDeadline(nowMillis)

    private fun claim(ready: Stage, waiting: Stage, type: Int, nowMillis: Long): Boolean {
        if (!withinDeadline(nowMillis)) return false
        if (stage != ready) return stop("unexpected-request")
        expectedType = type
        writeConfirmed = false
        acceptedResponse = false
        stage = waiting
        return true
    }

    private fun pending(): Boolean =
        stage == Stage.SYSTEM_EVENT_PENDING || stage == Stage.FILTER_PENDING

    private fun advanceIfConfirmed() {
        if (!writeConfirmed || !acceptedResponse) return
        stage = if (stage == Stage.SYSTEM_EVENT_PENDING) Stage.FILTER_READY else Stage.DIRECTORY_READY
        expectedType = null
    }

    private fun withinDeadline(nowMillis: Long): Boolean {
        if (stage == Stage.STOPPED) return false
        if (stage == Stage.NEW) return stop("not-started")
        if (nowMillis < startedAtMillis) return stop("invalid-clock")
        if (nowMillis >= deadlineMillis) return stop("timeout")
        return true
    }

    private fun stop(reason: String): Boolean {
        if (stage != Stage.STOPPED) stopReason = reason
        stage = Stage.STOPPED
        return false
    }

    companion object {
        const val MAX_SESSION_MILLIS = CirqaLegacyDownloadLimits.MAX_SESSION_MILLIS
    }
}

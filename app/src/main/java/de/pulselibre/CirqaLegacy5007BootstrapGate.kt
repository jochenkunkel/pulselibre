package de.pulselibre

/** Fail-closed gate: the single 5007 write is eligible only after the full observed bootstrap. */
object CirqaLegacy5007BootstrapGate {
    fun maySend5007(
        deviceInformationAckSent: Boolean,
        deviceInformationAckGattConfirmed: Boolean,
        hostConfigurationQueued: Boolean,
        hostConfigurationGattConfirmed: Boolean,
        deviceAckForHostConfigurationReceived: Boolean,
        gattWritePending: Boolean,
        queuedBootstrapChunksRemain: Boolean
    ): Boolean =
        deviceInformationAckSent &&
            deviceInformationAckGattConfirmed &&
            hostConfigurationQueued &&
            hostConfigurationGattConfirmed &&
            deviceAckForHostConfigurationReceived &&
            !gattWritePending &&
            !queuedBootstrapChunksRemain
}

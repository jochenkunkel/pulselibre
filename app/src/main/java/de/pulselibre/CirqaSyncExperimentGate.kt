package de.pulselibre

/**
 * Pure, offline decision policy for a future opt-in 5050 SYNC-bit diagnostic.
 * No Bluetooth calls and no capability negotiation are performed here.
 */
object CirqaSyncExperimentGate {
    enum class Decision { CONTINUE_OBSERVING, STOP, SYNC_SIGNAL_OBSERVED }

    data class Observation(
        val userConfirmed: Boolean,
        val recorderStopped: Boolean,
        val gattWriteConfirmed: Boolean,
        val configurationAckConfirmed: Boolean,
        val negativeProtocolResponse: Boolean,
        val unexpectedFrame: Boolean,
        val connectionLost: Boolean,
        val repeatedConfigurationRequest: Boolean,
        val elapsedMillis: Long,
        val crcValid5037Observed: Boolean
    )

    const val MAX_OBSERVATION_MILLIS = 10_000L

    fun evaluate(o: Observation): Decision {
        if (!o.userConfirmed || !o.recorderStopped || !o.gattWriteConfirmed ||
            !o.configurationAckConfirmed || o.negativeProtocolResponse ||
            o.unexpectedFrame || o.connectionLost || o.repeatedConfigurationRequest ||
            o.elapsedMillis < 0 || o.elapsedMillis > MAX_OBSERVATION_MILLIS) {
            return Decision.STOP
        }
        return if (o.crcValid5037Observed) Decision.SYNC_SIGNAL_OBSERVED
        else Decision.CONTINUE_OBSERVING
    }
}

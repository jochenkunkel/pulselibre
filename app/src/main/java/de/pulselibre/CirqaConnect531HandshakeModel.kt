package de.pulselibre

/**
 * Offline model based on Garmin Connect 5.31 handshake analysis.
 *
 * The device's incoming 5050 ACK and the host's outgoing 5050 response
 * completion are separate events. This model never touches BLE.
 */
class CirqaConnect531HandshakeModel {
    enum class State { WAIT_5024, WAIT_5050, HOST_RESPONSE_PENDING, COMPLETE, FAILED }
    enum class Decision { IGNORE, ACK_5024, ACK_5050_AND_PREPARE_HOST_RESPONSE, LENGTH_ERROR, NONE }
    var state = State.WAIT_5024
        private set

    fun deviceInformation(valid: Boolean): Decision {
        if (state == State.FAILED || state == State.COMPLETE) return Decision.IGNORE
        if (!valid) return Decision.LENGTH_ERROR
        state = State.WAIT_5050
        return Decision.ACK_5024
    }

    fun deviceConfiguration(body: ByteArray): Decision {
        if (state != State.WAIT_5050) return Decision.IGNORE
        if (Cirqa5050BodyParser.parse(body) == null) return Decision.LENGTH_ERROR
        state = State.HOST_RESPONSE_PENDING
        return Decision.ACK_5050_AND_PREPARE_HOST_RESPONSE
    }

    /** GATT completion is not GFDI protocol ACK or application completion. */
    fun gattWriteConfirmed() = Unit

    /** The peer acknowledging our 5050 is not sufficient by itself. */
    fun gfdi5050Ack() = Unit

    /** Only explicit application-level response completion can finish handshake. */
    fun hostResponseCompleted(success: Boolean) {
        if (state != State.HOST_RESPONSE_PENDING) return
        state = if (success) State.COMPLETE else State.FAILED
    }
}

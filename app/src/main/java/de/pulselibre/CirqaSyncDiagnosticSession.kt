package de.pulselibre

/**
 * Offline-only state machine for a future, explicitly approved CIRQA SYNC diagnostic.
 * This component never sends BLE commands or changes the current 5050 test.
 */
class CirqaSyncDiagnosticSession {
    enum class State { NOT_AUTHORIZED, WAITING_FOR_5050_ACK, OBSERVING, SYNC_SIGNAL, STOPPED }
    enum class Event {
        AUTHORIZE_WITH_RECORDER_STOPPED, WRITE_CONFIRMED, CONFIGURATION_ACK,
        CRC_VALID_5037, NEGATIVE_RESPONSE, REPEATED_5050,
        UNEXPECTED_FRAME, CONNECTION_LOST, TIMEOUT
    }

    var state: State = State.NOT_AUTHORIZED
        private set
    private var writeConfirmed = false
    private var ackConfirmed = false

    fun on(event: Event): State {
        if (state == State.STOPPED || state == State.SYNC_SIGNAL) return state
        when (event) {
            Event.AUTHORIZE_WITH_RECORDER_STOPPED -> {
                if (state == State.NOT_AUTHORIZED) state = State.WAITING_FOR_5050_ACK
                else state = State.STOPPED
            }
            Event.WRITE_CONFIRMED -> {
                if (state != State.WAITING_FOR_5050_ACK || writeConfirmed) state = State.STOPPED
                else writeConfirmed = true
            }
            Event.CONFIGURATION_ACK -> {
                if (state != State.WAITING_FOR_5050_ACK || !writeConfirmed || ackConfirmed) {
                    state = State.STOPPED
                } else {
                    ackConfirmed = true
                    state = State.OBSERVING
                }
            }
            Event.CRC_VALID_5037 -> {
                state = if (state == State.OBSERVING) State.SYNC_SIGNAL else State.STOPPED
            }
            Event.NEGATIVE_RESPONSE, Event.REPEATED_5050, Event.UNEXPECTED_FRAME,
            Event.CONNECTION_LOST, Event.TIMEOUT -> state = State.STOPPED
        }
        return state
    }
}

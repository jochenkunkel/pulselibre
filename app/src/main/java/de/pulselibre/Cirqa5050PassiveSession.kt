package de.pulselibre

/**
 * Passive state tracker for the existing 15-byte zero-capability 5050 test.
 * It has no authorization event and cannot enable the experimental SYNC bit.
 * Only a complete write followed by a fresh ACK can open the 5037 window.
 */
class Cirqa5050PassiveSession {
    enum class State { IDLE, QUEUED, WRITE_CONFIRMED, OBSERVING, SIGNAL, STOPPED }
    var state = State.IDLE
        private set
    var signalObserved = false
        private set

    fun queued() {
        if (state == State.IDLE) state = State.QUEUED else stop()
    }

    fun writeConfirmed() {
        if (state == State.QUEUED) state = State.WRITE_CONFIRMED else stop()
    }

    fun freshAck() {
        if (state == State.WRITE_CONFIRMED) state = State.OBSERVING else stop()
    }

    fun valid5037() {
        if (state == State.OBSERVING) {
            signalObserved = true
            state = State.SIGNAL
        }
    }

    fun stop() { state = State.STOPPED }
}

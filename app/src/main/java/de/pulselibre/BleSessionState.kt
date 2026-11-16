package de.pulselibre

/** Pure state transitions: no Android or Garmin hardware required. */
enum class BleSessionState { IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, ERROR }
enum class BleEvent { START_SCAN, STOP_SCAN, CONNECT, CONNECTED, DISCONNECTED, FAILED, RESET }

fun transition(current: BleSessionState, event: BleEvent): BleSessionState = when (event) {
    BleEvent.START_SCAN -> if (current == BleSessionState.IDLE || current == BleSessionState.DISCONNECTED || current == BleSessionState.ERROR) BleSessionState.SCANNING else current
    BleEvent.STOP_SCAN -> if (current == BleSessionState.SCANNING) BleSessionState.IDLE else current
    BleEvent.CONNECT -> if (current == BleSessionState.SCANNING || current == BleSessionState.IDLE || current == BleSessionState.DISCONNECTED) BleSessionState.CONNECTING else current
    BleEvent.CONNECTED -> if (current == BleSessionState.CONNECTING) BleSessionState.CONNECTED else current
    BleEvent.DISCONNECTED -> BleSessionState.DISCONNECTED
    BleEvent.FAILED -> BleSessionState.ERROR
    BleEvent.RESET -> BleSessionState.IDLE
}

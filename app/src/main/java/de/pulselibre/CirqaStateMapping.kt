package de.pulselibre

/** Maps Android's GATT profile state without inferring the owning application. */
internal object CirqaStateMapping {
    fun fromAndroidState(state: Int): CirqaConnectionHistory.State = when (state) {
        0 -> CirqaConnectionHistory.State.DISCONNECTED
        1 -> CirqaConnectionHistory.State.CONNECTING
        2 -> CirqaConnectionHistory.State.CONNECTED
        3 -> CirqaConnectionHistory.State.DISCONNECTING
        else -> CirqaConnectionHistory.State.UNKNOWN
    }

    fun label(strings: Strings, state: CirqaConnectionHistory.State): String = strings.get(when (state) {
        CirqaConnectionHistory.State.CONNECTED -> R.string.gatt_connected
        CirqaConnectionHistory.State.CONNECTING -> R.string.gatt_connecting
        CirqaConnectionHistory.State.DISCONNECTING -> R.string.gatt_disconnecting
        CirqaConnectionHistory.State.DISCONNECTED -> R.string.gatt_disconnected
        CirqaConnectionHistory.State.PERMISSION_DENIED -> R.string.gatt_permission
        CirqaConnectionHistory.State.NOT_PAIRED -> R.string.gatt_not_paired
        CirqaConnectionHistory.State.UNKNOWN -> R.string.gatt_unknown
    })
}

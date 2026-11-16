package de.pulselibre

/** Maps Android's GATT profile state without inferring the owning application. */
object CirqaStateMapping {
    fun fromAndroidState(state: Int): CirqaConnectionHistory.State = when (state) {
        0 -> CirqaConnectionHistory.State.DISCONNECTED
        1 -> CirqaConnectionHistory.State.CONNECTING
        2 -> CirqaConnectionHistory.State.CONNECTED
        3 -> CirqaConnectionHistory.State.DISCONNECTING
        else -> CirqaConnectionHistory.State.UNKNOWN
    }

    fun label(state: CirqaConnectionHistory.State): String = when (state) {
        CirqaConnectionHistory.State.CONNECTED -> "Android GATT: verbunden (App unbekannt)"
        CirqaConnectionHistory.State.CONNECTING -> "Android GATT: verbindet"
        CirqaConnectionHistory.State.DISCONNECTING -> "Android GATT: trennt"
        CirqaConnectionHistory.State.DISCONNECTED -> "Android GATT: nicht verbunden"
        CirqaConnectionHistory.State.PERMISSION_DENIED -> "Android GATT: Berechtigung fehlt"
        CirqaConnectionHistory.State.NOT_PAIRED -> "Kein gekoppeltes CIRQA"
        CirqaConnectionHistory.State.UNKNOWN -> "Android GATT: unbekannt"
    }
}

package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State

/** The header shows green only when Android reports a connected GATT link. */
object CirqaConnectionIndicator {
    fun isConnected(state: State): Boolean = state == State.CONNECTED

    fun contentDescription(state: State): String {
        val outline = if (isConnected(state)) "grüner Rand" else "schwarzer Rand"
        return "CIRQA-Verbindungsstatus: $outline. ${CirqaStateMapping.label(state)}. Einstellungen öffnen."
    }
}

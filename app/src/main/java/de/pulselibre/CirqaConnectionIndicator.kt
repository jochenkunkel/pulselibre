package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State

/** The header shows green only when Android reports a connected GATT link. */
internal object CirqaConnectionIndicator {
    fun isConnected(state: State): Boolean = state == State.CONNECTED

    fun contentDescription(strings: Strings, state: State): String {
        val outline = strings.get(if (isConnected(state)) R.string.link_outline_green else R.string.link_outline_black)
        return strings.get(R.string.link_status_cd, outline, CirqaStateMapping.label(strings, state))
    }
}

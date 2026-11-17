package de.pulselibre

/**
 * The header may show green only when Android freshly reports a connected GATT link
 * for a bonded device whose Android name identifies it as CIRQA.
 */
object CirqaConnectionIndicator {
    fun isConnected(
        bluetoothEnabled: Boolean,
        connectPermissionGranted: Boolean,
        bondedCirqaFound: Boolean,
        gattConnected: Boolean
    ): Boolean = bluetoothEnabled && connectPermissionGranted && bondedCirqaFound && gattConnected

    fun contentDescription(connected: Boolean, permissionGranted: Boolean): String = when {
        connected -> "CIRQA-Verbindungsstatus: grüner Rand. Android meldet GATT verbunden; verbundene App unbekannt. Einstellungen öffnen."
        !permissionGranted -> "CIRQA-Verbindungsstatus: schwarzer Rand; Verbindung mangels Berechtigung nicht bestätigt. Einstellungen öffnen."
        else -> "CIRQA-Verbindungsstatus: schwarzer Rand; keine aktive Android-GATT-Verbindung bestätigt. Einstellungen öffnen."
    }
}

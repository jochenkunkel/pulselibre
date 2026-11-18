package de.pulselibre

/**
 * Text of the shareable BLE diagnostic (#5). The inputs are counts, flags and aggregated summaries only:
 * there is deliberately no parameter for device names, MAC addresses, bond rows or raw advertisement
 * payloads, so none of them can reach the shared text.
 */
internal object DiagnosticReport {
    fun build(
        advertisingDevices: Int,
        scanStatus: String,
        gattLabel: String,
        transitions: List<CirqaConnectionHistory.Entry>,
        bonded: Boolean,
        addressMatch: Boolean,
        evidenceSummary: String,
        rssiSummary: String,
        bondListQueried: Boolean
    ): String = buildString {
        appendLine("PulsLibre · BLE-Scan-Diagnose (aktive Probes separat)")
        appendLine("Gefundene Werbegeräte: $advertisingDevices")
        appendLine("Scan-Status: $scanStatus")
        appendLine("CIRQA GATT (Android-Systemstatus; verantwortliche App unbekannt): $gattLabel")
        appendLine("CIRQA-Statuswechsel (in der App beobachtet): ${transitions.size}")
        transitions.forEach { appendLine("  ${it.timestampMillis} · ${it.state}") }
        appendLine("CIRQA-Kopplung: " + if (bonded) "vorhanden" else "nicht bestätigt")
        appendLine("CIRQA-Adressabgleich: " + if (addressMatch) "Treffer (lokal geprüft; Adresse nicht exportiert)" else "Kein bestätigter Treffer; wechselnde BLE-Adressen möglich")
        appendLine(evidenceSummary)
        appendLine()
        appendLine(rssiSummary)
        // Never share raw BLE names, MAC addresses, UUID payloads or bond rows.
        appendLine("BLE-Geräteeinträge (nur Anzahl): $advertisingDevices")
        appendLine("Android-Kopplungsliste: " + if (bondListQueried) "lokal abgefragt (Einträge nicht exportiert)" else "nicht abgefragt oder leer")
        appendLine("\nDieser passive Scan baut keine Verbindung auf. Separate, manuell gestartete CIRQA-Probes können aktive Verbindungen, Benachrichtigungsregistrierung und Schreiboperationen verwenden.")
    }
}

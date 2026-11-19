package de.pulselibre

/**
 * Read-only diagnostics snapshot of the app (#172). Counters, flags and versions only: there is deliberately no field
 * for MAC addresses, serial numbers, device names, tokens or measured health values.
 */
internal data class DiagnosticsSnapshot(
    val appVersion: String,
    val androidRelease: String,
    val sdkInt: Int,
    val deviceModel: String,
    val dbIntegrity: String,
    val dbBytes: Long,
    val tableCounts: Map<String, Long?>,
    val verifiedFitFiles: Int,
    val verifiedFitBytes: Long,
    val recorderFlagFresh: Boolean,
    val serviceAlive: Boolean,
    val lastReadingAgeSeconds: Long?,
    val heartbeatAgeSeconds: Long?,
    val bluetoothEnabled: Boolean?,
    val permissionConnect: Boolean,
    val permissionScan: Boolean,
    val permissionNotifications: Boolean
)

internal object DiagnosticsFormat {
    private fun yn(v: Boolean?) = when (v) { true -> "ja"; false -> "nein"; null -> "unbekannt" }
    private fun bytes(b: Long) = when {
        b >= 1_048_576 -> "%.1f MB".format(java.util.Locale.GERMANY, b / 1_048_576.0)
        b >= 1024 -> "%.1f kB".format(java.util.Locale.GERMANY, b / 1024.0)
        else -> "$b B"
    }
    private fun age(sec: Long?) = if (sec == null) "kein Wert" else when {
        sec < 60 -> "vor $sec s"
        sec < 3600 -> "vor ${sec / 60} min"
        else -> "vor ${sec / 3600} h ${(sec % 3600) / 60} min"
    }

    fun app(s: DiagnosticsSnapshot): String = buildString {
        appendLine("App-Version: ${s.appVersion}")
        appendLine("Android: ${s.androidRelease} (API ${s.sdkInt}) · ${s.deviceModel}")
        appendLine("Datenbank-Integrität (quick_check): ${s.dbIntegrity}")
        append("Datenbankgröße: ${bytes(s.dbBytes)}")
    }

    fun bluetooth(s: DiagnosticsSnapshot): String = buildString {
        appendLine("Bluetooth eingeschaltet: ${yn(s.bluetoothEnabled)}")
        appendLine("Berechtigung Geräte in der Nähe (Verbinden): ${yn(s.permissionConnect)}")
        appendLine("Berechtigung Geräte in der Nähe (Suchen): ${yn(s.permissionScan)}")
        appendLine("Benachrichtigungen erlaubt: ${yn(s.permissionNotifications)}")
        appendLine("Aufzeichnungsdienst läuft: ${yn(s.recorderFlagFresh && s.serviceAlive)} (Flag/Herzschlag: ${yn(s.recorderFlagFresh)}, Dienst aktiv: ${yn(s.serviceAlive)})")
        appendLine("Letzter Herzschlag des Dienstes: ${age(s.heartbeatAgeSeconds)}")
        append("Letzte empfangene Messung: ${age(s.lastReadingAgeSeconds)}")
    }

    fun sync(s: DiagnosticsSnapshot): String = buildString {
        appendLine("Verifizierte FIT-Dateien lokal: ${s.verifiedFitFiles} (${bytes(s.verifiedFitBytes)})")
        s.tableCounts.forEach { (table, count) -> appendLine("Tabelle $table: ${count?.toString() ?: "nicht verfügbar"} Zeilen") }
        append("Nur lesende Prüfung; ein Sync wird hier nicht gestartet.")
    }

    const val TRACE_PLACEHOLDER = "Die zeitlich begrenzte, ausdrücklich gestartete Kommunikationsaufzeichnung ist noch nicht verfügbar (folgt, Ticket #172). Hier wird nichts aufgezeichnet."

    /** Text for the explicit, user-initiated export. Preview is shown before sharing; nothing is uploaded automatically. */
    fun exportPackage(s: DiagnosticsSnapshot, createdAt: String): String = buildString {
        appendLine("PulsLibre Diagnosepaket · $createdAt")
        appendLine("Enthält nur Zähler, Versionen und Statusflags. Keine MAC-Adressen, Seriennummern, Gerätenamen, Tokens oder Messwerte.")
        appendLine()
        appendLine("[App]"); appendLine(app(s)); appendLine()
        appendLine("[Bluetooth]"); appendLine(bluetooth(s)); appendLine()
        appendLine("[Synchronisation]"); appendLine(sync(s))
    }
}

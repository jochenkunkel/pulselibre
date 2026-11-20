package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsFormatTest {
    private val snapshot = DiagnosticsSnapshot(
        appVersion = "2.0.0_build1", androidRelease = "15", sdkInt = 35, deviceModel = "samsung SM-X",
        dbIntegrity = "ok", dbBytes = 62_000_000, tableCounts = linkedMapOf("events" to 125_000L, "fit_unmapped_fields" to null),
        verifiedFitFiles = 31, verifiedFitBytes = 4_200_000, recorderFlagFresh = true, serviceAlive = false,
        lastReadingAgeSeconds = 95, heartbeatAgeSeconds = 130, bluetoothEnabled = true,
        permissionConnect = true, permissionScan = false, permissionNotifications = true,
        fitInventory = "Importierbare Schlüssel in Datenbank: 1"
    )

    @Test fun sectionsContainTheKeyFacts() {
        assertTrue(DiagnosticsFormat.app(snapshot).contains("quick_check): ok"))
        assertTrue(DiagnosticsFormat.app(snapshot).contains("59,1 MB"))
        val bt = DiagnosticsFormat.bluetooth(snapshot)
        assertTrue(bt.contains("Berechtigung Geräte in der Nähe (Suchen): nein"))
        assertTrue(bt.contains("Aufzeichnungsdienst läuft: nein"))     // fresh flag but service gone (#212 case)
        assertTrue(bt.contains("vor 1 min"))
        val sync = DiagnosticsFormat.sync(snapshot)
        assertTrue(sync.contains("Verifizierte FIT-Dateien lokal: 31"))
        assertTrue(sync.contains("Tabelle fit_unmapped_fields: nicht verfügbar Zeilen"))
        assertTrue(sync.contains("Tabelle events: 125000 Zeilen"))
        assertTrue(sync.contains("Importierbare Schlüssel in Datenbank: 1"))
    }

    @Test fun exportPackageIsRedactedByConstruction() {
        val report = DiagnosticsFormat.exportPackage(snapshot, "19.11.2026 22:00")
        assertTrue(report.contains("[App]") && report.contains("[Bluetooth]") && report.contains("[Synchronisation]"))
        assertTrue(report.contains("Keine MAC-Adressen"))
        assertFalse("MAC-ähnliche Muster dürfen nicht vorkommen", Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}").containsMatchIn(report))
        assertFalse(report.lowercase().contains("seriennummer:"))
    }

    @Test fun missingValuesAreShownHonestly() {
        val unknown = snapshot.copy(bluetoothEnabled = null, lastReadingAgeSeconds = null, heartbeatAgeSeconds = null)
        val bt = DiagnosticsFormat.bluetooth(unknown)
        assertTrue(bt.contains("Bluetooth eingeschaltet: unbekannt"))
        assertTrue(bt.contains("Letzte empfangene Messung: kein Wert"))
    }

    @Test fun traceAreaIsAnHonestPlaceholder() {
        assertTrue(DiagnosticsFormat.TRACE_PLACEHOLDER.contains("noch nicht verfügbar"))
        assertTrue(DiagnosticsFormat.TRACE_PLACEHOLDER.contains("nichts aufgezeichnet"))
    }
}

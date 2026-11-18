package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class DiagnosticReportPrivacyTest {
    private val mac = "AA:BB:CC:DD:EE:FF"
    private val mac2 = "11:22:33:44:55:66"
    private val name = "CIRQA Smart Band Privat"

    @Test fun reportNeverContainsAddressesNamesOrPayloadsEvenAfterRealObservations() {
        val evidence = CirqaScanEvidence()
        evidence.observe(mac, addressMatchesBond = true, garminHint = false)
        evidence.observe(mac2, addressMatchesBond = false, garminHint = true)
        evidence.observeFeatures(mac, hasName = true, hasManufacturerData = true, hasServiceUuids = true)
        val rssi = PassiveRssiTracker()
        rssi.observe(mac, -60, listOf("3e1d50cd-7e3e-427d-8e1c-b78aa87fe624"))
        val history = CirqaConnectionHistory().also { it.observe(1_000L, CirqaConnectionHistory.State.CONNECTED) }
        val text = DiagnosticReport.build(2, "Scan beendet", "verbunden", history.snapshot(),
            bonded = true, addressMatch = true, evidenceSummary = evidence.summary(),
            rssiSummary = rssi.summary(), bondListQueried = true)
        listOf(mac, mac2, mac.lowercase(), name, "AA:BB", "manufacturer", "0x").forEach {
            assertFalse("report must not contain '$it'", text.contains(it, ignoreCase = true))
        }
        assertTrue(text.contains("Adresse nicht exportiert"))
        assertTrue(text.contains("nur Anzahl"))
    }

    @Test fun countsAndFlagsAreReportedWithoutIdentifiers() {
        val text = DiagnosticReport.build(0, "Bitte Bluetooth aktivieren", "nicht verbunden", emptyList(),
            bonded = false, addressMatch = false, evidenceSummary = "", rssiSummary = "", bondListQueried = false)
        assertTrue(text.contains("Gefundene Werbegeräte: 0"))
        assertTrue(text.contains("nicht bestätigt"))
        assertTrue(text.contains("nicht abgefragt"))
    }
}

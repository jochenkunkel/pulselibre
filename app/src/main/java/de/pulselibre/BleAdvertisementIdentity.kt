package de.pulselibre

import java.util.Locale

/** Advertisement hints only: neither a name nor a company ID proves device ownership. */
object BleAdvertisementIdentity {
    const val GARMIN_COMPANY_ID = 0x0087
    const val GARMIN_SERVICE_16 = "0000fe1f"

    fun isGarminHint(name: String?, companyIds: Collection<Int>, serviceUuids: Collection<String>): Boolean =
        name?.contains("garmin", ignoreCase = true) == true ||
            companyIds.contains(GARMIN_COMPANY_ID) ||
            serviceUuids.any { it.lowercase(Locale.ROOT).startsWith(GARMIN_SERVICE_16) }

    fun describe(name: String?, companyIds: Collection<Int>, serviceUuids: Collection<String>, rssi: Int): String {
        val label = name?.takeIf { it.isNotBlank() } ?: "Unbekanntes Gerät"
        val ids = if (companyIds.isEmpty()) "keine" else companyIds.sorted().joinToString { "0x%04X".format(Locale.ROOT, it) }
        val services = if (serviceUuids.isEmpty()) "keine" else serviceUuids.joinToString(", ")
        val hint = if (isGarminHint(name, companyIds, serviceUuids)) "Garmin-Hinweis (nicht bestätigt)" else "Hersteller nicht erkannt"
        return "$label\n$hint · RSSI $rssi dBm\nHersteller-ID: $ids\nService-UUIDs: $services"
    }
}

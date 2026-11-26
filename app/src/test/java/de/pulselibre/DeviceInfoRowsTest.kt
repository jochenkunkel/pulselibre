package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoRowsTest {
    private val s = GermanStrings
    private fun v(key: String, text: String, at: Long = 1000L, source: String = DeviceMetadataSources.GATT_DEVICE_INFO) =
        LiveDeviceMetadata(key, text, null, source, at)

    @Test fun firmwareRowShowsSoftwareValue() {
        val values = listOf(v(DeviceMetadataFields.SOFTWARE_VERSION, "12.34"))
        assertEquals("12.34", DeviceInfoRows.firmware(values, s)?.text)
        assertTrue(DeviceInfoRows.coveredByFixedRows(values, s)(values[0]))
    }

    @Test fun firmwareValueIsFallbackAndDifferingFirmwareStaysListed() {
        val only = listOf(v(DeviceMetadataFields.FIRMWARE_VERSION, "9.9"))
        assertEquals("9.9", DeviceInfoRows.firmware(only, s)?.text)
        assertTrue(DeviceInfoRows.coveredByFixedRows(only, s)(only[0]))
        val both = listOf(v(DeviceMetadataFields.SOFTWARE_VERSION, "12.34"), v(DeviceMetadataFields.FIRMWARE_VERSION, "9.9"))
        assertFalse(DeviceInfoRows.coveredByFixedRows(both, s)(both[1]))
    }

    @Test fun identityJoinsDeviceNameAndGarminValueAndHidesBothBelow() {
        val values = listOf(v(DeviceMetadataFields.DEVICE_NAME, "CIRQA"), v(GattStandardFields.GARMIN_VENDOR, "ABCD", 2000L))
        val shown = DeviceInfoRows.identity(values, s)!!
        assertEquals("CIRQA / ABCD", shown.text)
        assertEquals(2000L, shown.observedAtMs)
        val covered = DeviceInfoRows.coveredByFixedRows(values, s)
        assertTrue(values.all(covered))
    }

    @Test fun fitSoftwareVersionFillsTheFirmwareRowWhenNoNewerLiveValueExists() {
        // Owner report 26.11.2026: FIT files carry software 4.2, live metadata has none -> "Firmware: nicht verfügbar".
        val fit = v(DeviceMetadataFields.SOFTWARE_VERSION, "4.2", at = 5000L, source = DeviceMetadataSources.FIT_DEVICE_INFO)
        val merged = DeviceInfoRows.withFitSoftwareVersion(listOf(v(DeviceMetadataFields.DEVICE_NAME, "CIRQA")), fit)
        assertEquals("4.2", DeviceInfoRows.firmware(merged, s)?.text)
        assertEquals(DeviceMetadataSources.FIT_DEVICE_INFO, DeviceInfoRows.firmware(merged, s)?.source)
        // An older live value is replaced, a newer one wins; without a FIT value nothing changes.
        val olderLive = listOf(v(DeviceMetadataFields.SOFTWARE_VERSION, "4.1", at = 1000L))
        assertEquals("4.2", DeviceInfoRows.firmware(DeviceInfoRows.withFitSoftwareVersion(olderLive, fit), s)?.text)
        val newerLive = listOf(v(DeviceMetadataFields.SOFTWARE_VERSION, "4.3", at = 9000L))
        assertEquals("4.3", DeviceInfoRows.firmware(DeviceInfoRows.withFitSoftwareVersion(newerLive, fit), s)?.text)
        assertEquals(olderLive, DeviceInfoRows.withFitSoftwareVersion(olderLive, null))
    }

    @Test fun olderValueShowsItsDateInsteadOfTimeUnknown() {
        val utc = java.time.ZoneOffset.UTC
        val at = java.time.Instant.parse("2026-11-25T22:14:00Z").toEpochMilli()
        assertEquals("Stand 25.11.2026 22:14", DeviceInfoRows.freshness(s, at, at + 24 * 3_600_000L, utc))
        assertEquals("vor 5 min", DeviceInfoRows.freshness(s, at, at + 5 * 60_000L, utc))
        assertEquals("Zeitpunkt unbekannt · letzter bekannter Stand", DeviceInfoRows.freshness(s, 0L, at, utc))
        assertEquals("Zeitpunkt unbekannt · letzter bekannter Stand", DeviceInfoRows.freshness(s, at + 60_000L, at, utc)) // future
    }

    @Test fun noValueMeansNoRowValue() {
        assertNull(DeviceInfoRows.firmware(emptyList(), s))
        assertNull(DeviceInfoRows.identity(emptyList(), s))
        assertNull(DeviceInfoRows.bandClock(emptyList(), s))
    }

    @Test fun gdiTextGetsReadableLabelAndSourceName() {
        val e = v("gdi_text_3", "ABC 1.2", source = DeviceMetadataSources.GARMIN_GDI_OTHER)
        assertEquals("Zusatztext 3 vom Band", DeviceMetadataFormat.label(e, s))
        assertEquals("Garmin-Protokoll (Zusatztexte)", DeviceBatteryFormat.sourceLabel(s, e.source))
        assertEquals("ABC 1.2", DeviceMetadataFormat.displayValue(e, s))
    }
}

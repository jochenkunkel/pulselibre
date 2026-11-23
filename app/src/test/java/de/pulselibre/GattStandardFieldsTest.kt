package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Readable band information for standard Bluetooth SIG characteristics; unmatched lengths fall back to the raw hex. */
class GattStandardFieldsTest {
    private val s = GermanStrings
    private fun meta(key: String, hex: String) = LiveDeviceMetadata(key, hex, null, DeviceMetadataSources.GATT_OTHER, 0L)

    @Test fun labelsAreReadableNotUuids() {
        assertEquals("Erscheinungsbild", DeviceMetadataFormat.label(meta("gatt_2a01", "c100"), s))
        assertEquals("Bevorzugtes Verbindungsintervall", DeviceMetadataFormat.label(meta("gatt_2a04", ""), s))
        assertEquals("Adressauflösung (Privatsphäre)", DeviceMetadataFormat.label(meta("gatt_2aa6", ""), s))
        assertEquals("Lauf-Sensor-Funktionen", DeviceMetadataFormat.label(meta("gatt_2a54", ""), s))
        assertEquals("Garmin-spezifischer Wert", DeviceMetadataFormat.label(meta("gatt_6a4e", ""), s))
    }

    @Test fun observedBandValuesAreDecodedPerSpecification() {
        assertEquals("Sportuhr", DeviceMetadataFormat.displayValue(meta("gatt_2a01", "c100"), s))
        assertEquals("30–45 ms, Latenz 0, Timeout 4000 ms",
            DeviceMetadataFormat.displayValue(meta("gatt_2a04", "1800240000009001"), s))
        assertEquals("Unterstützt", DeviceMetadataFormat.displayValue(meta("gatt_2aa6", "01"), s))
        assertEquals("Keine optionalen Funktionen", DeviceMetadataFormat.displayValue(meta("gatt_2a54", "0000"), s))
    }

    @Test fun unknownVendorValueAndMalformedInputStayRaw() {
        assertEquals("00d2b57923", DeviceMetadataFormat.displayValue(meta("gatt_6a4e", "00d2b57923"), s))
        assertEquals("c1", DeviceMetadataFormat.displayValue(meta("gatt_2a01", "c1"), s))
        assertEquals("zz", DeviceMetadataFormat.displayValue(meta("gatt_2a01", "zz"), s))
        assertEquals("18002400", DeviceMetadataFormat.displayValue(meta("gatt_2a04", "18002400"), s))
        assertTrue(DeviceMetadataFormat.displayValue(meta("gatt_2a01", "ffff"), s).startsWith("Andere"))
    }
}

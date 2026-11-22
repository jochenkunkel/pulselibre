package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #142/#159: newest-value mapping from the provenance store into the battery row. */
class DeviceMetadataFormatTest {
    private val now = 1_800_000_000_000L
    private fun meta(key: String, value: String, source: String, at: Long = now) =
        LiveDeviceMetadata(key, value, null, source, at)

    @Test fun buildsBatteryReadingFromNewestValues() {
        val values = listOf(
            meta(DeviceMetadataFields.BATTERY_LEVEL, "37", DeviceMetadataSources.GARMIN_GDI_STATUS),
            meta(DeviceMetadataFields.BATTERY_STATUS, "NORMAL", DeviceMetadataSources.GARMIN_GDI_STATUS)
        )
        val reading = DeviceMetadataFormat.batteryReading(values)!!
        assertEquals(37, reading.percent)
        assertEquals(DeviceMetadataSources.GARMIN_GDI_STATUS, reading.source)
        assertEquals("NORMAL", reading.statusLabel)
        assertNull(reading.statusCode)
    }

    @Test fun fitNumericStatusIsMappedToStatusCode() {
        val values = listOf(
            meta(DeviceMetadataFields.BATTERY_LEVEL, "70", DeviceMetadataSources.GATT_BATTERY),
            meta(DeviceMetadataFields.BATTERY_STATUS, "6", DeviceMetadataSources.FIT_DEVICE_INFO)
        )
        val reading = DeviceMetadataFormat.batteryReading(values)!!
        assertEquals(6, reading.statusCode)
        assertTrue(DeviceMetadataFormat.chargingFromFitCode(reading.statusCode))
    }

    @Test fun chargingLabelIsRecognised() {
        assertTrue(DeviceMetadataFormat.isCharging("CHARGING"))
        assertTrue(DeviceMetadataFormat.isCharging("charging"))
        assertFalse(DeviceMetadataFormat.isCharging("NORMAL"))
        assertFalse(DeviceMetadataFormat.isCharging(null))
    }

    @Test fun invalidOrMissingPercentageYieldsNull() {
        assertNull(DeviceMetadataFormat.batteryReading(emptyList()))
        assertNull(DeviceMetadataFormat.batteryReading(
            listOf(meta(DeviceMetadataFields.BATTERY_LEVEL, "n/a", DeviceMetadataSources.GATT_BATTERY))))
        assertNull(DeviceMetadataFormat.batteryReading(
            listOf(meta(DeviceMetadataFields.BATTERY_LEVEL, "150", DeviceMetadataSources.GATT_BATTERY))))
    }

    @Test fun newestPercentageWins() {
        val older = meta(DeviceMetadataFields.BATTERY_LEVEL, "20", DeviceMetadataSources.GATT_BATTERY, now - 60_000)
        val newer = meta(DeviceMetadataFields.BATTERY_LEVEL, "80", DeviceMetadataSources.GATT_BATTERY, now)
        assertEquals(80, DeviceMetadataFormat.batteryReading(listOf(older, newer))!!.percent)
    }
}

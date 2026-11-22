package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #142/#159: decoding of read-only GATT metadata values with provenance. */
class DeviceMetadataGattDecoderTest {
    private val now = 1_800_000_000_000L
    private fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    @Test fun batteryLevelDecodesToPercent() {
        val entry = DeviceMetadataGattDecoder.decode("00002a19-0000-1000-8000-00805f9b34fb", bytes(0x25), now)!!
        assertEquals(DeviceMetadataFields.BATTERY_LEVEL, entry.fieldKey)
        assertEquals("37", entry.valueText)
        assertEquals("%", entry.unit)
        assertEquals(DeviceMetadataSources.GATT_BATTERY, entry.source)
    }

    @Test fun firmwareAndSerialGetStableKeys() {
        val fw = DeviceMetadataGattDecoder.decode("00002a26-0000-1000-8000-00805f9b34fb", "1.2.3".toByteArray(), now)!!
        assertEquals(DeviceMetadataFields.FIRMWARE_VERSION, fw.fieldKey)
        assertEquals("1.2.3", fw.valueText)
        val serial = DeviceMetadataGattDecoder.decode("00002a25-0000-1000-8000-00805f9b34fb", "ABC123".toByteArray(), now)!!
        assertEquals(DeviceMetadataFields.SERIAL_NUMBER, serial.fieldKey)
    }

    @Test fun unknownCharacteristicIsStillStoredAsHex() {
        val entry = DeviceMetadataGattDecoder.decode("00002b00-0000-1000-8000-00805f9b34fb", bytes(0x01, 0xff), now)!!
        assertEquals("gatt_2b00", entry.fieldKey)
        assertEquals("01ff", entry.valueText)
        assertEquals(DeviceMetadataSources.GATT_OTHER, entry.source)
    }

    @Test fun currentTimeIsFormatted() {
        // year=2026, month=11, day=22, 19:30:15
        val value = bytes(0xEA, 0x07, 0x0B, 0x16, 0x13, 0x1E, 0x0F, 0x00)
        val entry = DeviceMetadataGattDecoder.decode("00002a2b-0000-1000-8000-00805f9b34fb", value, now)!!
        assertEquals("2026-11-22 19:30:15", entry.valueText)
    }

    @Test fun emptyValueIsIgnored() {
        assertNull(DeviceMetadataGattDecoder.decode("00002a19-0000-1000-8000-00805f9b34fb", ByteArray(0), now))
    }
}

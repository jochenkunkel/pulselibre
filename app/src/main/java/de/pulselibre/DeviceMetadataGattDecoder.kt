package de.pulselibre

/**
 * Pure decoder for read-only GATT metadata characteristics (#142/#159).
 * Known Bluetooth SIG UUIDs get stable field keys; every other readable characteristic is
 * still stored with its UUID as the key and a hex value — nothing offered by the band is dropped.
 */
internal object DeviceMetadataGattDecoder {
    /** A bounded view of one read characteristic result, or null when it carried no usable value. */
    fun decode(uuid: String, value: ByteArray, nowMs: Long): LiveDeviceMetadata? {
        if (value.isEmpty()) return null
        val key = uuid.lowercase(java.util.Locale.ROOT).let { short ->
            when {
                short.startsWith("0000") && short.length >= 8 -> short.substring(4, 8)
                short.length >= 4 -> short.take(4)
                else -> short
            }
        }
        return when (key) {
            "2a19" -> LiveDeviceMetadata(
                DeviceMetadataFields.BATTERY_LEVEL, (value[0].toInt() and 0xff).toString(), "%",
                DeviceMetadataSources.GATT_BATTERY, nowMs)
            "2a24" -> text(DeviceMetadataFields.MODEL_NUMBER, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a25" -> text(DeviceMetadataFields.SERIAL_NUMBER, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a26" -> text(DeviceMetadataFields.FIRMWARE_VERSION, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a27" -> text(DeviceMetadataFields.HARDWARE_VERSION, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a28" -> text(DeviceMetadataFields.SOFTWARE_VERSION, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a29" -> text(DeviceMetadataFields.MANUFACTURER, DeviceMetadataSources.GATT_DEVICE_INFO, value, nowMs)
            "2a00" -> text(DeviceMetadataFields.DEVICE_NAME, DeviceMetadataSources.GATT_DEVICE_NAME, value, nowMs)
            "2a2b" -> currentTime(value, nowMs)
            else -> LiveDeviceMetadata("gatt_$key", hex(value), null, DeviceMetadataSources.GATT_OTHER, nowMs)
        }
    }

    private fun text(field: String, source: String, value: ByteArray, nowMs: Long): LiveDeviceMetadata? {
        val raw = String(value, Charsets.UTF_8).trim('\u0000').trim()
        if (raw.isEmpty()) return null
        return LiveDeviceMetadata(field, raw.take(256), null, source, nowMs)
    }

    /** Bluetooth Current Time service (0x2A2B) precise time: year, month, day, hour, min, sec. */
    private fun currentTime(value: ByteArray, nowMs: Long): LiveDeviceMetadata? {
        if (value.size < 7) return null
        val year = u16(value, 0)
        val month = u8(value[2]); val day = u8(value[3])
        val hour = u8(value[4]); val minute = u8(value[5]); val second = u8(value[6])
        if (year !in 1900..9999 || month !in 1..12 || day !in 1..31 || hour > 23 || minute > 59 || second > 60) {
            return LiveDeviceMetadata(DeviceMetadataFields.BAND_CLOCK, hex(value), null,
                DeviceMetadataSources.GATT_CURRENT_TIME, nowMs)
        }
        val text = "%04d-%02d-%02d %02d:%02d:%02d".format(year, month, day, hour, minute, second)
        return LiveDeviceMetadata(DeviceMetadataFields.BAND_CLOCK, text, null,
            DeviceMetadataSources.GATT_CURRENT_TIME, nowMs)
    }

    private fun hex(value: ByteArray): String = value.take(128).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun u8(b: Byte): Int = b.toInt() and 0xff
    private fun u16(b: ByteArray, i: Int): Int = u8(b[i]) or (u8(b[i + 1]) shl 8)
}

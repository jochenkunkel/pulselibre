package de.pulselibre

/**
 * One observed device value with its provenance (#142/#159).
 *
 * Every field is stored as text plus an optional unit: the band may offer values we cannot
 * anticipate, and an unknown value is still an honest observation. Nothing is invented here;
 * a field exists only when a real source supplied it.
 */
internal data class LiveDeviceMetadata(
    val fieldKey: String,
    val valueText: String,
    val unit: String? = null,
    val source: String,
    val observedAtMs: Long
)

internal object DeviceMetadataFields {
    const val BATTERY_LEVEL = "battery_level_percent"
    const val BATTERY_STATUS = "battery_status"
    const val BATTERY_HOURS = "estimated_hours_remaining"
    const val FIRMWARE_VERSION = "firmware_version"
    const val SOFTWARE_VERSION = "software_version"
    const val HARDWARE_VERSION = "hardware_version"
    const val MODEL_NUMBER = "model_number"
    const val MANUFACTURER = "manufacturer"
    const val DEVICE_NAME = "device_name"
    const val SERIAL_NUMBER = "serial_number"
    const val BAND_CLOCK = "band_clock"
    const val PROTOCOL_VERSION = "protocol_version"
    const val PRODUCT_NUMBER = "product_number"
    const val MAX_PACKET_SIZE = "max_packet_size"

    /**
     * Only the bare percentage has a dedicated battery row; status and remaining runtime must stay
     * visible as their own provenance rows so no observed value is silently filtered away.
     */
    val PROMINENT = setOf(BATTERY_LEVEL)
}

internal object DeviceMetadataSources {
    const val GATT_BATTERY = "GATT_BATTERY"
    const val GATT_DEVICE_INFO = "GATT_DEVICE_INFO"
    const val GATT_DEVICE_NAME = "GATT_DEVICE_NAME"
    const val GATT_CURRENT_TIME = "GATT_CURRENT_TIME"
    const val GATT_OTHER = "GATT_OTHER"
    const val GARMIN_GDI_STATUS = "GARMIN_GDI_STATUS"
    /** Additional values the band offers inside Garmin ML notifications (not a battery reading). */
    const val GARMIN_GDI_OTHER = "GARMIN_GDI_OTHER"
    const val GARMIN_5024 = "GARMIN_5024"
    const val FIT_DEVICE_INFO = "FIT_DEVICE_INFO"
}

/** Pure mapping/validation helpers; no Android or Bluetooth dependencies. */
internal object DeviceMetadataFormat {
    /** Garmin GDI RemoteDeviceBatteryStatusBatteryStatus enum names from Connect 5.31s. */
    private val GDI_CHARGING = setOf("CHARGING")
    private val GDI_FULL = setOf("FULLY_CHARGED")

    fun isCharging(statusText: String?): Boolean =
        statusText != null && statusText.uppercase(java.util.Locale.ROOT) in GDI_CHARGING

    fun isFullyCharged(statusText: String?): Boolean =
        statusText != null && statusText.uppercase(java.util.Locale.ROOT) in GDI_FULL

    fun chargingFromFitCode(code: Int?): Boolean = code == 6

    /** Build the battery row from the newest stored values; null when no percentage was observed. */
    fun batteryReading(values: List<LiveDeviceMetadata>): BatteryReading? {
        val percentEntry = values.filter { it.fieldKey == DeviceMetadataFields.BATTERY_LEVEL }
            .maxByOrNull { it.observedAtMs } ?: return null
        val percent = percentEntry.valueText.toIntOrNull()?.takeIf { it in 0..100 } ?: return null
        val statusEntry = values.filter { it.fieldKey == DeviceMetadataFields.BATTERY_STATUS }
            .maxByOrNull { it.observedAtMs }
        val statusText = statusEntry?.valueText?.takeIf { it.isNotBlank() }
        val fitCode = statusText?.toIntOrNull()
        return BatteryReading(
            percent = percent,
            statusCode = fitCode,
            observedAtMs = percentEntry.observedAtMs,
            source = percentEntry.source,
            statusLabel = if (fitCode == null) statusText else null
        )
    }

    /** Identifiers stay in the local database but are masked wherever a value is displayed. */
    fun displayValue(entry: LiveDeviceMetadata, strings: Strings): String = when (entry.fieldKey) {
        DeviceMetadataFields.SERIAL_NUMBER -> strings.get(R.string.dev_meta_masked)
        else -> buildString {
            append(entry.valueText)
            if (!entry.unit.isNullOrBlank()) append(' ').append(entry.unit)
        }
    }

    fun label(entry: LiveDeviceMetadata, strings: Strings): String = when (entry.fieldKey) {
        DeviceMetadataFields.FIRMWARE_VERSION -> strings.get(R.string.dev_meta_firmware)
        DeviceMetadataFields.SOFTWARE_VERSION -> strings.get(R.string.dev_meta_software)
        DeviceMetadataFields.HARDWARE_VERSION -> strings.get(R.string.dev_meta_hardware)
        DeviceMetadataFields.MODEL_NUMBER -> strings.get(R.string.dev_meta_model)
        DeviceMetadataFields.MANUFACTURER -> strings.get(R.string.dev_meta_manufacturer)
        DeviceMetadataFields.DEVICE_NAME -> strings.get(R.string.dev_meta_device_name)
        DeviceMetadataFields.SERIAL_NUMBER -> strings.get(R.string.dev_meta_identity)
        DeviceMetadataFields.BAND_CLOCK -> strings.get(R.string.dev_meta_clock)
        DeviceMetadataFields.PROTOCOL_VERSION -> strings.get(R.string.dev_meta_protocol)
        DeviceMetadataFields.PRODUCT_NUMBER -> strings.get(R.string.dev_meta_product)
        DeviceMetadataFields.MAX_PACKET_SIZE -> strings.get(R.string.dev_meta_packet)
        DeviceMetadataFields.BATTERY_HOURS -> strings.get(R.string.dev_meta_runtime)
        else -> entry.fieldKey.replace('_', ' ')
    }
}

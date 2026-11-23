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
        in GattStandardFields.KEYS -> GattStandardFields.describe(entry.fieldKey, entry.valueText, strings)
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
        in GattStandardFields.KEYS -> GattStandardFields.label(entry.fieldKey, strings)
        else -> entry.fieldKey.replace('_', ' ')
    }
}

/**
 * Readable names and values for the standard Bluetooth SIG characteristics the band offers (Bluetooth Assigned Numbers /
 * GATT Specification Supplement). The stored value stays the raw hex; this only translates it for display, and falls back
 * to the hex text whenever the length or content does not match the specification. Nothing is guessed.
 */
internal object GattStandardFields {
    const val APPEARANCE = "gatt_2a01"
    const val PREFERRED_CONNECTION = "gatt_2a04"
    const val ADDRESS_RESOLUTION = "gatt_2aa6"
    const val RSC_FEATURE = "gatt_2a54"
    /** Only the first 16 bits of a Garmin 128-bit UUID are kept by the decoder (service 6A4E2800-667B-11E3-949A-0800200C9A66). */
    const val GARMIN_VENDOR = "gatt_6a4e"

    val KEYS = setOf(APPEARANCE, PREFERRED_CONNECTION, ADDRESS_RESOLUTION, RSC_FEATURE, GARMIN_VENDOR)

    fun label(key: String, strings: Strings): String = strings.get(when (key) {
        APPEARANCE -> R.string.dev_meta_gatt_appearance
        PREFERRED_CONNECTION -> R.string.dev_meta_gatt_conn_pref
        ADDRESS_RESOLUTION -> R.string.dev_meta_gatt_addr_res
        RSC_FEATURE -> R.string.dev_meta_gatt_rsc
        else -> R.string.dev_meta_gatt_garmin
    })

    fun describe(key: String, hex: String, strings: Strings): String {
        val bytes = parseHex(hex) ?: return hex
        return when (key) {
            APPEARANCE -> appearance(bytes, hex, strings)
            PREFERRED_CONNECTION -> connection(bytes, hex, strings)
            ADDRESS_RESOLUTION -> if (bytes.size == 1 && bytes[0] == 1) strings.get(R.string.dev_val_supported)
                else if (bytes.size == 1 && bytes[0] == 0) strings.get(R.string.dev_val_not_supported) else hex
            RSC_FEATURE -> if (bytes.size == 2 && bytes[0] == 0 && bytes[1] == 0) strings.get(R.string.dev_val_rsc_none) else hex
            else -> hex
        }
    }

    private fun parseHex(hex: String): IntArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        return IntArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toIntOrNull(16) ?: return null }
    }

    /** GAP Appearance (0x2A01, uint16 little endian): category 3 = watch, sub-category 1 = sports watch. */
    private fun appearance(b: IntArray, hex: String, strings: Strings): String {
        if (b.size != 2) return hex
        return when (val v = b[0] or (b[1] shl 8)) {
            0x00C0 -> strings.get(R.string.dev_val_appearance_watch)
            0x00C1 -> strings.get(R.string.dev_val_appearance_sports_watch)
            else -> strings.get(R.string.dev_val_appearance_other, "0x%04X".format(v))
        }
    }

    /** Peripheral Preferred Connection Parameters (0x2A04): 4 x uint16 LE; intervals in 1.25 ms, timeout in 10 ms. */
    private fun connection(b: IntArray, hex: String, strings: Strings): String {
        if (b.size != 8) return hex
        fun u16(i: Int) = b[i] or (b[i + 1] shl 8)
        val min = u16(0) * 1.25; val max = u16(2) * 1.25; val latency = u16(4); val timeoutMs = u16(6) * 10
        fun ms(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')
        return strings.get(R.string.dev_val_conn_params, ms(min), ms(max), latency, timeoutMs)
    }
}

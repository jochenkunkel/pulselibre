package de.pulselibre

/**
 * What the four fixed rows of the band information card show, and which stored values they already cover (so the list
 * below does not repeat them). Pure and JVM-testable; nothing is invented: a row has a value only when a real
 * observation exists.
 */
internal object DeviceInfoRows {
    data class Shown(val text: String, val observedAtMs: Long, val source: String)

    private fun newest(values: List<LiveDeviceMetadata>, key: String) =
        values.filter { it.fieldKey == key && it.valueText.isNotBlank() }.maxByOrNull { it.observedAtMs }

    private fun shown(e: LiveDeviceMetadata?, strings: Strings): Shown? =
        e?.let { Shown(DeviceMetadataFormat.displayValue(it, strings), it.observedAtMs, it.source) }

    /** "Firmware" row: the band reports its version as "software version"; a firmware value is the fallback. */
    fun firmware(values: List<LiveDeviceMetadata>, strings: Strings): Shown? =
        shown(newest(values, DeviceMetadataFields.SOFTWARE_VERSION) ?: newest(values, DeviceMetadataFields.FIRMWARE_VERSION), strings)

    fun bandClock(values: List<LiveDeviceMetadata>, strings: Strings): Shown? =
        shown(newest(values, DeviceMetadataFields.BAND_CLOCK), strings)

    /** Identity row: device name and the Garmin-specific value (presumably the serial number), joined with " / ". */
    fun identity(values: List<LiveDeviceMetadata>, strings: Strings): Shown? {
        val parts = listOfNotNull(newest(values, DeviceMetadataFields.DEVICE_NAME), newest(values, GattStandardFields.GARMIN_VENDOR))
        if (parts.isEmpty()) return null
        return Shown(parts.joinToString(" / ") { DeviceMetadataFormat.displayValue(it, strings) },
            parts.maxOf { it.observedAtMs }, parts.first().source)
    }

    /** Entries the fixed rows already show; a differing firmware value stays visible in the list. */
    fun coveredByFixedRows(values: List<LiveDeviceMetadata>, strings: Strings): (LiveDeviceMetadata) -> Boolean {
        val fw = firmware(values, strings)?.text
        return { e ->
            when (e.fieldKey) {
                DeviceMetadataFields.SOFTWARE_VERSION, DeviceMetadataFields.BAND_CLOCK,
                DeviceMetadataFields.DEVICE_NAME, GattStandardFields.GARMIN_VENDOR -> true
                DeviceMetadataFields.FIRMWARE_VERSION -> DeviceMetadataFormat.displayValue(e, strings) == fw
                else -> false
            }
        }
    }

    const val GDI_TEXT_PREFIX = "gdi_text_"
    fun gdiTextNumber(key: String): Int? = if (key.startsWith(GDI_TEXT_PREFIX)) key.removePrefix(GDI_TEXT_PREFIX).toIntOrNull() else null
}

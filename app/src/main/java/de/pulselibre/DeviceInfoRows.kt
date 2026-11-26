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

    /**
     * Adds the newest FIT software version when it is newer than any stored live software value (or there is none), so
     * the Firmware row shows the version from the band's own files. Never invents a value.
     */
    fun withFitSoftwareVersion(live: List<LiveDeviceMetadata>, fit: LiveDeviceMetadata?): List<LiveDeviceMetadata> {
        if (fit == null || fit.valueText.isBlank()) return live
        val newestLive = newest(live, DeviceMetadataFields.SOFTWARE_VERSION)
        return if (newestLive != null && newestLive.observedAtMs >= fit.observedAtMs) live
            else live.filterNot { it.fieldKey == DeviceMetadataFields.SOFTWARE_VERSION } + fit
    }

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

    /**
     * Freshness line of a device value: "vor 5 min" while fresh, the date of an older but known observation
     * ("Stand 25.11.2026 22:14"), and "Zeitpunkt unbekannt" only when there really is no usable time (owner 26.11.2026:
     * the FIT firmware value has a known, just older time).
     */
    fun freshness(strings: Strings, observedAtMs: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
        val age = nowMs - observedAtMs
        return when {
            age in 0..DeviceBatteryFormat.STALE_AFTER_MS -> DeviceBatteryFormat.ageText(strings, age)
            observedAtMs > 0 && age > 0 -> strings.get(R.string.dev_meta_as_of,
                java.time.Instant.ofEpochMilli(observedAtMs).atZone(zone).format(AS_OF))
            else -> strings.get(R.string.bat_time_unknown)
        }
    }
    private val AS_OF = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    const val GDI_TEXT_PREFIX = "gdi_text_"
    fun gdiTextNumber(key: String): Int? = if (key.startsWith(GDI_TEXT_PREFIX)) key.removePrefix(GDI_TEXT_PREFIX).toIntOrNull() else null
}

package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant

/** Wear state of the band as shown in the header button (#649). Only evidenced states, otherwise [UNKNOWN]. */
internal enum class WearState { WORN, NOT_WORN, OFFLINE, UNKNOWN }

/** Newest live respiration value (service 21) with its reception time; the only wear evidence the app has today. */
internal data class RespirationSample(val receivedAtMs: Long, val value: Double)

/**
 * Pure rules of the combined band status / battery / settings button (#649).
 *
 * Wear evidence: the band itself reports the documented Garmin respiration status code -100 ("off wrist") when it is
 * not worn, and a respiration rate only while it measures on the wrist. Nothing else (no heart-rate guess, no motion
 * heuristic) is used; without a fresh respiration reading the state stays [WearState.UNKNOWN].
 */
internal object WearableStatus {
    const val OFF_WRIST_CODE = -100.0
    /** Same freshness as the connection status: older evidence says nothing about now. */
    const val FRESH_MS = CirqaLinkStatus.FRESH_READING_MS
    /** A band battery value older than this is not shown as a current number. */
    const val BATTERY_MAX_AGE_MS = 12 * 3_600_000L
    const val LOW_BATTERY_PERCENT = 15

    fun state(online: Boolean, respiration: RespirationSample?, nowMs: Long): WearState {
        if (!online) return WearState.OFFLINE
        val sample = respiration?.takeIf { it.receivedAtMs in (nowMs - FRESH_MS)..(nowMs + 60_000L) } ?: return WearState.UNKNOWN
        return when {
            sample.value == OFF_WRIST_CODE -> WearState.NOT_WORN
            sample.value > 0 && sample.value.isFinite() -> WearState.WORN
            else -> WearState.UNKNOWN // -200 large motion, -300 invalid: no statement about wearing
        }
    }

    /** Percentage to draw, or null for "–": offline, no value, a value without time or older than [BATTERY_MAX_AGE_MS]. */
    fun shownBattery(online: Boolean, reading: BatteryReading?, nowMs: Long): Int? {
        if (!online || reading == null) return null
        val observed = reading.observedAtMs ?: return null
        if (observed > nowMs + 60_000L || nowMs - observed > BATTERY_MAX_AGE_MS) return null
        return reading.percent.takeIf { it in 0..100 }
    }

    fun isLow(percent: Int?): Boolean = percent != null && percent <= LOW_BATTERY_PERCENT

    /** Newest of the live (GATT/GDI) and FIT battery values. */
    fun newestBattery(vararg readings: BatteryReading?): BatteryReading? =
        readings.filterNotNull().maxByOrNull { it.observedAtMs ?: Long.MIN_VALUE }

    fun description(strings: Strings, link: CirqaLinkStatus.Result, state: WearState, shownPercent: Int?,
                    battery: BatteryReading?, nowMs: Long): String {
        val wear = strings.get(when (state) {
            WearState.WORN -> R.string.wear_worn
            WearState.NOT_WORN -> R.string.wear_not_worn
            WearState.OFFLINE -> R.string.wear_offline
            WearState.UNKNOWN -> R.string.wear_unknown
        })
        val batteryText = if (shownPercent == null) strings.get(R.string.wear_battery_none) else {
            val age = battery?.observedAtMs?.let { CirqaLinkStatus.ageText(strings, (nowMs - it).coerceAtLeast(0)) } ?: ""
            strings.get(R.string.wear_battery_value, shownPercent, age) +
                (if (isLow(shownPercent)) strings.get(R.string.wear_battery_low) else "")
        }
        return strings.get(R.string.wear_button_cd, link.label, wear, batteryText)
    }
}

/** Reads the inputs of the header button from local storage only: no BLE access, no sync. Call off the UI thread. */
internal object WearableStatusReader {
    data class Inputs(val respiration: RespirationSample?, val battery: BatteryReading?)

    fun read(context: Context, database: CirqaDatabase): Inputs {
        val respiration = try {
            database.readableDatabase.rawQuery(
                """SELECT payload_json FROM events WHERE kind = 'reading' AND service = 21
                   ORDER BY dashboard_time DESC, id DESC LIMIT 1""", null
            ).use { c ->
                if (!c.moveToFirst()) null else {
                    val item = JSONObject(c.getString(0))
                    val time = Instant.parse(item.getString("received_at_utc")).toEpochMilli()
                    val value = (item.opt("breaths_per_min") as? Number)?.toDouble()
                    value?.let { RespirationSample(time, it) }
                }
            }
        } catch (_: Exception) { null }
        val live = try { DeviceMetadataFormat.batteryReading(database.latestLiveDeviceMetadata()) } catch (_: Exception) { null }
        return Inputs(respiration, WearableStatus.newestBattery(live, DeviceBatteryReader.read(context)))
    }
}

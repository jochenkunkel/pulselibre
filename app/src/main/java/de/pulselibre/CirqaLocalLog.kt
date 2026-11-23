package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant

/**
 * SQLite-backed CIRQA event store. No device IDs, MACs,
 * raw GATT payloads, cloud, or external storage permission.
 */
class CirqaLocalLog(private val context: Context) {
    @Synchronized fun append(type: String, fields: Map<String, Any?> = emptyMap()): Boolean {
        val item = JSONObject()
            .put("schema", 1)
            .put("received_at_utc", Instant.now().toString())
            .put("type", type)
        for ((key, value) in fields) item.put(key, value ?: JSONObject.NULL)
        // A transient SQLITE_BUSY ("database is locked") from a concurrent connection of this process must not look
        // like a full store: the recorder stops on `false` (S10 test 23.11.: stop after 40 s, reason storage_limit).
        repeat(BUSY_RETRIES) { attempt ->
            try {
                return CirqaDatabase(context).use { db -> db.insertLine(item.toString()) }
            } catch (_: android.database.sqlite.SQLiteDatabaseLockedException) {
                if (attempt < BUSY_RETRIES - 1) Thread.sleep(BUSY_RETRY_DELAY_MS)
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    private companion object {
        const val BUSY_RETRIES = 6
        const val BUSY_RETRY_DELAY_MS = 250L
    }

    fun reading(service: Int, value: GarminRealtime.Reading): Boolean {
        val fields: Map<String, Any?> = when (value) {
            is GarminRealtime.Reading.HeartRate -> mapOf("bpm" to value.bpm, "resting_bpm" to value.restingBpm)
            is GarminRealtime.Reading.Steps -> mapOf("steps" to value.count, "goal" to value.goal)
            is GarminRealtime.Reading.SpO2 -> mapOf("spo2_percent" to value.percent)
            is GarminRealtime.Reading.Hrv -> mapOf("rr_ms" to value.rrMs)
            is GarminRealtime.Reading.Stress -> mapOf("stress" to value.level)
            is GarminRealtime.Reading.BodyBattery -> mapOf("body_battery" to value.level)
            is GarminRealtime.Reading.Respiration -> mapOf("breaths_per_min" to value.breathsPerMinute)
            is GarminRealtime.Reading.Calories -> mapOf("total" to value.total, "active" to value.active)
            is GarminRealtime.Reading.Intensity -> mapOf("moderate" to value.moderate, "vigorous" to value.vigorous)
        }
        return append("reading", fields + ("service" to service))
    }

    fun sizeBytes(): Long = CirqaDatabase(context).use { db ->
        db.eventBytes()
    }
    fun lines(): Int = CirqaDatabase(context).use { db ->
        db.eventCount().toInt()
    }
    fun copyTo(context: Context, uri: android.net.Uri) {
        CirqaDatabase(context).use { db ->
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { output ->
                db.allLines().forEach { output.append(it).append('\n') }
            } ?: error("Exportziel nicht beschreibbar")
        }
    }
}

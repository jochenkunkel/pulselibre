package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * SQLite-backed CIRQA event store; legacy JSONL is imported idempotently. No device IDs, MACs,
 * raw GATT payloads, cloud, or external storage permission.
 */
class CirqaLocalLog(private val context: Context) {
    private val file = File(context.filesDir, "cirqa_overnight.jsonl")


    @Synchronized fun append(type: String, fields: Map<String, Any?> = emptyMap()): Boolean {
        val item = JSONObject()
            .put("schema", 1)
            .put("received_at_utc", Instant.now().toString())
            .put("type", type)
        for ((key, value) in fields) item.put(key, value ?: JSONObject.NULL)
        return try {
            CirqaDatabase(context).use { db ->
                db.importJsonl()
                db.insertLine(item.toString())
            }
        } catch (_: Exception) { false }
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
        db.importJsonl()
        db.eventBytes()
    }
    fun lines(): Int = CirqaDatabase(context).use { db ->
        db.importJsonl()
        db.eventCount().toInt()
    }
    fun copyTo(context: Context, uri: android.net.Uri) {
        CirqaDatabase(context).use { db ->
            db.importJsonl()
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { output ->
                db.allLines().forEach { output.append(it).append('\n') }
            } ?: error("Exportziel nicht beschreibbar")
        }
    }
}

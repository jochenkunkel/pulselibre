package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * Private, append-only, bounded JSON Lines diagnostic. No device IDs, MACs,
 * raw GATT payloads, cloud, or external storage permission.
 */
class CirqaLocalLog(context: Context) {
    private val file = File(context.filesDir, "cirqa_overnight.jsonl")
    private val maxBytes = 10L * 1024 * 1024

    @Synchronized fun append(type: String, fields: Map<String, Any?> = emptyMap()): Boolean {
        val item = JSONObject()
            .put("schema", 1)
            .put("received_at_utc", Instant.now().toString())
            .put("type", type)
        for ((key, value) in fields) item.put(key, value ?: JSONObject.NULL)
        val line = item.toString() + "\n"
        if (file.length() + line.toByteArray(Charsets.UTF_8).size > maxBytes) return false
        file.appendText(line, Charsets.UTF_8)
        return true
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

    fun sizeBytes(): Long = file.length()
    fun lines(): Int = if (!file.exists()) 0 else file.bufferedReader().use { reader ->
        reader.lineSequence().count()
    }
    fun copyTo(context: Context, uri: android.net.Uri) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            if (file.exists()) file.inputStream().use { it.copyTo(output) }
        } ?: error("Exportziel nicht beschreibbar")
    }
}

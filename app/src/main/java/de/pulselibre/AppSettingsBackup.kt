package de.pulselibre

import android.content.Context
import org.json.JSONObject

/**
 * Personal configuration in the backup (owner request 26.11.2026, backup v12): profile (name, birth date, sex, height,
 * weight, goals, wrist, units, resting pulse, note, SpO₂ thresholds), theme and language, activity detection, recording
 * auto-start and the dashboard layout. Only these user stores are written; technical state (BLE attempt gates,
 * recorder flags, diagnostics) never is, so a restore cannot revive a stale protocol state.
 *
 * One JSON line per value: `{"store":…, "key":…, "type":"string|int|long|float|boolean", "value":…}`.
 */
internal object AppSettingsBackup {
    const val ENTRY = "app_settings.jsonl"
    val STORES = listOf("local_personal_profile", "app_appearance", "activity_detection", "recording_settings", "dashboard_display")
    private val KEY = Regex("[A-Za-z0-9_.-]{1,64}")
    private const val MAX_STRING = 16 * 1024
    private const val MAX_VALUES = 500

    data class Value(val store: String, val key: String, val value: Any)

    fun export(context: Context, emit: (String) -> Unit) {
        for (store in STORES) {
            context.getSharedPreferences(store, Context.MODE_PRIVATE).all.toSortedMap().forEach { (key, value) ->
                val type = when (value) {
                    is String -> "string"; is Int -> "int"; is Long -> "long"; is Float -> "float"; is Boolean -> "boolean"
                    else -> return@forEach // string sets are not used by these stores
                }
                if (!KEY.matches(key) || (value is String && value.length > MAX_STRING)) return@forEach
                emit(JSONObject().put("store", store).put("key", key).put("type", type)
                    .put("value", if (value is Float) value.toDouble() else value).toString())
            }
        }
    }

    /** Strict parser: unknown stores, keys, types or oversized values reject the whole backup before restore. */
    fun parse(line: String): Value {
        val o = JSONObject(line)
        require(o.length() == 4) { "Ungültiger Einstellungseintrag im Backup" }
        val store = o.getString("store")
        require(store in STORES) { "Unbekannter Einstellungsbereich im Backup" }
        val key = o.getString("key")
        require(KEY.matches(key)) { "Ungültiger Einstellungsname im Backup" }
        val value: Any = when (o.getString("type")) {
            "string" -> o.get("value").let { require(it is String && it.length <= MAX_STRING) { "Ungültiger Einstellungswert" }; it }
            "int" -> o.get("value").let { require(it is Int) { "Ungültiger Einstellungswert" }; it }
            "long" -> o.get("value").let { require(it is Int || it is Long) { "Ungültiger Einstellungswert" }; (it as Number).toLong() }
            "float" -> o.get("value").let {
                require(it is Number && it.toDouble().isFinite()) { "Ungültiger Einstellungswert" }; it.toDouble().toFloat()
            }
            "boolean" -> o.get("value").let { require(it is Boolean) { "Ungültiger Einstellungswert" }; it }
            else -> throw IllegalArgumentException("Unbekannter Einstellungstyp im Backup")
        }
        return Value(store, key, value)
    }

    fun validateAll(lines: Sequence<String>): List<Value> {
        val values = lines.map(::parse).toList()
        require(values.size <= MAX_VALUES) { "Zu viele Einstellungen im Backup" }
        require(values.map { it.store to it.key }.toSet().size == values.size) { "Doppelte Einstellung im Backup" }
        return values
    }

    /**
     * Replaces the five stores with the backup's values (stores missing in the backup are emptied, i.e. defaults).
     * The caller applies theme and language on the main thread afterwards (AppAppearance.apply, activity recreate).
     */
    fun restore(context: Context, values: List<Value>) {
        for (store in STORES) {
            val edit = context.getSharedPreferences(store, Context.MODE_PRIVATE).edit().clear()
            values.filter { it.store == store }.forEach { v ->
                when (val x = v.value) {
                    is String -> edit.putString(v.key, x); is Int -> edit.putInt(v.key, x); is Long -> edit.putLong(v.key, x)
                    is Float -> edit.putFloat(v.key, x); is Boolean -> edit.putBoolean(v.key, x)
                }
            }
            edit.commit()
        }
    }
}

package de.pulselibre

import android.content.Context

/**
 * PULSELIBRE EXPERIMENTAL HEURISTIC — NOT CLINICALLY VALIDATED for CIRQA wrist readings.
 * Reference thresholds are an orientation preview behind the device-validation gate.
 */
internal data class SpO2DisplaySettings(
    val greenFrom: Int = 95,
    val redBelow: Int = 90,
    val colorBandsEnabled: Boolean = false
) {
    fun colorFor(percent: Int?, palette: AppPalette): Int {
        val value = percent?.takeIf { it in 1..100 } ?: return missingColor(palette)
        if (!colorBandsEnabled) return neutralColor(palette)
        return when {
            value >= greenFrom -> palette.indicator
            value >= redBelow -> palette.oxygen
            else -> android.graphics.Color.rgb(205, 57, 65)
        }
    }

    fun bandLabel(): String =
        "🟢 $greenFrom–100 %   🟠 $redBelow–${greenFrom - 1} %   🔴 < $redBelow %   ⚪ kein gültiger Wert"

    companion object {
        private const val STORE = "local_personal_profile"
        private const val GREEN_FROM_KEY = "spo2_display_green_from"
        private const val RED_BELOW_KEY = "spo2_display_red_below"
        private const val VALIDATION_GATE_KEY = "spo2_device_validation_recorded"
        internal fun isInRange(percent: Int?): Boolean = percent != null && percent in 1..100
        internal fun neutralColor(palette: AppPalette): Int = palette.ink
        internal fun missingColor(palette: AppPalette): Int = palette.muted

        fun read(context: Context): SpO2DisplaySettings {
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            val green = prefs.getInt(GREEN_FROM_KEY, 95).coerceIn(2, 100)
            val red = prefs.getInt(RED_BELOW_KEY, 90).coerceIn(1, green)
            return SpO2DisplaySettings(green, red, prefs.getBoolean(VALIDATION_GATE_KEY, false))
        }

        /** The gate is deliberately inaccessible from ordinary preferences. */
        fun recordDeviceValidation(context: Context, validated: Boolean) {
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putBoolean(VALIDATION_GATE_KEY, validated).apply()
        }

        fun save(context: Context, settings: SpO2DisplaySettings) {
            require(settings.greenFrom in 2..100)
            require(settings.redBelow in 1 until settings.greenFrom)
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putInt(GREEN_FROM_KEY, settings.greenFrom)
                .putInt(RED_BELOW_KEY, settings.redBelow)
                // Preserve the validation gate: changing display thresholds does not validate a sensor.
                .apply()
        }
    }
}

package de.pulselibre

import android.content.Context

/** User-configurable visual bands; these are not a device signal-quality or clinical assessment. */
internal data class SpO2DisplaySettings(
    val greenFrom: Int = 95,
    val orangeFrom: Int = 93
) {
    fun colorFor(percent: Int?, palette: AppPalette): Int {
        if (percent == null || percent !in 1..100) return palette.muted
        return when {
            percent >= greenFrom -> palette.indicator
            percent >= orangeFrom -> palette.oxygen
            else -> android.graphics.Color.rgb(205, 57, 65)
        }
    }

    companion object {
        private const val STORE = "local_personal_profile"
        private const val GREEN_FROM_KEY = "spo2_display_green_from"
        private const val ORANGE_FROM_KEY = "spo2_display_orange_from"

        fun read(context: Context): SpO2DisplaySettings {
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            val green = prefs.getInt(GREEN_FROM_KEY, 95).coerceIn(1, 100)
            val orange = prefs.getInt(ORANGE_FROM_KEY, 93).coerceIn(1, green)
            return SpO2DisplaySettings(green, orange)
        }

        fun save(context: Context, settings: SpO2DisplaySettings) {
            require(settings.greenFrom in 1..100)
            require(settings.orangeFrom in 1 until settings.greenFrom)
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putInt(GREEN_FROM_KEY, settings.greenFrom)
                .putInt(ORANGE_FROM_KEY, settings.orangeFrom)
                .apply()
        }
    }
}

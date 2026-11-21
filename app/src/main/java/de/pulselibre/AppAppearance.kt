package de.pulselibre

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate

object AppAppearance {
    private const val STORE = "app_appearance"
    fun preference(context: Context): ThemePreference = ThemePreference.from(
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString("theme", null))

    fun save(context: Context, preference: ThemePreference) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putString("theme", preference.storedValue).apply()
        apply(preference)
    }

    fun apply(preference: ThemePreference) {
        AppCompatDelegate.setDefaultNightMode(when (preference) {
            ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        })
    }
}

class PulseLibreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAppearance.apply(AppAppearance.preference(this))
        AppLanguageSettings.apply(AppLanguageSettings.preference(this))
    }
}

/** AppCompat's resolved configuration includes either the system mode or the user's override. */
data class AppPalette(val dark: Boolean) {
    val background = Color.parseColor(if (dark) "#0D1220" else "#F8F7FC")
    val card = Color.parseColor(if (dark) "#171E30" else "#FFFFFF")
    val ink = Color.parseColor(if (dark) "#F4F2FB" else "#231D3B")
    val muted = Color.parseColor(if (dark) "#B9B4CB" else "#6D687F")
    val accent = Color.parseColor(if (dark) "#C0A0FF" else "#5127C9")
    val primary = Color.parseColor(if (dark) "#7950D2" else "#5127C9")
    val tinted = Color.parseColor(if (dark) "#2B2443" else "#F1EBFF")
    val header = Color.parseColor(if (dark) "#12192A" else "#5127C9")
    val headerEnd = Color.parseColor(if (dark) "#25203D" else "#5E41BB")
    val pulse = Color.parseColor(if (dark) "#FF97BE" else "#A6264F")
    val oxygen = Color.parseColor(if (dark) "#8FCBFF" else "#1B5B9C")
    val indicator = Color.parseColor(if (dark) "#52DDB2" else "#167B60")
    val track = Color.parseColor(if (dark) "#354158" else "#E8E2F2")

    companion object {
        fun of(context: Context) = AppPalette(
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    }
}

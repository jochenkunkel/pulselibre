package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

/**
 * App language (#i18n): default is the system language; the user may pick one of the supported languages in the settings.
 * Strings come from res/values* (values/ = English fallback for every other system language). Language names are always
 * shown in their own language so a user can find theirs whatever language the app currently speaks.
 */
enum class AppLanguage(val storedValue: String, val tag: String?, val nativeName: String?) {
    SYSTEM("system", null, null),
    GERMAN("de", "de", "Deutsch"),
    ENGLISH("en", "en", "English"),
    FRENCH("fr", "fr", "Français"),
    SPANISH("es", "es", "Español");

    fun label(context: Context): String = nativeName ?: context.getString(R.string.language_system)

    companion object {
        fun from(value: String?): AppLanguage = entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

object AppLanguageSettings {
    private const val STORE = "app_appearance"
    fun preference(context: Context): AppLanguage = AppLanguage.from(
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString("language", null))

    /**
     * Stores the choice; the visible activity is recreated (its base context picks the language up again).
     * Deterministic and independent of the system's per-app locale service (which applies asynchronously).
     */
    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putString("language", language.storedValue).commit()
        var c: Context? = context
        while (c is ContextWrapper && c !is android.app.Activity) c = c.baseContext
        (c as? android.app.Activity)?.recreate()
    }

    /**
     * Base context for activities: the chosen language, or the unchanged context for "system language" (then Android,
     * including its own per-app language setting on Android 13+, decides).
     */
    fun wrap(base: Context): Context {
        val tag = preference(base).tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}

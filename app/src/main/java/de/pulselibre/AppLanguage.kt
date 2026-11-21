package de.pulselibre

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

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

    /** Stores the choice and applies it; AppCompat recreates visible activities when the language really changes. */
    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putString("language", language.storedValue).apply()
        apply(language)
    }

    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            if (language.tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag))
    }
}

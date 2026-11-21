package de.pulselibre

/** Stable persisted values; unrecognised settings follow the host system. */
enum class ThemePreference(val storedValue: String, val labelRes: Int) {
    SYSTEM("system", R.string.theme_system), LIGHT("light", R.string.theme_light), DARK("dark", R.string.theme_dark);

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun from(value: String?): ThemePreference = entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

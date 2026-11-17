package de.pulselibre

/** Stable persisted values; unrecognised settings follow the host system. */
enum class ThemePreference(val storedValue: String, val label: String) {
    SYSTEM("system", "Systemeinstellung"), LIGHT("light", "Hell"), DARK("dark", "Dunkel");

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun from(value: String?): ThemePreference = entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

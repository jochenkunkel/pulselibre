package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class ThemePreferenceTest {
    @Test fun defaultsAndUnknownValuesFollowSystem() {
        for (value in listOf(null, "", "unsupported")) {
            val mode = ThemePreference.from(value)
            assertEquals(ThemePreference.SYSTEM, mode)
            assertFalse(mode.isDark(false))
            assertTrue(mode.isDark(true))
        }
    }
    @Test fun explicitChoiceOverridesEitherHostMode() {
        for (host in listOf(false, true)) {
            assertFalse(ThemePreference.from("light").isDark(host))
            assertTrue(ThemePreference.from("dark").isDark(host))
        }
    }
}

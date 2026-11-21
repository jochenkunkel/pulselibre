package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test fun unknownOrMissingStoredValueFallsBackToSystemLanguage() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.from(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.from(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.from("xx"))
    }

    @Test fun everyStoredValueRoundTrips() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.from(it.storedValue)) }
    }

    @Test fun storedValuesAreUnique() {
        assertEquals(AppLanguage.entries.size, AppLanguage.entries.map { it.storedValue }.toSet().size)
    }

    @Test fun onlySystemHasNoLocaleTag() {
        assertNull(AppLanguage.SYSTEM.tag)
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.forEach { assertEquals(it.storedValue, it.tag) }
    }
}

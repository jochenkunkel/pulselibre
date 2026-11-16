package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaAddressMatchTest {
    @Test fun equalAddressesAreOnlyHints() {
        assertTrue(CirqaAddressMatch.matches("AA:BB:CC:DD:EE:FF", "aa:bb:cc:dd:ee:ff"))
    }
    @Test fun missingAddressesNeverMatch() {
        assertFalse(CirqaAddressMatch.matches(null, "AA:BB"))
        assertFalse(CirqaAddressMatch.matches("AA:BB", null))
        assertFalse(CirqaAddressMatch.matches("", ""))
        assertFalse(CirqaAddressMatch.matches("  ", "AA:BB"))
    }
    @Test fun rotatingOrDifferentAddressesDoNotMatch() {
        assertFalse(CirqaAddressMatch.matches("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:00"))
    }
    @Test fun whitespaceIsIgnored() {
        assertTrue(CirqaAddressMatch.matches(" AA:BB ", "aa:bb"))
    }
}

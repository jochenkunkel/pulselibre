package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionInfoTest {
    @Test fun splitsVersionNameAndBuild() {
        val v = AppVersionInfo.of("2.0.0_build996", 996, false)
        assertEquals("2.0.0", v.version); assertEquals("2.0.0 · Build 996", v.short())
    }
    @Test fun plainVersionNameAndDebugMarker() {
        assertEquals("1.2 · Build 7 · Debug-Signatur", AppVersionInfo.of("1.2", 7, true).detail())
        assertEquals("? · Build 0", AppVersionInfo.of(null, 0, false).short())
    }
}

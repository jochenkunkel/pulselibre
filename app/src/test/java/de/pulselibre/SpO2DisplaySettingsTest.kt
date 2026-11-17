package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.AssertTrue
import org.junit.Test

class SpO2DisplaySettingsTest {
    @Test fun defaultOrientationBandsRemainDisabled() {
        val settings = SpO2DisplaySettings()
        assertEquals(95, settings.greenFrom)
        assertEquals(90, settings.redBelow)
        assertFalse(settings.colorBandsEnabled)
        assertEquals("🟢 95–100 %   🟠 90–94 %   🔴 < 90 %   ⚪ kein gültiger Wert", settings.bandLabel())
    }

    @Test fun validityRejectsSentinelAndOutOfRangeValues() {
        assertFalse(SpO2DisplaySettings.isInRange(null))
        assertFalse(SpO2DisplaySettings.isInRange(0))
        assertTrue(SpO2DisplaySettings.isInRange(1))
        assertTrue(SpO2DisplaySettings.isInRange(100))
        assertFalse(SpO2DisplaySettings.isInRange(101))
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpO2DisplaySettingsTest {
    @Test fun defaultsKeepHealthColorsOffAndExposePreviewBands() {
        val settings = SpO2DisplaySettings()
        assertEquals(95, settings.greenFrom)
        assertEquals(90, settings.redBelow)
        assertFalse(settings.colorBandsEnabled)
        assertEquals("🟢 95–100 %   🟠 90–94 %   🔴 < 90 %   ⚪ kein gültiger Wert", settings.bandLabel())
    }

    @Test fun rangeValidationRejectsSentinelAndOutOfRangeValues() {
        assertEquals(false, SpO2DisplaySettings.isInRange(null))
        assertEquals(false, SpO2DisplaySettings.isInRange(0))
        assertEquals(true, SpO2DisplaySettings.isInRange(1))
        assertEquals(true, SpO2DisplaySettings.isInRange(100))
        assertEquals(false, SpO2DisplaySettings.isInRange(101))
    }
}

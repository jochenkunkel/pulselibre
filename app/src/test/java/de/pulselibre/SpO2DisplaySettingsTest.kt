package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class SpO2DisplaySettingsTest {
    @Test fun defaultsAreAnUnvalidatedNeutralPreview() {
        val palette = palette()
        val settings = SpO2DisplaySettings()
        listOf(1, 89, 90, 94, 95, 100).forEach { value ->
            assertEquals(palette.ink, settings.colorFor(value, palette))
        }
        assertEquals(palette.muted, settings.colorFor(null, palette))
        assertEquals(palette.muted, settings.colorFor(0, palette))
    }

    @Test fun configured95And90BandsAreExactWhenValidationGateIsEnabled() {
        val palette = palette()
        val settings = SpO2DisplaySettings(greenFrom = 95, redBelow = 90, colorBandsEnabled = true)
        assertEquals(android.graphics.Color.rgb(205, 57, 65), settings.colorFor(1, palette))
        assertEquals(android.graphics.Color.rgb(205, 57, 65), settings.colorFor(89, palette))
        assertEquals(palette.oxygen, settings.colorFor(90, palette))
        assertEquals(palette.oxygen, settings.colorFor(94, palette))
        assertEquals(palette.indicator, settings.colorFor(95, palette))
        assertEquals(palette.indicator, settings.colorFor(100, palette))
    }

    @Test fun bandLabelExposesOnlyTheConfiguredOrientationRanges() {
        assertEquals("🟢 95–100 %   🟠 90–94 %   🔴 < 90 %   ⚪ kein gültiger Wert",
            SpO2DisplaySettings().bandLabel())
    }

    @Test fun rangeValidationRejectsSentinelAndOutOfRangeValues() {
        assertEquals(false, SpO2DisplaySettings.isInRange(null))
        assertEquals(false, SpO2DisplaySettings.isInRange(0))
        assertEquals(true, SpO2DisplaySettings.isInRange(1))
        assertEquals(true, SpO2DisplaySettings.isInRange(100))
        assertEquals(false, SpO2DisplaySettings.isInRange(101))
    }
}

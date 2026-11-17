package de.pulselibre

import org.junit.Test

class SpO2DisplaySettingsTest {
    @Test fun rangeValidationRejectsSentinelAndOutOfRangeValues() {
        assertEquals(false, SpO2DisplaySettings.isInRange(null))
        assertEquals(false, SpO2DisplaySettings.isInRange(0))
        assertEquals(true, SpO2DisplaySettings.isInRange(1))
        assertEquals(true, SpO2DisplaySettings.isInRange(100))
        assertEquals(false, SpO2DisplaySettings.isInRange(101))
    }
}

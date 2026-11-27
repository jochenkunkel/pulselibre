package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GroundRulesTest {
    @Test fun whoActivityGuidelineAppliesToAdults() {
        assertNull(whoModerateMinutesPerWeek(17))
        assertEquals(150, whoModerateMinutesPerWeek(18))
        assertEquals(150, whoModerateMinutesPerWeek(75))
        assertEquals(75, whoVigorousMinutesPerWeek(30))
    }
}

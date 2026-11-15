package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GroundRulesTest {
    @Test fun bmiUsesMetersSquared() {
        assertEquals(20.06920415224914, adultBmi(58.0, 170.0, 30)!!, 0.000001)
    }
    @Test fun bmiRefusesMinorsAndInvalidMeasurements() {
        assertNull(adultBmi(60.0, 170.0, 17))
        assertNull(adultBmi(60.0, 0.0, 30))
        assertNull(adultBmi(Double.NaN, 170.0, 30))
    }
    @Test fun bmiCategoriesAtWhoBoundaries() {
        assertEquals(AdultBmiCategory.UNDERWEIGHT, adultBmiCategory(18.49))
        assertEquals(AdultBmiCategory.NORMAL_RANGE, adultBmiCategory(18.5))
        assertEquals(AdultBmiCategory.OVERWEIGHT, adultBmiCategory(25.0))
        assertEquals(AdultBmiCategory.OBESITY, adultBmiCategory(30.0))
        assertNull(adultBmiCategory(null))
    }
    @Test fun whoActivityGuidelineAppliesToAdults() {
        assertNull(whoModerateMinutesPerWeek(17))
        assertEquals(150, whoModerateMinutesPerWeek(18))
        assertEquals(150, whoModerateMinutesPerWeek(75))
        assertEquals(75, whoVigorousMinutesPerWeek(30))
    }
}

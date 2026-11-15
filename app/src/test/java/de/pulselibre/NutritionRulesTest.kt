package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class NutritionRulesTest {
    @Test fun dgeProteinAdultUnder65() {
        assertEquals(56.0, dgeProteinGramsPerDay(35, 70.0, true)!!, 0.00001)
    }
    @Test fun dgeProteinOlderAdult() {
        assertEquals(70.0, dgeProteinGramsPerDay(65, 70.0, true)!!, 0.00001)
    }
    @Test fun doNotInferProteinForUnsupportedCases() {
        assertNull(dgeProteinGramsPerDay(18, 70.0, true))
        assertNull(dgeProteinGramsPerDay(35, 70.0, false))
        assertNull(dgeProteinGramsPerDay(35, Double.NaN, true))
    }
    @Test fun energyUnitConversion() {
        assertEquals(4184.0, kilocaloriesToKilojoules(1000.0)!!, 0.0001)
        assertNull(kilocaloriesToKilojoules(-1.0))
    }
    @Test fun pulseAloneCannotEstablishCalories() {
        assertNull(heartRateOnlyEnergyEstimate(110))
    }
}

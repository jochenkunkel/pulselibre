package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class MetricReferenceProfileTest {
    private fun metric(title: String) = DashboardMetric(title, "72", "")

    @Test fun restingPulseReferenceIsAdultContextAndSourceBacked() {
        val profile = MetricReferenceProfile.forMetric(metric("Ruhepuls"))!!
        assertEquals(60, profile.referenceMinimum)
        assertEquals(100, profile.referenceMaximum)
        assertEquals("American Heart Association", profile.sourceName)
        assertTrue(profile.caveat.contains("kein persönlicher Ziel- oder Diagnosewert"))
    }

    @Test fun oxygenReferenceDisclosesFingerOximeterAndWristSensorLimit() {
        val profile = MetricReferenceProfile.forMetric(metric("Sauerstoffsättigung · SpO₂"))!!
        assertEquals(95, profile.referenceMinimum)
        assertEquals(100, profile.referenceMaximum)
        assertEquals("Oxford University Hospitals NHS", profile.sourceName)
        assertTrue(profile.caveat.contains("nicht für CIRQA am Handgelenk validiert"))
    }

    @Test fun respiratoryRateReferenceIsExplicitlyForAdultsAtRest() {
        val profile = MetricReferenceProfile.forMetric(metric("Atemfrequenz"))!!
        assertEquals(12, profile.referenceMinimum)
        assertEquals(20, profile.referenceMaximum)
        assertEquals("Erwachsene in Ruhe", profile.referenceLabel)
    }

    @Test fun proprietaryPercentValuesHaveScaleButNoClinicalReferenceBand() {
        listOf("Stress (Geräteskala)", "Body Battery (Geräteskala)").forEach { title ->
            val profile = MetricReferenceProfile.forMetric(metric(title))!!
            assertEquals(0, profile.minimum)
            assertEquals(100, profile.maximum)
            assertNull(profile.referenceMinimum)
            assertNull(profile.referenceMaximum)
            assertTrue(profile.caveat.contains("kein klinischer Normbereich"))
        }
    }

    @Test fun stepProgressRequiresDeviceProvidedGoalAndIsNotAHealthRecommendation() {
        assertNull(MetricReferenceProfile.forMetric(metric("Schritte (Tageszähler)")))
        val profile = MetricReferenceProfile.forMetric(metric("Schritte (Tageszähler)"), 8000)!!
        assertEquals(8000, profile.maximum)
        assertTrue(profile.caveat.contains("keine WHO-Norm"))
    }

    @Test fun intensityMinutesHaveNoScaleUntilTheirDeviceWindowIsVerified() {
        assertNull(MetricReferenceProfile.forMetric(metric("Moderate Intensitätsminuten")))
        assertNull(MetricReferenceProfile.forMetric(metric("Intensive Intensitätsminuten")))
    }
    @Test fun stepGoalProgressUsesPercentScaleWithoutClinicalBand() {
        val profile = MetricReferenceProfile.forMetric(metric("Tagesziel Schritte"))!!
        assertEquals(0, profile.minimum)
        assertEquals(100, profile.maximum)
        assertNull(profile.referenceMinimum)
        assertNull(profile.referenceMaximum)
        assertTrue(profile.caveat.contains("keine Gesundheitsbewertung"))
    }

}

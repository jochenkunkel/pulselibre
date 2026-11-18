package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class UnvalidatedFeaturesTest {
    @Test fun experimentalAndGatedTilesAreMarked() {
        listOf("Sauerstoffsättigung · SpO₂", "Ruhepuls", "Schlaf", "Stress (Geräteskala)",
            "Body Battery (Geräteskala)", "Kalorien gesamt (Gerätewert)").forEach { assertTrue(it, UnvalidatedFeatures.isUnvalidated(it)) }
    }
    @Test fun plainTilesAreNotMarked() {
        listOf("Herzfrequenz", "Schritte", "Schrittziel", "RR-Intervall (kein RMSSD)").forEach { assertFalse(it, UnvalidatedFeatures.isUnvalidated(it)) }
    }
    @Test fun ownerAcceptedGateEnablesBandsByDefaultInStoredSettings() {
        assertTrue(SpO2DisplaySettings.OWNER_ACCEPTED_UNVALIDATED)
    }
}

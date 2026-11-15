package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaScanEvidenceTest {
    @Test fun unknownDevicesNeverCountAsCirqa() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, false)
        assertTrue(evidence.summary().contains("Adressgleichheit mit CIRQA-Kopplung: 0"))
        assertTrue(evidence.summary().contains("Nicht zuordenbare Werbegeräte: 1"))
    }

    @Test fun garminHintIsNotConfirmation() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, true)
        assertTrue(evidence.summary().contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 1"))
        assertTrue(evidence.summary().contains("Adressgleichheit mit CIRQA-Kopplung: 0"))
    }

    @Test fun duplicateUpdatesDoNotInflateCounts() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, true)
        evidence.observe("a", false, true)
        evidence.observe("a", true, true)
        assertTrue(evidence.summary().contains("Adressgleichheit mit CIRQA-Kopplung: 1"))
        assertTrue(evidence.summary().contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 0"))
    }

    @Test fun resetClearsPreviousScan() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", true, false)
        evidence.reset()
        assertTrue(evidence.summary().contains("Adressgleichheit mit CIRQA-Kopplung: 0"))
    }
    @Test fun weakerFollowupAdvertisementDoesNotEraseGarminHint() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", false, true)
        evidence.observe("a", false, false)
        assertTrue(evidence.summary().contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 1"))
        assertTrue(evidence.summary().contains("Nicht zuordenbare Werbegeräte: 0"))
    }

    @Test fun addressMatchDoesNotGetDowngraded() {
        val evidence = CirqaScanEvidence()
        evidence.observe("a", true, false)
        evidence.observe("a", false, false)
        assertTrue(evidence.summary().contains("Adressgleichheit mit CIRQA-Kopplung: 1"))
    }
    @Test fun featureCountersDeduplicateDevicesAndAccumulateFlags() {
        val evidence = CirqaScanEvidence()
        evidence.observeFeatures("a", hasName = false, hasManufacturerData = true, hasServiceUuids = false)
        evidence.observeFeatures("a", hasName = true, hasManufacturerData = false, hasServiceUuids = false)
        evidence.observeFeatures("b", hasName = false, hasManufacturerData = false, hasServiceUuids = true)
        val summary = evidence.summary()
        assertTrue(summary.contains("Werbeereignisse insgesamt: 3"))
        assertTrue(summary.contains("Geräte mit sichtbarem Namen: 1"))
        assertTrue(summary.contains("Geräte mit Herstellerdaten: 1"))
        assertTrue(summary.contains("Geräte mit Service-UUIDs: 1"))
        assertFalse(summary.contains("a:"))
    }

    @Test fun resetClearsAdvertisementFeatures() {
        val evidence = CirqaScanEvidence()
        evidence.observeFeatures("a", true, true, true)
        evidence.reset()
        assertTrue(evidence.summary().contains("Werbeereignisse insgesamt: 0"))
        assertTrue(evidence.summary().contains("Geräte mit Herstellerdaten: 0"))
    }

    @Test fun featureFlagsAreMonotonicAcrossAdvertisements() {
        val evidence = CirqaScanEvidence()
        evidence.observeFeatures("device-one", false, false, true)
        evidence.observeFeatures("device-one", false, false, false)
        evidence.observeFeatures("device-one", true, true, false)
        val summary = evidence.summary()
        assertTrue(summary.contains("Werbeereignisse insgesamt: 3"))
        assertTrue(summary.contains("Geräte mit sichtbarem Namen: 1"))
        assertTrue(summary.contains("Geräte mit Herstellerdaten: 1"))
        assertTrue(summary.contains("Geräte mit Service-UUIDs: 1"))
    }

    @Test fun aggregateSummaryNeverExposesDeviceAddressOrName() {
        val evidence = CirqaScanEvidence()
        val privateAddress = "AA:BB:CC:DD:EE:FF"
        evidence.observe(privateAddress, false, true)
        evidence.observeFeatures(privateAddress, true, true, true)
        val summary = evidence.summary()
        assertFalse(summary.contains(privateAddress))
        assertFalse(summary.contains("AA:BB"))
        assertTrue(summary.contains("Garmin-Hinweise (nicht CIRQA-bestätigt): 1"))
    }

}

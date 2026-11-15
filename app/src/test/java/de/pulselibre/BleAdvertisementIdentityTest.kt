package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class BleAdvertisementIdentityTest {
    @Test fun recognizesCompanyIdentifier() {
        assertTrue(BleAdvertisementIdentity.isGarminHint(null, listOf(0x0087), emptyList()))
    }

    @Test fun recognizesAdvertisedServiceAndName() {
        assertTrue(BleAdvertisementIdentity.isGarminHint(null, emptyList(), listOf("0000fe1f-0000-1000-8000-00805f9b34fb")))
        assertTrue(BleAdvertisementIdentity.isGarminHint("Garmin", emptyList(), emptyList()))
    }

    @Test fun unknownIsNotMisidentified() {
        assertFalse(BleAdvertisementIdentity.isGarminHint(null, listOf(0x004c), emptyList()))
        val description = BleAdvertisementIdentity.describe(null, emptyList(), emptyList(), -86)
        assertTrue(description.contains("Unbekanntes Gerät"))
        assertTrue(description.contains("Hersteller-ID: keine"))
        assertTrue(description.contains("Service-UUIDs: keine"))
    }

    @Test fun formatsCompanyIdsAsHex() {
        val description = BleAdvertisementIdentity.describe(null, listOf(0x0087), emptyList(), -65)
        assertTrue(description.contains("0x0087"))
        assertTrue(description.contains("nicht bestätigt"))
    }
}

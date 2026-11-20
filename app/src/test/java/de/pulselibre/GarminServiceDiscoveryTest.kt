package de.pulselibre

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class GarminServiceDiscoveryTest {
    @Test fun onlyReviewedCandidateIsListed() = assertEquals(listOf(16), GarminServiceDiscovery.CANDIDATES)

    @Test fun describesAcceptedRejectedAndSilent() {
        assertTrue(GarminServiceDiscovery.describe(mapOf(16 to 7), emptyList(), mapOf(16 to 3)).contains("registriert (Handle 7, Pakete 3)"))
        assertTrue(GarminServiceDiscovery.describe(emptyMap(), listOf(16), emptyMap()).contains("abgelehnt"))
        assertTrue(GarminServiceDiscovery.describe(emptyMap(), emptyList(), emptyMap()).contains("ohne bestätigte Antwort"))
    }

    @Test fun acceptedWithoutFramesReportsZeroPackets() {
        assertTrue(GarminServiceDiscovery.describe(mapOf(16 to 1), emptyList(), emptyMap()).contains("Pakete 0"))
    }
}

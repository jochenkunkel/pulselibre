package de.pulselibre

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceptionFreshnessTest {
    private val now = Instant.parse("2026-11-17T12:00:00Z")

    @Test fun mapsReceptionAgeToNonClinicalFreshnessStates() {
        assertEquals(ReceptionFreshness.UNKNOWN, ReceptionFreshness.of(null, now))
        assertEquals(ReceptionFreshness.UNKNOWN, ReceptionFreshness.of(now.plusSeconds(1), now))
        assertEquals(ReceptionFreshness.FRESH, ReceptionFreshness.of(now.minusSeconds(180), now))
        assertEquals(ReceptionFreshness.RECENT, ReceptionFreshness.of(now.minusSeconds(181), now))
        assertEquals(ReceptionFreshness.RECENT, ReceptionFreshness.of(now.minusSeconds(900), now))
        assertEquals(ReceptionFreshness.DELAYED, ReceptionFreshness.of(now.minusSeconds(901), now))
        assertEquals(ReceptionFreshness.DELAYED, ReceptionFreshness.of(now.minusSeconds(3600), now))
        assertEquals(ReceptionFreshness.OLD, ReceptionFreshness.of(now.minusSeconds(3601), now))
    }
}

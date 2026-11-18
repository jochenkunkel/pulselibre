package de.pulselibre

import org.junit.Assert.assertEquals
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class NightOxygenWindowTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test fun afterSixUsesTheNightThatJustEnded() {
        val (s, e) = NightOxygenReader.window(Instant.parse("2026-11-18T12:00:00Z"), berlin)
        assertEquals(Instant.parse("2026-11-17T21:00:00Z"), s) // 22:00 CET
        assertEquals(Instant.parse("2026-11-18T05:00:00Z"), e) // 06:00 CET
    }
    @Test fun beforeSixUsesThePreviousCompletedNight() {
        val (s, e) = NightOxygenReader.window(Instant.parse("2026-11-18T03:00:00Z"), berlin)
        assertEquals(Instant.parse("2026-11-16T21:00:00Z"), s)
        assertEquals(Instant.parse("2026-11-17T05:00:00Z"), e)
    }
    @Test fun springForwardNightIsSevenHoursLong() {
        // night 28→29 March 2026 loses one hour
        val (s, e) = NightOxygenReader.window(Instant.parse("2026-03-29T12:00:00Z"), berlin)
        assertEquals(Duration.ofHours(7), Duration.between(s, e))
    }
    @Test fun fallBackNightIsNineHoursLong() {
        val (s, e) = NightOxygenReader.window(Instant.parse("2026-10-25T12:00:00Z"), berlin)
        assertEquals(Duration.ofHours(9), Duration.between(s, e))
    }
}

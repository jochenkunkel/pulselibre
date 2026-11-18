package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DeviceFormatTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")
    @Test fun ageIsReadableAndNeverNegative() {
        assertEquals("vor 12 s", DeviceFormat.age(now.minusSeconds(12), now))
        assertEquals("vor 5 min", DeviceFormat.age(now.minusSeconds(300), now))
        assertEquals("vor 2 h 3 min", DeviceFormat.age(now.minusSeconds(2 * 3600 + 180), now))
        assertEquals("vor 3 Tagen", DeviceFormat.age(now.minusSeconds(3 * 86_400L), now))
        assertEquals("kein Empfang", DeviceFormat.age(null, now)); assertEquals("kein Empfang", DeviceFormat.age(now.plusSeconds(5), now))
    }
    @Test fun sizesAndCountsUseGermanFormat() {
        assertEquals("55,6 MB", DeviceFormat.bytes(58_372_096)); assertEquals("12 KB", DeviceFormat.bytes(12_500)); assertEquals("900 B", DeviceFormat.bytes(900))
        assertEquals("112.366", DeviceFormat.count(112_366))
    }
}

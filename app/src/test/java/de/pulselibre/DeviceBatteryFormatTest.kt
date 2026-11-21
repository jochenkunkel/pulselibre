package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBatteryFormatTest {
    private val now = 1_800_000_000_000L

    @Test fun freshValueShowsPercentAndAge() {
        val v = DeviceBatteryFormat.describe(GermanStrings, BatteryReading(37, null, now - 5 * 60_000), now)
        assertEquals("37 %", v.valueText)
        assertEquals("Stand: vor 5 min", v.freshness)
        assertFalse(v.stale)
        assertFalse("kein Ladestatus ohne belegten Statuscode", v.charging)
        assertTrue(v.description.startsWith("Akku: 37 Prozent"))
    }

    @Test fun oldValueIsMarkedAsLastKnownStateNotLive() {
        val v = DeviceBatteryFormat.describe(GermanStrings, BatteryReading(37, null, now - 30L * 60 * 60 * 1000), now)
        assertTrue(v.stale)
        assertTrue(v.freshness.startsWith("Veraltet · letzter bekannter Stand vor 30 h"))
    }

    @Test fun missingTimestampIsNeverPresentedAsLive() {
        val v = DeviceBatteryFormat.describe(GermanStrings, BatteryReading(80, null, null), now)
        assertTrue(v.stale)
        assertEquals("Zeitpunkt unbekannt · letzter bekannter Stand", v.freshness)
    }

    @Test fun onlyTheChargingCodeMakesAStatementAboutCharging() {
        assertTrue(DeviceBatteryFormat.describe(GermanStrings, BatteryReading(50, 6, now), now).charging)
        assertEquals("50 % · lädt", DeviceBatteryFormat.describe(GermanStrings, BatteryReading(50, 6, now), now).valueText)
        listOf(1, 2, 3, 4, 5, 7, null).forEach { assertFalse(DeviceBatteryFormat.describe(GermanStrings, BatteryReading(50, it, now), now).charging) }
    }

    @Test fun percentIsClampedAndFutureTimestampsAreTreatedAsUnknown() {
        assertEquals("100 %", DeviceBatteryFormat.describe(GermanStrings, BatteryReading(250, null, now), now).valueText)
        assertTrue(DeviceBatteryFormat.describe(GermanStrings, BatteryReading(50, null, now + 60_000), now).stale)
    }

    @Test fun levelColorsDifferByRangeInBothThemes() {
        for (dark in listOf(true, false)) {
            val low = DeviceBatteryFormat.levelColor(10, dark)
            val mid = DeviceBatteryFormat.levelColor(30, dark)
            val high = DeviceBatteryFormat.levelColor(80, dark)
            assertNotEquals(low, mid); assertNotEquals(mid, high); assertNotEquals(low, high)
        }
    }

    @Test fun ageTextUnits() {
        assertEquals("vor 45 s", DeviceBatteryFormat.ageText(GermanStrings, 45_000))
        assertEquals("vor 2 h", DeviceBatteryFormat.ageText(GermanStrings, 2 * 3600_000L))
        assertEquals("vor 3 Tagen", DeviceBatteryFormat.ageText(GermanStrings, 3 * 86400_000L))
    }
}

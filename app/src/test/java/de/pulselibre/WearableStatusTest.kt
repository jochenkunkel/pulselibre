package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #649: four evidenced states, real battery or "–", never colour alone, band battery named as such. */
class WearableStatusTest {
    private val now = 1_800_000_000_000L
    private val online = CirqaLinkStatus.Result(CirqaLinkStatus.Kind.ONLINE_RECEIVING, true, "empfängt Daten")
    private val offline = CirqaLinkStatus.Result(CirqaLinkStatus.Kind.RECORDER_OFF_DISCONNECTED, false, "nicht verbunden")
    private fun breath(value: Double, ageMs: Long = 10_000) = RespirationSample(now - ageMs, value)
    private fun battery(percent: Int, ageMs: Long? = 60_000) = BatteryReading(percent, null, ageMs?.let { now - it })

    @Test fun fourStatesFromEvidenceOnly() {
        assertEquals(WearState.WORN, WearableStatus.state(true, breath(14.0), now))
        assertEquals(WearState.NOT_WORN, WearableStatus.state(true, breath(-100.0), now))
        assertEquals(WearState.OFFLINE, WearableStatus.state(false, breath(14.0), now))
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, null, now))
    }

    @Test fun staleOrAmbiguousSignalsSayNothingAboutWearing() {
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, breath(14.0, ageMs = WearableStatus.FRESH_MS + 1), now))
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, breath(-200.0), now)) // large motion
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, breath(-300.0), now)) // invalid
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, breath(0.0), now))
        assertEquals(WearState.UNKNOWN, WearableStatus.state(true, RespirationSample(now + 3_600_000, 14.0), now)) // future stamp
    }

    @Test fun batteryIsRealOrDash() {
        assertEquals(62, WearableStatus.shownBattery(true, battery(62), now))
        assertNull(WearableStatus.shownBattery(false, battery(62), now))                      // offline: "–"
        assertNull(WearableStatus.shownBattery(true, null, now))                              // unknown
        assertNull(WearableStatus.shownBattery(true, battery(62, ageMs = null), now))         // value without time
        assertNull(WearableStatus.shownBattery(true, battery(62, ageMs = WearableStatus.BATTERY_MAX_AGE_MS + 1), now))
        assertNull(WearableStatus.shownBattery(true, battery(140), now))                      // implausible
    }

    @Test fun lowBatteryIsFlagged() {
        assertTrue(WearableStatus.isLow(15)); assertTrue(WearableStatus.isLow(3))
        assertFalse(WearableStatus.isLow(16)); assertFalse(WearableStatus.isLow(null))
    }

    @Test fun newestBatterySourceWins() {
        val live = battery(40, ageMs = 60_000); val fit = battery(90, ageMs = 86_400_000)
        assertEquals(40, WearableStatus.newestBattery(live, fit)!!.percent)
        assertEquals(90, WearableStatus.newestBattery(null, fit)!!.percent)
        assertNull(WearableStatus.newestBattery(null, null))
    }

    @Test fun talkBackNamesLinkWearStatusAndBandBattery() {
        val text = WearableStatus.description(GermanStrings, online, WearState.WORN, 62, battery(62, ageMs = 120_000), now)
        assertTrue(text, text.startsWith("Einstellungen öffnen. CIRQA-Armband: empfängt Daten."))
        assertTrue(text, text.contains("Tragestatus: getragen"))
        assertTrue(text, text.contains("Armband-Akku (nicht Telefon): 62 % (gemessen vor 2 min)"))
        val low = WearableStatus.description(GermanStrings, online, WearState.UNKNOWN, 9, battery(9), now)
        assertTrue(low, low.contains("9 % (gemessen vor 1 min), niedrig") && low.contains("kein aktuelles Tragesignal"))
        val none = WearableStatus.description(GermanStrings, offline, WearState.OFFLINE, null, null, now)
        assertTrue(none, none.contains("Armband nicht verbunden") && none.contains("keine aktuelle Angabe"))
    }
}

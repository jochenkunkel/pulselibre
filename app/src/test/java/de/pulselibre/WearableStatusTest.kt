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

    @Test fun lastKnownBatteryIsAlwaysShownButOnlyFreshValuesAreCurrent() {
        assertEquals(WearableStatus.BatteryShown(62, true, 60_000), WearableStatus.battery(true, battery(62), now))
        // Offline or old: still the number (like the device page), but marked "last known" (drawn grey).
        assertEquals(false, WearableStatus.battery(false, battery(62), now)!!.current)
        assertEquals(false, WearableStatus.battery(true, battery(62, ageMs = WearableStatus.BATTERY_MAX_AGE_MS + 1), now)!!.current)
        assertEquals(false, WearableStatus.battery(true, battery(62, ageMs = null), now)!!.current)
        assertNull(WearableStatus.battery(true, null, now))               // nothing known: "–"
        assertNull(WearableStatus.battery(true, battery(140), now))       // implausible
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

    @Test fun zoneLabelsNameLinkWearStatusAndBandBattery() {
        val status = WearableStatus.statusZoneDescription(GermanStrings, online, WearState.WORN)
        assertTrue(status, status.startsWith("CIRQA-Armband: empfängt Daten.") && status.contains("Tragestatus: getragen"))
        val fresh = WearableStatus.batteryZoneDescription(GermanStrings, WearableStatus.battery(true, battery(62, 120_000), now))
        assertTrue(fresh, fresh.contains("Armband-Akku (nicht Telefon): 62 % (gemessen vor 2 min)"))
        val low = WearableStatus.batteryText(GermanStrings, WearableStatus.battery(true, battery(9), now))
        assertTrue(low, low.contains("9 %") && low.endsWith(", niedrig"))
        val old = WearableStatus.batteryText(GermanStrings, WearableStatus.battery(false, battery(55, 3 * 3_600_000L), now))
        assertTrue(old, old.startsWith("zuletzt bekannt 55 %"))
        assertTrue(WearableStatus.batteryText(GermanStrings, null).contains("keine aktuelle Angabe"))
    }

    @Test fun batteryDetailsNameSourceAgeAndThatItIsTheBand() {
        val live = BatteryReading(48, null, now - 600_000, source = "GARMIN_GDI_STATUS")
        val text = WearableStatus.batteryDetails(GermanStrings, live, WearableStatus.battery(true, live, now))
        assertTrue(text, text.contains("48 % · gemessen vor 10 min · Quelle: Gerätestatus des Armbands"))
        assertTrue(text, text.contains("nicht der des Telefons"))
        val fit = BatteryReading(70, null, now - 2 * 86_400_000L)
        val old = WearableStatus.batteryDetails(GermanStrings, fit, WearableStatus.battery(true, fit, now))
        assertTrue(old, old.contains("FIT-Datei des Armbands") && old.contains("„zuletzt bekannt“"))
        assertTrue(WearableStatus.batteryDetails(GermanStrings, null, null).contains("noch kein Akkuwert"))
    }

    @Test fun statusDetailsExplainWhereConnectionAndWearStatusComeFrom() {
        val text = WearableStatus.statusDetails(GermanStrings, offline, WearState.OFFLINE)
        assertTrue(text, text.startsWith("Verbindung: nicht verbunden") && text.contains("Android das Armband als verbunden meldet"))
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Stored band heart rate must become visible, and normal all-day history must never be confused with activities. */
class BandHistoryDisplayTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 20), zone)
    private fun at(seconds: Long) = day.start.plusSeconds(seconds)

    @Test fun messageNumbersMapToTheirKind() {
        assertEquals(BandRecordKind.MONITORING, BandRecordKind.of(55))
        assertEquals(BandRecordKind.ACTIVITY, BandRecordKind.of(20))
        assertEquals(BandRecordKind.OTHER, BandRecordKind.of(18))
        assertEquals(BandRecordKind.OTHER, BandRecordKind.of(null))
    }

    @Test fun noteNamesValuesCountedOnceAcrossFiles() {
        val note = FitKindText.note(GermanStrings, mapOf(BandRecordKind.MONITORING to 10), duplicates = 3)!!
        assertTrue(note, note.contains("3 Werte waren in mehreren FIT-Dateien enthalten und zählen einmal."))
        assertFalse(FitKindText.note(GermanStrings, mapOf(BandRecordKind.MONITORING to 10))!!.contains("mehreren FIT-Dateien"))
    }

    @Test fun noteSaysHowMuchIsNormalHistoryAndHowMuchActivity() {
        assertNull(FitKindText.note(GermanStrings, emptyMap()))
        val both = FitKindText.note(GermanStrings, mapOf(BandRecordKind.MONITORING to 120, BandRecordKind.ACTIVITY to 30))!!
        assertTrue(both, both.contains("normale Tageshistorie: 120") && both.contains("Aktivitätsaufzeichnungen: 30"))
        assertFalse(both.contains("Nur Werte aus Aktivitätsaufzeichnungen"))
        assertFalse(both.contains("sonstige"))
    }

    @Test fun activityOnlyIsNeverPresentedAsAllDayHistory() {
        val note = FitKindText.note(GermanStrings, mapOf(BandRecordKind.ACTIVITY to 300))!!
        assertTrue(note, note.contains("normale Tageshistorie: 0"))
        assertTrue(note, note.contains("keine normale Tageshistorie gespeichert"))
    }

    @Test fun otherMessagesAreCountedSeparately() {
        val note = FitKindText.note(GermanStrings, mapOf(BandRecordKind.MONITORING to 5, BandRecordKind.OTHER to 2))!!
        assertTrue(note, note.contains("sonstige FIT-Nachrichten: 2"))
    }

    @Test fun bandDayKeepsExtremaAndLatestPerMinuteAndDropsOtherDays() {
        val points = listOf(
            HistoryPoint(at(10), 70), HistoryPoint(at(20), 95), HistoryPoint(at(30), 60), HistoryPoint(at(40), 72), // minute 0
            HistoryPoint(at(70), 80), // minute 1
            HistoryPoint(day.start.minusSeconds(5), 99), HistoryPoint(day.end, 99) // outside the local day
        )
        val reduced = DayBandBuckets.reduce(points, day)
        assertEquals(listOf(95L, 60L, 72L, 80L), reduced.map { it.value })
        assertTrue(reduced.all { it.time >= day.start && it.time < day.end })
    }

    @Test fun oneHertzActivityDayStaysBounded() {
        val points = (0 until 86_400L step 1).map { HistoryPoint(at(it), (60 + it % 40).toInt()) }
        val reduced = DayBandBuckets.reduce(points, day)
        assertTrue("${reduced.size} points", reduced.size <= 3 * 1440)
        assertEquals(points.maxOf { it.value }.toLong(), reduced.maxOf { it.value })
        assertEquals(points.minOf { it.value }.toLong(), reduced.minOf { it.value })
    }

    @Test fun dstDayIsHandledInLocalTime() {
        val dst = DashboardDay(LocalDate.of(2026, 10, 25), zone) // 25 hours
        val late = HistoryPoint(dst.end.minusSeconds(30), 66)
        assertEquals(listOf(66L), DayBandBuckets.reduce(listOf(late), dst).map { it.value })
        assertEquals(Instant.parse("2026-10-25T23:00:00Z"), dst.end)
    }
}

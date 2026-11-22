package de.pulselibre

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTableTest {
    private val utc = ZoneId.of("UTC")
    private fun at(s: String) = Instant.parse(s)
    private val start = at("2026-11-01T00:00:00Z")
    private val end = at("2026-11-30T00:00:00Z")
    private fun snap(band: List<HistoryPoint> = emptyList(), phone: List<HistoryPoint> = emptyList(),
                     spo2: List<HistoryPoint> = emptyList()) =
        HistorySnapshot(HistoryRange.MONTH, start, end, phone, spo2, band, band.isNotEmpty())

    @Test fun bandAndPhoneStaySeparateRowsOnTheSameDay() {
        val rows = HistoryTable.rows(snap(
            band = listOf(HistoryPoint(at("2026-11-19T10:00:00Z"), 60), HistoryPoint(at("2026-11-19T11:00:00Z"), 100)),
            phone = listOf(HistoryPoint(at("2026-11-19T12:00:00Z"), 80))), utc)
        assertEquals(2, rows.size)
        assertEquals(HistorySource.BAND, rows[0].source)
        assertEquals(2, rows[0].count); assertEquals(60, rows[0].minimum); assertEquals(100, rows[0].maximum)
        assertEquals(80.0, rows[0].median, 0.0); assertEquals(80.0, rows[0].mean, 0.0)
        assertEquals(HistorySource.PHONE, rows[1].source); assertEquals(1, rows[1].count)
    }

    @Test fun newestDayFirstAndEmptyDaysHaveNoRow() {
        val rows = HistoryTable.rows(snap(band = listOf(
            HistoryPoint(at("2026-11-15T10:00:00Z"), 70), HistoryPoint(at("2026-11-19T10:00:00Z"), 90))), utc)
        assertEquals(listOf(19, 15), rows.map { it.day.dayOfMonth })
    }

    @Test fun valuesOutsideTheWindowAreIgnored() {
        val rows = HistoryTable.rows(snap(phone = listOf(HistoryPoint(at("2026-10-31T23:59:00Z"), 70))), utc)
        assertTrue(rows.isEmpty())
    }

    @Test fun daysFollowTheGivenZone() {
        val berlin = ZoneId.of("Europe/Berlin")
        val rows = HistoryTable.rows(snap(phone = listOf(HistoryPoint(at("2026-11-19T23:30:00Z"), 70))), berlin)
        assertEquals(20, rows.single().day.dayOfMonth)
    }

    @Test fun medianOfEvenCountIsTheMeanOfTheMiddlePair() {
        val rows = HistoryTable.rows(snap(spo2 = listOf(95, 96, 98, 100).mapIndexed { i, v ->
            HistoryPoint(at("2026-11-19T0$i:00:00Z"), v) }), utc)
        assertEquals(HistoryMetric.SPO2, rows.single().metric)
        assertEquals(97.0, rows.single().median, 0.0)
    }
}

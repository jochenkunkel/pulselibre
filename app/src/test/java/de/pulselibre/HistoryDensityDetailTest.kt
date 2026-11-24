package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class HistoryDensityDetailTest {
    private val start = Instant.parse("2026-11-19T00:00:00Z")
    private val end = start.plus(Duration.ofHours(4))
    private val axis = AxisScale(50, 110)
    private val zone = ZoneId.of("UTC")
    private fun pts(vararg v: Pair<Int, Int>) = v.map { (minute, value) -> HistoryPoint(start.plusSeconds(minute * 60L), value) }
    private fun density(points: List<HistoryPoint>) = aggregateHistoryDensity(points, start, end, HistoryResolution.ONE_HOUR, axis, 10)

    @Test fun bimodalWindowReportsTheMostFrequentIntervalIndependentOfTheMean() {
        // 10 values near 60 and 6 values near 100 within the first hour -> mean ~75 lies between the modes, peak bin is 60..70.
        val points = pts(*((0 until 10).map { it to 62 } + (10 until 16).map { it to 101 }).toTypedArray())
        val col = density(points).columns[0]
        val text = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false)
        assertTrue(text, text.contains("Stichprobe: 16 Werte"))
        assertTrue(text, text.contains("Häufigstes Intervall: 60–<70 bpm (63 % der 16 Werte"))
        assertTrue(text, text.contains("Beobachtet: 62–101 bpm"))
        assertTrue(text, text.contains("Mittelwert 76,6 bpm"))   // between the modes, far from the peak interval
    }

    @Test fun medianAndMeanComeFromTheRawPointsOfExactlyThatWindow() {
        val points = pts(5 to 60, 10 to 70, 15 to 80, 70 to 200)        // the 200 belongs to the second window
        val col = density(points).columns[0]
        val text = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false)
        assertTrue(text.contains("Stichprobe: 3 Werte"))
        assertTrue(text.contains("Median 70 bpm · Mittelwert 70 bpm"))
    }

    @Test fun emptyWindowIsAGapAndNothingIsInterpolated() {
        val points = pts(5 to 60)
        val col = density(points).columns[2]
        val text = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false)
        assertTrue(text.contains("Keine Messwerte in diesem Zeitfenster"))
        assertTrue(text.contains("nichts interpoliert"))
    }

    @Test fun valuesOutsideTheAxisAreReportedNotColoured() {
        val points = pts(5 to 60, 6 to 250)
        val col = density(points).columns[0]
        val text = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false)
        assertEquals(1, col.excludedOutsideAxis)
        assertTrue(text.contains("1 Werte außerhalb der gezeigten Achse nicht eingefärbt"))
    }

    @Test fun legendExplainsColourAndThatItIsRelativeNotAbsolute() {
        assertTrue(HistoryDensityDetail.legend(GermanStrings, false).contains("relative Häufigkeit"))
        assertTrue(HistoryDensityDetail.legend(GermanStrings, false).contains("nicht die Anzahl"))
        assertTrue(HistoryDensityDetail.legend(GermanStrings, false).contains("Leer = keine Messwerte"))
    }

    @Test fun numberAndDateFormatFollowTheAppLanguage() {
        val points = pts(5 to 60, 10 to 70, 15 to 81)
        val col = density(points).columns[0]
        val de = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false, locale = java.util.Locale.GERMANY)
        val en = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false, locale = java.util.Locale.ENGLISH)
        val fr = HistoryDensityDetail.describe(GermanStrings, col, points, "bpm", zone, isLast = false, locale = java.util.Locale.FRENCH)
        assertTrue(de, de.contains("Mittelwert 70,3 bpm") && de.contains("19.11. 00:00"))
        assertTrue(en, en.contains("70.3") && en.contains("11/19 00:00"))
        assertTrue(fr, fr.contains("70,3") && fr.contains("19/11 00:00"))
    }
}

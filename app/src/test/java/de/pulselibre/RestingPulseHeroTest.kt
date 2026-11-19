package de.pulselibre

import de.pulselibre.RestingPulseHero.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RestingPulseHeroTest {
    private val today = LocalDate.of(2026, 11, 19)
    private fun hero(values: Map<LocalDate, Double>, days: Int) =
        RestingPulseHero.build(values, today, days, TrendAnalysis.analyze(values, today, days, 1.0))
    /** value for offsets 1..n (days before today) from [f]; null skips the day. */
    private fun series(n: Int, f: (Int) -> Double?) = (1..n).mapNotNull { o -> f(o)?.let { today.minusDays(o.toLong()) to it } }.toMap()

    @Test fun deltaIsNeutralAndNamesThePreviousPeriod() {
        // current 28 days median 72, previous 28 days median 76 -> "−4 bpm gegenüber vorherigen 28 Tagen"
        val v = series(56) { o -> if (o <= 28) 72.0 else 76.0 }
        val m = hero(v, 28)
        assertEquals(State.OK, m.state)
        assertEquals("72 bpm", m.valueText)
        assertEquals("−4 bpm gegenüber vorherigen 28 Tagen", m.deltaText)
        assertFalse((m.deltaText + m.message).lowercase().contains("fitter"))
        assertEquals(76.0, m.baselineBpm!!, 0.0001)
    }

    @Test fun tenValidDaysOfTwentyEightAreInsufficientAndShowPointsWithoutDelta() {
        val v = series(28) { o -> if (o % 3 == 1 && o <= 28) 70.0 + o else null }   // 10 days
        val m = hero(v, 28)
        assertEquals(10, v.size)
        assertEquals(State.INSUFFICIENT, m.state)
        assertEquals(10, m.points.size)
        assertNull(m.valueText); assertNull(m.deltaText)
        assertTrue(m.message.contains("10 von mindestens 14"))
    }

    @Test fun missingDayBetweenValidDaysBreaksTheLine() {
        val v = series(7) { o -> if (o == 3) null else 70.0 + o }
        val m = hero(v, 7)
        assertEquals(6, m.points.size)
        // two runs: days before and after the gap; no segment spans the gap
        assertEquals(2, m.segments.size)
        m.segments.forEach { seg -> seg.zipWithNext().forEach { (a, b) -> assertEquals(a.dayIndex + 1, b.dayIndex) } }
    }

    @Test fun noDataShowsNoFictionalCurve() {
        val m = hero(emptyMap(), 28)
        assertEquals(State.NO_DATA, m.state)
        assertTrue(m.points.isEmpty() && m.segments.isEmpty())
        assertEquals("Noch keine verifizierten Ruhepulswerte", m.message.substringBefore("."))
        assertTrue(m.message.contains("kein Ersatz durch Live-Puls"))
    }

    @Test fun baselineWithTooFewDaysSuppressesTheComparison() {
        val v = series(56) { o -> if (o <= 28) 72.0 else if (o <= 35) 76.0 else null }   // previous period: 7 of 28 days
        val m = hero(v, 28)
        assertEquals(State.OK, m.state)
        assertNull(m.deltaText); assertNull(m.baselineBpm)
        assertTrue(m.message.contains("Vergleichszeitraum hat zu wenige Daten"))
    }

    @Test fun zeroChangeAndSmallChangesAreWordedNeutrally() {
        val flat = hero(series(56) { 70.0 }, 28)
        assertEquals("±0 bpm gegenüber vorherigen 28 Tagen", flat.deltaText)
        val up = hero(series(14) { o -> if (o <= 7) 73.5 else 72.0 }, 7)
        assertEquals("+1,5 bpm gegenüber vorherigen 7 Tagen", up.deltaText)
    }

    @Test fun descriptionContainsWindowValueAndDelta() {
        val v = series(14) { o -> if (o <= 7) 72.0 else 76.0 }
        val d = hero(v, 7).description()
        assertTrue(d.contains("über 7 Tage") && d.contains("72 bpm") && d.contains("−4 bpm"))
    }
}

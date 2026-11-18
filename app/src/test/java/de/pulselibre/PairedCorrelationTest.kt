package de.pulselibre

import de.pulselibre.PairedCorrelation.Verdict
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.Random

class PairedCorrelationTest {
    private val start = LocalDate.of(2026, 1, 1)
    private fun days(n: Int, f: (Int) -> Double?) =
        (0 until n).mapNotNull { i -> f(i)?.let { start.plusDays(i.toLong()) to it } }.toMap()

    @Test fun tooFewPairsIsNotComputable() {
        val x = days(10) { it.toDouble() }
        val r = PairedCorrelation.analyze(x, x, 0)
        assertEquals(Verdict.NOT_COMPUTABLE, r.verdict); assertNull(r.rho); assertEquals(10, r.pairs)
    }
    @Test fun independentNoiseShowsNoReliableAssociation() {
        val rnd = Random(7)
        val x = days(120) { rnd.nextGaussian() }
        val y = days(120) { rnd.nextGaussian() }
        val r = PairedCorrelation.analyze(x, y, 0)
        assertEquals(Verdict.NO_RELIABLE_ASSOCIATION, r.verdict)
        assertTrue(r.ciLow!! < 0 && r.ciHigh!! > 0)
    }
    @Test fun strongRelationIsExploratoryAssociationWithInterval() {
        val rnd = Random(3)
        val x = days(120) { rnd.nextGaussian() }
        val y = days(120) { x[start.plusDays(it.toLong())]!! * 0.8 + rnd.nextGaussian() * 0.3 }
        val r = PairedCorrelation.analyze(x, y, 0)
        assertEquals(Verdict.EXPLORATORY_ASSOCIATION, r.verdict)
        assertTrue(r.rho!! > 0.8); assertTrue(r.ciLow!! > 0.6 && r.ciLow!! < r.rho!!)
    }
    @Test fun lagOneFindsNextDayRelationAndLagZeroDoesNot() {
        val rnd = Random(11)
        val x = days(150) { rnd.nextGaussian() }
        val y = days(150) { i -> if (i == 0) rnd.nextGaussian() else x[start.plusDays(i - 1L)]!! + rnd.nextGaussian() * 0.3 }
        assertEquals(Verdict.EXPLORATORY_ASSOCIATION, PairedCorrelation.analyze(x, y, 1).verdict)
        assertEquals(Verdict.NO_RELIABLE_ASSOCIATION, PairedCorrelation.analyze(x, y, 0).verdict)
    }
    @Test fun gapsReducePairsAndOnlyOverlappingDaysCount() {
        val x = days(100) { if (it % 2 == 0) it.toDouble() else null }
        val y = days(100) { if (it % 3 == 0) it.toDouble() else null }
        assertEquals(17, PairedCorrelation.pair(x, y, 0).size) // multiples of 6 below 100
    }
    @Test fun constantSeriesIsNotComputable() {
        val x = days(60) { 5.0 }; val y = days(60) { it.toDouble() }
        assertEquals(Verdict.NOT_COMPUTABLE, PairedCorrelation.analyze(x, y, 0).verdict)
    }
    @Test fun pureTrendsAreNotComputableBecauseDaysAreNotIndependent() {
        val x = days(60) { it.toDouble() }; val y = days(60) { it * 2.0 }
        val r = PairedCorrelation.analyze(x, y, 0)
        assertEquals(Verdict.NOT_COMPUTABLE, r.verdict); assertTrue(r.effectiveN!! < 3.0)
    }
    @Test fun ranksAverageTies() {
        assertEquals(listOf(1.5, 1.5, 3.0), PairedCorrelation.ranks(listOf(1.0, 1.0, 2.0)))
    }
    @Test fun benjaminiHochbergMatchesKnownValues() {
        val adj = PairedCorrelation.benjaminiHochberg(listOf(0.01, 0.04, 0.03, 0.005))
        assertEquals(listOf(0.02, 0.04, 0.04, 0.02), adj.map { Math.round(it * 1000) / 1000.0 })
    }
}

package de.pulselibre

import de.pulselibre.PairedCorrelation.Verdict
import de.pulselibre.RelationshipHypotheses.Id
import de.pulselibre.RelationshipHypotheses.Missing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Random

/** #45: pre-specified hypotheses, lag-0 sensitivity, multiple-testing correction, honest "not computable". */
class RelationshipHypothesesTest {
    private val start = LocalDate.of(2026, 1, 1)
    private fun days(n: Int, f: (Int) -> Double?) =
        (0 until n).mapNotNull { i -> f(i)?.let { start.plusDays(i.toLong()) to it } }.toMap()

    @Test fun normalCdfMatchesKnownValues() {
        assertEquals(0.5, PairedCorrelation.normalCdf(0.0), 1e-7)
        assertEquals(0.975002, PairedCorrelation.normalCdf(1.959964), 1e-5)
        assertEquals(0.024998, PairedCorrelation.normalCdf(-1.959964), 1e-5)
    }

    @Test fun pValueAgreesWithTheInterval() {
        val rnd = Random(3)
        val x = days(120) { rnd.nextGaussian() }
        val strong = PairedCorrelation.analyze(x, days(120) { x[start.plusDays(it.toLong())]!! * 0.8 + rnd.nextGaussian() * 0.3 }, 0)
        assertTrue(strong.pValue!! < 0.001)
        val noise = PairedCorrelation.analyze(days(120) { rnd.nextGaussian() }, days(120) { rnd.nextGaussian() }, 0)
        // Interval contains 0 <=> p >= 0.05 (same Fisher z and effective n).
        assertTrue(noise.ciLow!! < 0 && noise.ciHigh!! > 0); assertTrue(noise.pValue!! >= 0.05)
    }

    @Test fun allThreeHypothesesAreAlwaysListedAndMissingDataIsNamed() {
        val rnd = Random(1)
        val out = RelationshipHypotheses.evaluate(days(60) { rnd.nextGaussian() }, days(60) { rnd.nextGaussian() })
        assertEquals(listOf(Id.STEPS_REST_NEXT_DAY, Id.SLEEP_REST_NEXT_DAY, Id.SLEEP_HRV), out.map { it.id })
        assertEquals(Missing.SLEEP_NOT_DECODED, out[1].missing); assertEquals(Verdict.NOT_COMPUTABLE, out[1].verdict)
        assertEquals(Missing.SLEEP_NOT_DECODED, out[2].missing)
        val withSleep = RelationshipHypotheses.evaluate(days(60) { 1.0 * it }, days(60) { 1.0 * it }, sleepMinutes = days(60) { 400.0 + it })
        assertEquals(Missing.HRV_NOT_DECODED, withSleep[2].missing)
    }

    @Test fun nextDayEffectIsFoundAndLagZeroIsOnlyASensitivity() {
        val rnd = Random(11)
        val steps = days(150) { rnd.nextGaussian() }
        val rest = days(150) { i -> if (i == 0) rnd.nextGaussian() else steps[start.plusDays(i - 1L)]!! + rnd.nextGaussian() * 0.3 }
        val h = RelationshipHypotheses.evaluate(steps, rest).first()
        assertEquals(1, h.primaryLag)
        assertEquals(Verdict.EXPLORATORY_ASSOCIATION, h.verdict)
        assertNotNull(h.lagZero); assertEquals(0, h.lagZero!!.lagDays)
        assertTrue("lag 0 must not carry the effect", kotlin.math.abs(h.lagZero!!.rho!!) < 0.3)
        // One computed test: the adjusted p equals the raw p.
        assertEquals(h.primary!!.pValue!!, h.adjustedP!!, 1e-12)
    }

    @Test fun independentSeriesNeverBecomeAnAssociation() {
        repeat(20) { seed ->
            val rnd = Random(100L + seed)
            val h = RelationshipHypotheses.evaluate(days(120) { rnd.nextGaussian() }, days(120) { rnd.nextGaussian() },
                sleepMinutes = days(120) { rnd.nextGaussian() }, hrv = days(120) { rnd.nextGaussian() })
            // With three corrected tests a chance hit at p ~ 0.03 is not enough.
            h.filter { it.verdict == Verdict.EXPLORATORY_ASSOCIATION }.forEach { assertTrue(it.adjustedP!! < RelationshipHypotheses.ALPHA) }
        }
    }

    @Test fun correctionDowngradesAWeakSingleHitAmongSeveralTests() {
        // Search a seed where H1 alone is nominally significant (p < 0.05) but not after correcting three tests.
        var checked = false
        for (seed in 0 until 400) {
            val rnd = Random(seed.toLong())
            val steps = days(90) { rnd.nextGaussian() }
            val rest = days(90) { i -> if (i == 0) 0.0 else steps[start.plusDays(i - 1L)]!! * 0.22 + rnd.nextGaussian() }
            val out = RelationshipHypotheses.evaluate(steps, rest, sleepMinutes = days(90) { rnd.nextGaussian() }, hrv = days(90) { rnd.nextGaussian() })
            val h1 = out.first()
            if (h1.primary!!.verdict == Verdict.EXPLORATORY_ASSOCIATION && h1.adjustedP!! >= RelationshipHypotheses.ALPHA) {
                assertEquals(Verdict.NO_RELIABLE_ASSOCIATION, h1.verdict)
                checked = true; break
            }
        }
        assertTrue("no seed exercised the correction", checked)
    }

    @Test fun gappedDataBelowTheMinimumIsNotComputable() {
        val sparse = days(100) { if (it % 5 == 0) it.toDouble() else null } // 20 days < 30 pairs
        val h = RelationshipHypotheses.evaluate(sparse, sparse).first()
        assertEquals(Verdict.NOT_COMPUTABLE, h.verdict); assertNull(h.adjustedP); assertEquals(Missing.NONE, h.missing)
    }
}

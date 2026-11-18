package de.pulselibre

import de.pulselibre.NocturnalOxygen.Sample
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant

class NocturnalOxygenTest {
    private val start = Instant.parse("2026-11-17T22:00:00Z")
    private val end = start.plus(Duration.ofHours(8))
    private fun series(stepSec: Long, f: (Long) -> Int?): List<Sample> =
        generateSequence(0L) { it + stepSec }.takeWhile { it < 8 * 3600 }.mapNotNull { t -> f(t)?.let { Sample(start.plusSeconds(t), it) } }.toList()

    @Test fun shortWindowAndNoDataAreNotComputable() {
        assertFalse(NocturnalOxygen.summarize(emptyList(), start, start.plusSeconds(3600)).computable)
        assertFalse(NocturnalOxygen.summarize(emptyList(), start, end).computable)
    }
    @Test fun lowCoverageIsWithheldWithoutAnyFigures() {
        val s = series(10) { if (it < 2 * 3600) 96 else null } // 25 % coverage
        val r = NocturnalOxygen.summarize(s, start, end)
        assertFalse(r.computable); assertNull(r.minutesBelow90); assertNull(r.odi3)
    }
    @Test fun denseStableNightHasZeroEventsAndTimeBelow() {
        val r = NocturnalOxygen.summarize(series(10) { 96 }, start, end)
        assertTrue(r.computable); assertEquals(0.0, r.minutesBelow90!!, 1e-9); assertEquals(0.0, r.odi3!!, 1e-9)
        assertTrue(r.coveragePercent!! > 99)
    }
    @Test fun dipsAreCountedAsOdi3AndOdi4() {
        // two dips of 5 points (96 -> 91) lasting 60 s, separated by long normal phases
        val s = series(10) { t -> if (t in 3600..3659 || t in 7200..7259) 91 else 96 }
        val r = NocturnalOxygen.summarize(s, start, end)
        assertEquals(2.0 / 8.0, r.odi3!!, 1e-9); assertEquals(2.0 / 8.0, r.odi4!!, 1e-9)
        assertEquals(0.0, r.minutesBelow90!!, 1e-9)
    }
    @Test fun smallDipOfTwoPointsIsNoEvent() {
        val s = series(10) { t -> if (t in 3600..3659) 94 else 96 }
        assertEquals(0.0, NocturnalOxygen.summarize(s, start, end).odi3!!, 1e-9)
    }
    @Test fun timeBelowNinetyIsMeasuredFromHeldDuration() {
        val s = series(10) { t -> if (t in 1000..1599) 88 else 96 } // 60 samples x 10 s = 10 min
        assertEquals(10.0, NocturnalOxygen.summarize(s, start, end).minutesBelow90!!, 1e-9)
    }
    @Test fun sparseSamplingGivesTimeBelowButNoOdi() {
        val r = NocturnalOxygen.summarize(series(60) { 96 }, start, end)
        assertTrue(r.computable); assertNull(r.odi3); assertNull(r.odi4); assertTrue(r.reason.contains("ODI"))
    }
    @Test fun invalidReadingsAreIgnored() {
        val s = series(10) { t -> if (t % 100 == 0L) 0 else if (t % 70 == 0L) 255 else 96 }
        val r = NocturnalOxygen.summarize(s, start, end)
        assertTrue(r.computable); assertEquals(0.0, r.minutesBelow90!!, 1e-9)
    }
    @Test fun singleSampleDipsAreArtifactsNotEvents() {
        // 10 s sampling: a one-sample drop of 6 points every 10 minutes must not count
        val s = series(10) { t -> if (t % 600 == 0L && t > 0) 90 else 96 }
        assertEquals(0.0, NocturnalOxygen.summarize(s, start, end).odi3!!, 1e-9)
    }
    @Test fun dipShorterThanTenSecondsIsIgnoredAndLongerCounts() {
        fun withDip(durationSec: Int) = series(1) { t -> if (t in 3600 until 3600 + durationSec) 90 else 96 }
        assertEquals(0.0, NocturnalOxygen.summarize(withDip(8), start, end).odi3!!, 1e-9)
        assertEquals(1.0 / 8.0, NocturnalOxygen.summarize(withDip(12), start, end).odi3!!, 1e-9)
    }
}

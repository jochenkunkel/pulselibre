package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Random

/** The look-back scan of [NocturnalOxygen.countEvents] must give exactly the result of the plain definition, and stay fast on dense nights. */
class NocturnalOxygenEventCountTest {
    private val t0 = Instant.parse("2026-11-20T22:00:00Z")

    private fun series(n: Int, stepSeconds: Long, seed: Long): List<NocturnalOxygen.Sample> {
        val r = Random(seed)
        var v = 96
        return (0 until n).map { i ->
            if (r.nextInt(40) == 0) v = 88 + r.nextInt(10) else v = (v + r.nextInt(3) - 1).coerceIn(85, 99)
            NocturnalOxygen.Sample(t0.plusSeconds(i * stepSeconds), v)
        }
    }

    /** The definition of the baseline: maximum of all earlier readings within the look-back, else the reading itself. */
    private fun naiveCount(s: List<NocturnalOxygen.Sample>, drop: Int): Int {
        var events = 0; var armed = true; var dipStart: Instant? = null; var dipCounted = false
        for (i in s.indices) {
            val from = s[i].time - NocturnalOxygen.BASELINE_LOOKBACK
            val baseline = s.subList(0, i).filter { it.time >= from }.maxOfOrNull { it.percent } ?: s[i].percent
            if (baseline - s[i].percent >= drop) {
                if (dipStart == null) { dipStart = s[i].time; dipCounted = false }
                if (armed && !dipCounted && java.time.Duration.between(dipStart, s[i].time) >= NocturnalOxygen.MIN_EVENT_DURATION) { events++; dipCounted = true; armed = false }
            } else { dipStart = null; if (!armed && baseline - s[i].percent <= 1) armed = true }
        }
        return events
    }

    @Test fun sameResultAsThePlainDefinition() {
        for (seed in 1L..6L) for (step in listOf(1L, 5L, 30L)) {
            val s = series(1500, step, seed)
            for (drop in listOf(3, 4)) assertEquals("seed=$seed step=$step drop=$drop", naiveCount(s, drop), NocturnalOxygen.countEvents(s, drop))
        }
    }

    @Test fun denseEightHourNightIsFast() {
        val s = series(28_800, 1L, 42L)
        val start = System.nanoTime()
        NocturnalOxygen.countEvents(s, 3); NocturnalOxygen.countEvents(s, 4)
        assertTrue("took ${(System.nanoTime() - start) / 1_000_000} ms", (System.nanoTime() - start) / 1_000_000 < 2_000)
    }
}

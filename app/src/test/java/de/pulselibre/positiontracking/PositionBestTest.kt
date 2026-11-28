package de.pulselibre.positiontracking

import org.junit.Assert.assertSame
import org.junit.Test

class PositionBestTest {
    private fun p(t: Long, acc: Float?) = PositionPoint(t, 1.0, 2.0, acc, "x")

    @Test fun mostAccurateWins() {
        val gps = p(1, 4f); val net = p(2, 30f)
        assertSame(gps, PositionBest.pick(gps, net))
        assertSame(gps, PositionBest.pick(net, gps))
    }

    @Test fun knownAccuracyBeatsUnknownAndTiesKeepNewer() {
        val known = p(1, 50f); val unknown = p(2, null)
        assertSame(known, PositionBest.pick(known, unknown))
        assertSame(known, PositionBest.pick(unknown, known))
        val newer = p(3, 50f)
        assertSame(newer, PositionBest.pick(known, newer))
        assertSame(newer, PositionBest.pick(null, newer))
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class AxisScaleTest {
    private fun fits(s: AxisScale, lo: Int, hi: Int) { assertTrue("$s must contain $lo..$hi", s.minimum <= lo && s.maximum >= hi) }

    @Test fun spo2NearTopUsesTightRangeInsteadOfZeroToHundred() {
        val s = AxisScale.forData(89, 100, 0, 100, 10)
        fits(s, 89, 100); assertEquals(100, s.maximum); assertTrue(s.span <= 20)
        assertEquals(0, s.span % 4)
    }
    @Test fun pulseRangeIsNotStretchedToZeroTo240() {
        val s = AxisScale.forData(62, 138, 0, 240, 40)
        fits(s, 62, 138); assertTrue(s.span <= 100)
    }
    @Test fun singleValueStillGetsMinimumSpan() {
        val s = AxisScale.forData(97, 97, 0, 100, 10)
        fits(s, 97, 97); assertTrue(s.span >= 10)
    }
    @Test fun clampsToHardLimitsAndKeepsFourEqualIntervals() {
        for ((a, b) in listOf(0 to 3, 237 to 240, 1 to 240, 50 to 51, 99 to 100)) {
            val s = AxisScale.forData(a, b, 0, 240, 40)
            fits(s, a, b); assertTrue(s.minimum >= 0 && s.maximum <= 240); assertEquals(0, s.span % 4)
        }
    }
    @Test fun wideDataKeepsWholeRange() {
        val s = AxisScale.forData(40, 200, 0, 240, 40)
        fits(s, 40, 200)
    }
    @Test fun valuesOutsideHardLimitsAreClampedNotTrusted() {
        val s = AxisScale.forData(-5, 500, 0, 240, 40)
        assertEquals(0, s.minimum); assertEquals(240, s.maximum)
    }
}

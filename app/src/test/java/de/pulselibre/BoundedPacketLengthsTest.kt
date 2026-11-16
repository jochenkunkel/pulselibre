package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class BoundedPacketLengthsTest {
    @Test fun countsRepeatedLengthsAndSortsOutput() {
        val histogram = BoundedPacketLengths(2)
        histogram.record(7)
        histogram.record(2)
        histogram.record(7)
        assertEquals("2:1, 7:2", histogram.summary())
    }

    @Test fun capsDistinctLengthsAndCountsOverflow() {
        val histogram = BoundedPacketLengths(2)
        histogram.record(1)
        histogram.record(2)
        histogram.record(3)
        histogram.record(4)
        histogram.record(1)
        assertEquals("1:2, 2:1 (weitere Pakete: 2)", histogram.summary())
    }

    @Test fun resetClearsBothBucketsAndOverflow() {
        val histogram = BoundedPacketLengths(1)
        histogram.record(1)
        histogram.record(2)
        histogram.clear()
        histogram.record(3)
        assertEquals("3:1", histogram.summary())
    }
}

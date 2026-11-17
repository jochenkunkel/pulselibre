package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaCompactSessionObserverTest {
    private fun frame(seq: Int, counter: Int) =
        GarminCompactTransport.frame(0x39, seq, counter, byteArrayOf(0x01))

    @Test fun readsValidatedRxIds() {
        val observer = CirqaCompactSessionObserver()
        assertEquals(CirqaCompactSessionObserver.Observation(0x39, 10, 495),
            observer.observe(frame(10, 495)))
        assertEquals(1, observer.received)
        assertTrue(observer.summary().contains("TX-Session-Zähler=UNBEKANNT"))
    }

    @Test fun rejectsCorruptedFrame() {
        val observer = CirqaCompactSessionObserver()
        val corrupted = frame(10, 1)
        corrupted[4] = (corrupted[4].toInt() xor 1).toByte()
        assertNull(observer.observe(corrupted))
        assertEquals(0, observer.received)
    }

    @Test fun observesWraparound() {
        val observer = CirqaCompactSessionObserver()
        observer.observe(frame(127, 65535))
        observer.observe(frame(1, 0))
        assertEquals(0, observer.discontinuities)
    }

    @Test fun countsDuplicateAndGap() {
        val observer = CirqaCompactSessionObserver()
        observer.observe(frame(10, 1))
        observer.observe(frame(10, 1))
        observer.observe(frame(13, 4))
        assertEquals(1, observer.duplicates)
        assertEquals(1, observer.discontinuities)
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminCompactTransportTest {
    @Test fun compactFrameHasValidEnvelopeAndCrc() {
        val payload = byteArrayOf(0xDA.toByte(), 2, 2, 0x4A, 0)
        val frame = GarminCompactTransport.frame(0x39, 10, 1, payload)
        assertEquals(13, frame.size)
        assertEquals(0x39, frame[2].toInt() and 255)
        assertEquals(0x8A, frame[3].toInt() and 255)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertArrayEquals(GarminFileListRequest.gfdi(), frame)
    }

    @Test fun sequenceAndCounterWrapIndependently() {
        val ids = GarminCompactTransport.Ids(127, 65535)
        assertEquals(127 to 65535, ids.next())
        assertEquals(1 to 0, ids.next())
        assertEquals(2 to 1, ids.next())
    }

    @Test fun invalidIdentifiersAreRejected() {
        try {
            GarminCompactTransport.frame(0x39, 0, 1, byteArrayOf())
            fail("sequence zero must fail")
        } catch (_: IllegalArgumentException) {}
        try {
            GarminCompactTransport.frame(0x39, 128, 1, byteArrayOf())
            fail("sequence 128 must fail")
        } catch (_: IllegalArgumentException) {}
    }
}

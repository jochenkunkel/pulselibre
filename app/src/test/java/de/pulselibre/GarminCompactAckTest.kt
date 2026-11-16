package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminCompactAckTest {
    @Test fun genericAckMatchesPublishedWatchCapture() {
        val ack = GarminCompactAck.generic(0x32, 1)
        val expected = byteArrayOf(0x09, 0x00, 0x00, 0x81.toByte(), 0xba.toByte(), 0x13, 0x00, 0x9d.toByte(), 0xe9.toByte())
        assertArrayEquals(expected, ack)
        assertNotNull(GarminFrameValidator.inspectVerified(ack))
    }

    @Test fun chunkAckHasValidatedEnvelopeAndFields() {
        val ack = GarminCompactAck.chunk(0x2b, 127, 65535, 0x12345678)
        assertEquals(17, ack.size)
        assertEquals(0xff, ack[3].toInt() and 255)
        assertEquals(0x2b, ack[4].toInt() and 255)
        assertEquals(0x13, ack[5].toInt() and 255)
        assertEquals(0xff, ack[7].toInt() and 255)
        assertEquals(0xff, ack[8].toInt() and 255)
        assertEquals(0x78, ack[9].toInt() and 255)
        assertEquals(0x12, ack[12].toInt() and 255)
        assertNotNull(GarminFrameValidator.inspectVerified(ack))
    }

    @Test fun invalidChunkKindIsRejected() {
        try {
            GarminCompactAck.chunk(0x39, 1, 1, 0)
            fail("Only partial protobuf kinds can use chunk ACK")
        } catch (_: IllegalArgumentException) {}
    }
}

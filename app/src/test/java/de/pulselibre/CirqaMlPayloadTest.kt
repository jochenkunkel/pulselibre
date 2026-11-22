package de.pulselibre

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class CirqaMlPayloadTest {
    @Test fun emptyPacketGivesEmptyPayload() = assertArrayEquals(byteArrayOf(), payloadAfterHandle(byteArrayOf()))

    @Test fun routingByteOnlyGivesEmptyPayload() = assertArrayEquals(byteArrayOf(), payloadAfterHandle(byteArrayOf(0x81.toByte())))

    @Test fun exactlyOneRoutingByteIsRemoved() =
        assertArrayEquals(byteArrayOf(1, 2, 3), payloadAfterHandle(byteArrayOf(0x91.toByte(), 1, 2, 3)))

    @Test fun inputIsNotModified() {
        val input = byteArrayOf(5, 6, 7)
        payloadAfterHandle(input)
        assertArrayEquals(byteArrayOf(5, 6, 7), input)
    }
}

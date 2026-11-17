package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacy5002CodecTest {
    @Test fun exactRequestVectors() {
        assertArrayEquals(byteArrayOf(
            0x34, 0x12, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1
        ), CirqaLegacy5002Codec.request(0x1234, true))
        assertArrayEquals(byteArrayOf(
            0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0
        ), CirqaLegacy5002Codec.request(0, false))
        assertEquals(14, CirqaLegacy5002Codec.request(65535, true).size)
    }

    @Test fun invalidIndexRejected() {
        for (index in listOf(-1, 65536)) {
            try {
                CirqaLegacy5002Codec.request(index, false)
                fail("Expected invalid index")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun decodesUnsignedResponseAndRejectsOversize() {
        val body = byteArrayOf(0, 0x78, 0x56, 0x34, 0x12)
        assertEquals(0x12345678L, CirqaLegacy5002Codec.response(body)?.unsignedValue)
        assertEquals(0x12345678, CirqaLegacy5002Codec.boundedSuccessSize(body, 0x12345678))
        assertNull(CirqaLegacy5002Codec.boundedSuccessSize(body, 1024))
        assertEquals(0xffffffffL, CirqaLegacy5002Codec.response(
            byteArrayOf(0, -1, -1, -1, -1))?.unsignedValue)
        assertNull(CirqaLegacy5002Codec.boundedSuccessSize(
            byteArrayOf(0, -1, -1, -1, -1), Int.MAX_VALUE))
        assertNull(CirqaLegacy5002Codec.response(byteArrayOf(0, 1)))
        assertNull(CirqaLegacy5002Codec.boundedSuccessSize(byteArrayOf(3), 1024))
    }
}

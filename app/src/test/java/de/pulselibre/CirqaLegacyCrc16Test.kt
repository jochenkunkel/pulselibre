package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyCrc16Test {
    @Test fun knownConnectHelperVectors() {
        assertEquals(0, CirqaLegacyCrc16.compute(byteArrayOf()))
        assertEquals(0xbb3d, CirqaLegacyCrc16.compute("123456789".toByteArray(Charsets.US_ASCII)))
        assertArrayEquals(byteArrayOf(0x3d, 0xbb.toByte()),
            CirqaLegacyCrc16.littleEndian("123456789".toByteArray(Charsets.US_ASCII)))
        assertEquals(0xa110, CirqaLegacyCrc16.compute(byteArrayOf(1, 2, 3)))
        assertEquals(0xbad7, CirqaLegacyCrc16.compute("FIT".toByteArray(Charsets.US_ASCII)))
    }

    @Test fun dexVerifiedChunkComparisonUsesUpdatedAccumulatorForEmptySingleAndMultipleBytes() {
        // DEX yw40.a: empty input compares the incoming accumulator; each
        // payload byte replaces it with u45.a's returned accumulator before
        // the final if-eq compares it with the wire CRC.
        assertEquals(0x0000, CirqaLegacyCrc16.compute(byteArrayOf(), initial = 0))
        assertEquals(0x1234, CirqaLegacyCrc16.compute(byteArrayOf(), initial = 0x1234))
        assertEquals(0x30c0, CirqaLegacyCrc16.compute(byteArrayOf(0x41), initial = 0))
        assertEquals(0xe7d3, CirqaLegacyCrc16.compute(
            byteArrayOf(0x41), initial = 0x1234
        )) // confirms the running accumulator is carried across chunks
        assertEquals(0xbb3d, CirqaLegacyCrc16.compute(
            "123456789".toByteArray(Charsets.US_ASCII), initial = 0
        ))
    }

    @Test fun incrementalUpdateMatchesSinglePass() {
        val a = "1234".toByteArray(Charsets.US_ASCII)
        val b = "56789".toByteArray(Charsets.US_ASCII)
        assertEquals(CirqaLegacyCrc16.compute(a + b),
            CirqaLegacyCrc16.compute(b, CirqaLegacyCrc16.compute(a)))
    }
}

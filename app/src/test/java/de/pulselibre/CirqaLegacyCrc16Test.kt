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

    @Test fun incrementalUpdateMatchesSinglePass() {
        val a = "1234".toByteArray(Charsets.US_ASCII)
        val b = "56789".toByteArray(Charsets.US_ASCII)
        assertEquals(CirqaLegacyCrc16.compute(a + b),
            CirqaLegacyCrc16.compute(b, CirqaLegacyCrc16.compute(a)))
    }
}

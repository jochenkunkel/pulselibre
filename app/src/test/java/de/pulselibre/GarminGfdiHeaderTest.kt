package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminGfdiHeaderTest {
    private fun hex(s: String): ByteArray =
        s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun classifiesCapturedCirqaHeaders() {
        val device = hex("3600a01397009b138d5bcbd9a401000210434952514120536d6172742042616e640543495251410a536d6172742042616e640000a569")
        val retry = hex("1900ba131279071420f6df680e9a350695230209000200f480")
        val last = hex("1b00931342000001000515048c17048c1a048c01010a1601009559")
        for ((bytes, expected) in listOf(device to 5024, retry to 5050, last to 5011)) {
            val h = GarminGfdiHeader.inspect(bytes)
            assertNotNull(h)
            assertEquals(expected, h!!.type)
            assertFalse(h.compact)
            assertNull(h.sequence)
            assertNull(h.responseTo)
        }
    }

    @Test fun rejectsInvalidCrc() {
        val bad = hex("1900ba131279071420f6df680e9a350695230209000200f481")
        assertNull(GarminGfdiHeader.inspect(bad))
    }
}

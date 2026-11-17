package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyFileTypesParserTest {
    @Test fun parsesSyntheticSupportedTypes() {
        val bytes = byteArrayOf(
            2, 0x80.toByte(), 4, 10,
            70, 73, 84, 95, 84, 89, 80, 69, 95, 52,
            0x80.toByte(), 36, 11,
            70, 73, 84, 95, 84, 89, 80, 69, 95, 51, 54)
        val types = CirqaLegacyFileTypesParser.parse(bytes)!!
        assertEquals(2, types.size)
        assertEquals("FIT_TYPE_4", types[0].name)
        assertEquals(36, types[1].subtype)
        assertEquals("FIT_TYPE_36", types[1].name)
    }

    @Test fun rejectsTruncationAndInvalidUtf8() {
        assertNull(CirqaLegacyFileTypesParser.parse(byteArrayOf()))
        assertNull(CirqaLegacyFileTypesParser.parse(byteArrayOf(1, 0x80.toByte(), 4, 2, 65)))
        assertNull(CirqaLegacyFileTypesParser.parse(byteArrayOf(1, 0x80.toByte(), 4, 1, 0xff.toByte())))
        assertNull(CirqaLegacyFileTypesParser.parse(byteArrayOf(0, 1)))
        assertTrue(CirqaLegacyFileTypesParser.parse(byteArrayOf(0))!!.isEmpty())
    }
}

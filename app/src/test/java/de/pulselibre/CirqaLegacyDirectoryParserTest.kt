package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyDirectoryParserTest {
    @Test fun parsesDocumentedSyntheticEntry() {
        val header = ByteArray(16)
        val entry = byteArrayOf(
            0x34, 0x12, 0x80.toByte(), 1, 2, 3, 0xff.toByte(), 0x94.toByte(),
            0x78, 0x56, 0x34, 0x12, 0xef.toByte(), 0xcd.toByte(), 0xab.toByte(), 0x89.toByte())
        val directory = CirqaLegacyDirectoryParser.parse(header + entry)!!
        assertEquals(1, directory.entries.size)
        val file = directory.entries.single()
        assertEquals(0x1234, file.fileIndex)
        assertEquals(128, file.type)
        assertEquals(listOf(1, 2, 3), file.subtypeBytes)
        assertEquals(0x94, file.attributes)
        assertTrue(file.readable)
        assertEquals(0x12345678L, file.fieldA)
        assertEquals(0x89abcdefL, file.fieldB)
    }

    @Test fun rejectsTruncatedDirectory() {
        assertNull(CirqaLegacyDirectoryParser.parse(ByteArray(15)))
        assertNull(CirqaLegacyDirectoryParser.parse(ByteArray(31)))
        assertEquals(0, CirqaLegacyDirectoryParser.parse(ByteArray(16))!!.entries.size)
    }

    @Test fun parsesMultipleEntries() {
        assertEquals(2, CirqaLegacyDirectoryParser.parse(ByteArray(48))!!.entries.size)
    }
}

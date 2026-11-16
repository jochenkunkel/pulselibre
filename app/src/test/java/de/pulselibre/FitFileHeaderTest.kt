package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class FitFileHeaderTest {
    private fun fixture(headerSize: Int = 14, data: ByteArray = byteArrayOf()): ByteArray {
        val file = ByteArray(headerSize + data.size + 2)
        file[0] = headerSize.toByte()
        file[1] = 0x20
        file[2] = 0x64
        file[3] = 0
        file[4] = data.size.toByte()
        file[5] = (data.size ushr 8).toByte()
        file[8] = '.'.code.toByte()
        file[9] = 'F'.code.toByte()
        file[10] = 'I'.code.toByte()
        file[11] = 'T'.code.toByte()
        data.copyInto(file, headerSize)
        return file
    }

    @Test fun acceptsSynthetic12ByteEnvelope() {
        val header = FitFileHeader.inspect(fixture(12, byteArrayOf(1, 2, 3)))
        assertEquals(12, header?.headerSize)
        assertEquals(3L, header?.dataSize)
        assertEquals(100, header?.profileVersion)
    }

    @Test fun acceptsSynthetic14ByteEnvelope() {
        assertEquals(14, FitFileHeader.inspect(fixture())?.headerSize)
    }

    @Test fun rejectsWrongSignatureAndUnsupportedHeaderSize() {
        val wrongMagic = fixture()
        wrongMagic[9] = 'X'.code.toByte()
        assertNull(FitFileHeader.inspect(wrongMagic))
        val wrongSize = fixture()
        wrongSize[0] = 13
        assertNull(FitFileHeader.inspect(wrongSize))
    }

    @Test fun rejectsTruncationAndExtraBytes() {
        val valid = fixture(14, byteArrayOf(1, 2))
        assertNull(FitFileHeader.inspect(valid.copyOf(valid.size - 1)))
        assertNull(FitFileHeader.inspect(valid.copyOf(valid.size + 1)))
    }

    @Test fun rejectsHugeDeclaredDataSizeWithoutAllocating() {
        val file = fixture()
        file[7] = 0x7f
        assertNull(FitFileHeader.inspect(file))
    }

    @Test fun rejectsEmptyAndOversizedInputs() {
        assertNull(FitFileHeader.inspect(byteArrayOf()))
        assertNull(FitFileHeader.inspect(ByteArray(FitFileHeader.MAX_FILE_BYTES + 1)))
    }
    @Test fun standardCrcVector() {
        assertEquals(0xBB3D, FitFileHeader.crc16("123456789".toByteArray(Charsets.US_ASCII)))
    }

    @Test fun verified12ByteEnvelopeUsesIndependentKnownBytes() {
        val bytes = byteArrayOf(
            0x0c, 0x20, 0x64, 0, 0, 0, 0, 0,
            0x2e, 0x46, 0x49, 0x54, 0x27, 0x29
        )
        assertEquals(12, FitFileHeader.inspectVerified(bytes)?.headerSize)
        bytes[2] = 0x65
        assertNull(FitFileHeader.inspectVerified(bytes))
    }

    @Test fun verified14ByteEnvelopeChecksHeaderAndFileCrc() {
        val bytes = byteArrayOf(
            0x0e, 0x20, 0x64, 0, 0, 0, 0, 0,
            0x2e, 0x46, 0x49, 0x54,
            0xde.toByte(), 0xee.toByte(), 0, 0
        )
        assertEquals(14, FitFileHeader.inspectVerified(bytes)?.headerSize)
        bytes[12] = 0
        assertNull(FitFileHeader.inspectVerified(bytes))
    }

    @Test fun rejectsValidStructureWithCorruptFileCrc() {
        val bytes = byteArrayOf(
            0x0c, 0x20, 0x64, 0, 0, 0, 0, 0,
            0x2e, 0x46, 0x49, 0x54, 0x27, 0x29
        )
        bytes[13] = 0
        assertNotNull(FitFileHeader.inspect(bytes))
        assertNull(FitFileHeader.inspectVerified(bytes))
    }

}

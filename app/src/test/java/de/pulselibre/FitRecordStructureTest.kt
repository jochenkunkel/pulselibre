package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class FitRecordStructureTest {
    private fun file(records: ByteArray): ByteArray {
        val bytes = ByteArray(14 + records.size + 2)
        bytes[0] = 14
        bytes[1] = 0x20
        bytes[2] = 0x64
        bytes[4] = records.size.toByte()
        bytes[5] = (records.size ushr 8).toByte()
        bytes[8] = '.'.code.toByte()
        bytes[9] = 'F'.code.toByte()
        bytes[10] = 'I'.code.toByte()
        bytes[11] = 'T'.code.toByte()
        val headerCrc = FitFileHeader.crc16(bytes, 0, 12)
        bytes[12] = headerCrc.toByte()
        bytes[13] = (headerCrc ushr 8).toByte()
        records.copyInto(bytes, 14)
        val fileCrc = FitFileHeader.crc16(bytes, 0, bytes.size - 2)
        bytes[bytes.size - 2] = fileCrc.toByte()
        bytes[bytes.size - 1] = (fileCrc ushr 8).toByte()
        return bytes
    }

    @Test fun readsDefinitionAndDataRecordWithoutDecodingHealthValues() {
        val records = byteArrayOf(
            0x40, 0, 0, 20, 0, 1, 3, 2, 0x84.toByte(),
            0, 0x34, 0x12
        )
        val result = FitRecordStructure.inspect(file(records))
        assertEquals(1, result?.definitions?.size)
        assertEquals(20, result?.definitions?.first()?.globalMessage)
        assertEquals(1, result?.dataRecords)
    }

    @Test fun rejectsDataWithoutDefinition() {
        assertNull(FitRecordStructure.inspect(file(byteArrayOf(0, 1))))
    }

    @Test fun rejectsTruncatedDefinitionAndData() {
        assertNull(FitRecordStructure.inspect(file(byteArrayOf(0x40, 0, 0))))
        assertNull(FitRecordStructure.inspect(file(byteArrayOf(
            0x40, 0, 0, 20, 0, 1, 3, 2, 0x84.toByte(), 0, 1
        ))))
    }

    @Test fun rejectsCorruptedFileChecksum() {
        val bytes = file(byteArrayOf())
        bytes[12] = (bytes[12].toInt() xor 1).toByte()
        assertNull(FitRecordStructure.inspect(bytes))
    }

    @Test fun supportsCompressedTimestampHeaderForDefinedType() {
        val records = byteArrayOf(
            0x40, 0, 0, 20, 0, 1, 3, 1, 2,
            0x80.toByte(), 0x7f
        )
        assertEquals(1, FitRecordStructure.inspect(file(records))?.dataRecords)
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyTransferDecoderTest {
    @Test fun readResponseStatusAndTruncation() {
        val d = CirqaLegacyTransferDecoder
        assertNull(d.readResponse(byteArrayOf()))
        assertNull(d.readResponse(byteArrayOf(0, 1, 2, 3)))
        assertNull(d.readResponse(byteArrayOf(7)))
        assertEquals(d.ReadStatus.SUCCESS,
            d.readResponse(byteArrayOf(0, 1, 2, 3, 4))!!.status)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4),
            d.readResponse(byteArrayOf(0, 1, 2, 3, 4))!!.sizeBytes)
        assertEquals(d.ReadStatus.NOT_READY,
            d.readResponse(byteArrayOf(3))!!.status)
        assertEquals(d.ReadStatus.CRC_MISMATCH,
            d.readResponse(byteArrayOf(5))!!.status)
    }

    @Test fun plainChunkParsesUnsignedOffsetAndPayload() {
        val d = CirqaLegacyTransferDecoder
        assertNull(d.plainChunk(ByteArray(6)))
        val chunk = d.plainChunk(byteArrayOf(
            0, 0x34, 0x12, 0xff.toByte(), 0xff.toByte(),
            0xff.toByte(), 0xff.toByte(), 42, 43))!!
        assertEquals(0x1234, chunk.crc16)
        assertEquals(4294967295L, chunk.offset)
        assertArrayEquals(byteArrayOf(42, 43), chunk.payload)
    }

    @Test fun compressedChunkParsesFlagsSequenceAndCrc() {
        val d = CirqaLegacyTransferDecoder
        assertNull(d.compressedChunk(ByteArray(7)))
        val chunk = d.compressedChunk(byteArrayOf(
            0xff.toByte(), 2, 1, 0, 0, 0, 0x78, 0x56, 10))!!
        assertEquals(255, chunk.sequence)
        assertEquals(2, chunk.flags)
        assertEquals(1L, chunk.uncompressedOffset)
        assertEquals(0x5678, chunk.expectedCrc16)
        assertArrayEquals(byteArrayOf(10), chunk.compressedBytes)
    }
}

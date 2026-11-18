package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaDirectoryReadV2SessionTest {
    private fun gfdiFrame(type: Int, body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), (size ushr 8).toByte(), type.toByte(), (type ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    private fun chunkFrame(payload: ByteArray, offset: Int, crc: Int): ByteArray {
        val body = byteArrayOf(0, crc.toByte(), (crc ushr 8).toByte(), offset.toByte(),
            (offset ushr 8).toByte(), (offset ushr 16).toByte(), (offset ushr 24).toByte()) + payload
        return gfdiFrame(5004, body)
    }

    private fun directoryBytes(entries: List<Triple<Int, Int, Int>>): ByteArray {
        // 16-byte header + 16-byte entries (index u16, type u8, subtype u8, ..., flags u8@7, size u32@8)
        val out = java.io.ByteArrayOutputStream()
        out.write(ByteArray(16))
        entries.forEach { (idx, type, size) ->
            out.write(idx and 255); out.write(idx ushr 8)
            out.write(type); out.write(4); out.write(0); out.write(0); out.write(0)
            out.write(0x80) // readable
            out.write(size and 255); out.write((size ushr 8) and 255)
            out.write((size ushr 16) and 255); out.write(size ushr 24)
            out.write(ByteArray(4)) // timestamp bytes omitted from report
        }
        return out.toByteArray()
    }

    @Test fun readsDirectoryChunksWithCumulativeCrcAndParsesEntries() {
        val dir = directoryBytes(listOf(Triple(1, 128, 1000), Triple(2, 128, 2000)))
        val s = CirqaDirectoryReadV2Session()
        assertTrue(s.begin(dir.size))
        val half = dir.copyOfRange(0, dir.size / 2)
        val rest = dir.copyOfRange(dir.size / 2, dir.size)
        val crc1 = CirqaLegacyCrc16.compute(half)
        val crc2 = CirqaLegacyCrc16.compute(rest, crc1)
        // COBS-encode frames as the device would (delimiters at both ends)
        val f1 = GarminFileListRequest.cobs(chunkFrame(half, 0, crc1))
        assertEquals(CirqaDirectoryReadV2Session.Result.ACCEPTED, s.onNotification(f1))
        assertEquals(half.size, s.nextAckOffset())
        val f2 = GarminFileListRequest.cobs(chunkFrame(rest, half.size, crc2))
        assertEquals(CirqaDirectoryReadV2Session.Result.COMPLETE, s.onNotification(f2))
        assertEquals(CirqaDirectoryReadV2Session.Phase.COMPLETED, s.phase)
        assertEquals(2, s.directoryEntries)
        assertTrue(s.redactedEntryReport.contains("fit=true"))
        assertFalse(s.redactedEntryReport.contains("timestamp"))
        assertArrayEquals(dir, s.completedBytes())
    }

    @Test fun ignoresInterleavedControlFrameBeforeDataInSameNotification() {
        val dir = directoryBytes(listOf(Triple(1, 128, 1000)))
        val s = CirqaDirectoryReadV2Session()
        assertTrue(s.begin(dir.size))
        val control = GarminFileListRequest.cobs(gfdiFrame(5000, byteArrayOf(0, 0, 0)))
        val crc = CirqaLegacyCrc16.compute(dir)
        val data = GarminFileListRequest.cobs(chunkFrame(dir, 0, crc))
        // Device may place a 5000 ACK/protobuf control frame before the 5004 chunk.
        val combined = control.copyOfRange(0, control.size - 1) + data
        assertEquals(CirqaDirectoryReadV2Session.Result.COMPLETE, s.onNotification(combined))
        assertEquals(1, s.nonDataFrames)
        assertEquals(1, s.dataFramesProcessed)
        assertEquals("5000x1", s.controlFrameTypes)
        assertEquals(dir.size, s.receivedBytes)
    }

    @Test fun reassemblesCobsFrameSplitAcrossNotifications() {
        val dir = directoryBytes(listOf(Triple(1, 128, 1000)))
        val s = CirqaDirectoryReadV2Session()
        assertTrue(s.begin(dir.size))
        val crc = CirqaLegacyCrc16.compute(dir)
        val encoded = GarminFileListRequest.cobs(chunkFrame(dir, 0, crc))
        val cut = encoded.size / 2
        assertEquals(CirqaDirectoryReadV2Session.Result.ACCEPTED, s.onNotification(encoded.copyOfRange(0, cut)))
        assertEquals(0, s.receivedBytes)
        assertEquals(CirqaDirectoryReadV2Session.Result.COMPLETE, s.onNotification(encoded.copyOfRange(cut, encoded.size)))
        assertEquals(dir.size, s.receivedBytes)
    }

    @Test fun queuesTransportAckForInterleaved5043Chunk() {
        val dir = directoryBytes(listOf(Triple(1, 128, 1000)))
        val s = CirqaDirectoryReadV2Session()
        assertTrue(s.begin(dir.size))
        val protobuf = byteArrayOf(0x6a, 0x02, 0x08, 0x01) // field 13 wire 2
        val body = byteArrayOf(
            0x91.toByte(), 0x0a,                   // request id 2705
            0, 0, 0, 0,                             // offset 0
            protobuf.size.toByte(), 0, 0, 0,        // total 4
            protobuf.size.toByte(), 0, 0, 0         // chunk 4
        ) + protobuf
        val frame = GarminFileListRequest.cobs(gfdiFrame(5043, body))
        assertEquals(CirqaDirectoryReadV2Session.Result.DUPLICATE, s.onNotification(frame))
        assertEquals(listOf(2705 to 0L), s.drainProtobufAcks())
        assertTrue(s.drainProtobufAcks().isEmpty())
        assertEquals(0, s.receivedBytes)
    }

    @Test fun rejectsBadCrcAndWrongOffset() {
        val dir = directoryBytes(listOf(Triple(1, 128, 1000)))
        val s = CirqaDirectoryReadV2Session()
        assertTrue(s.begin(dir.size))
        val bad = GarminFileListRequest.cobs(chunkFrame(dir, 0, 12345))
        assertEquals(CirqaDirectoryReadV2Session.Result.CRC_ERROR, s.onNotification(bad))
        val s2 = CirqaDirectoryReadV2Session()
        s2.begin(dir.size)
        val crc = CirqaLegacyCrc16.compute(dir)
        val wrongOffset = GarminFileListRequest.cobs(chunkFrame(dir, 8, crc))
        assertEquals(CirqaDirectoryReadV2Session.Result.OFFSET_ERROR, s2.onNotification(wrongOffset))
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaRfcommDirectorySessionTest {
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

    private fun directory(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(ByteArray(16))
        out.write(0); out.write(0) // index 0
        out.write(0); out.write(0); out.write(0); out.write(0)
        out.write(0)
        out.write(ByteArray(9))
        return out.toByteArray()
    }

    @Test fun reassemblesSplitStreamAndReturns5004Ack() {
        val dir = directory()
        assertEquals(32, dir.size)
        val s = CirqaRfcommDirectorySession()
        assertTrue(s.begin(dir.size))
        val frame = GarminFileListRequest.cobs(chunkFrame(dir, 0, CirqaLegacyCrc16.compute(dir)))
        val acks = ArrayList<ByteArray>()
        // Simulate arbitrary socket chunking, including a split COBS frame.
        var i = 0
        while (i < frame.size) {
            val end = minOf(i + 3, frame.size)
            acks.addAll(s.acceptStream(frame.copyOfRange(i, end)))
            i = end
        }
        assertEquals(1, acks.size)
        val h = GarminGfdiHeader.inspect(acks[0])!!
        assertEquals(5000, h.type)
        assertEquals(5004, h.responseTo)
        assertEquals(0, h.responseStatus)
        assertNotNull(GarminFrameValidator.inspectVerified(acks[0]))
        assertEquals(32, u32(acks[0], 8))
        assertEquals(CirqaDirectoryReadV2Session.Phase.COMPLETED, s.phase)
        assertArrayEquals(dir, s.completedBytes)
    }

    @Test fun beginsDirectoryFrom5002Response() {
        val s = CirqaRfcommDirectorySession()
        val body = byteArrayOf(0x8A.toByte(), 0x13, 0, 0, 32, 0, 0, 0) // responseTo=5002, status 0, size 32
        val frame = GarminFileListRequest.cobs(gfdiFrame(5000, body))
        assertTrue(s.acceptStream(frame).isEmpty())
        assertEquals(CirqaDirectoryReadV2Session.Phase.RECEIVING, s.phase)
        assertEquals(32, s.expectedSize)
    }

    @Test fun acknowledgesInterleavedProtobufChunk() {
        val s = CirqaRfcommDirectorySession()
        assertTrue(s.begin(32))
        val protobuf = byteArrayOf(0x6a, 0x02, 0x0a, 0x00)
        val body = byteArrayOf(0x91.toByte(), 0x0a, 0, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0) + protobuf
        val frame = GarminFileListRequest.cobs(gfdiFrame(5043, body))
        val acks = s.acceptStream(frame)
        assertEquals(1, acks.size)
        val h = GarminGfdiHeader.inspect(acks[0])!!
        assertEquals(5043, h.responseTo)
        assertEquals(1, s.controlFrames)
    }

    private fun u32(bytes: ByteArray, i: Int): Long =
        (0..3).fold(0L) { n, k -> n or ((bytes[i + k].toLong() and 255L) shl (8 * k)) }
}

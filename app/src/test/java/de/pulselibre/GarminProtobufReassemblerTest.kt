package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminProtobufReassemblerTest {
    private fun packet(id: Int, offset: Int, total: Int, chunk: ByteArray, type: Int = 5044): ByteArray {
        fun le16(n: Int) = byteArrayOf(n.toByte(), (n ushr 8).toByte())
        fun le32(n: Int) = byteArrayOf(n.toByte(), (n ushr 8).toByte(), (n ushr 16).toByte(), (n ushr 24).toByte())
        val body = le16(id) + le32(offset) + le32(total) + le32(chunk.size) + chunk
        val length = body.size + 6
        val header = le16(length) + le16(type) + body
        val crc = GarminFrameValidator.crc16Arc(header)
        return header + le16(crc)
    }

    @Test fun joinsContiguousChunksAndClearsBuffer() {
        val receiver = GarminProtobufReassembler()
        assertNull(receiver.accept(packet(7, 0, 5, byteArrayOf(1, 2))))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5),
            receiver.accept(packet(7, 2, 5, byteArrayOf(3, 4, 5))))
        assertEquals(1, receiver.completed)
        assertEquals(0, receiver.rejected)
    }

    @Test fun duplicateFirstChunkDoesNotResetProgress() {
        val receiver = GarminProtobufReassembler()
        val first = packet(7, 0, 5, byteArrayOf(1, 2))
        assertNull(receiver.accept(first))
        assertNull(receiver.accept(first))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5),
            receiver.accept(packet(7, 2, 5, byteArrayOf(3, 4, 5))))
        assertEquals(0, receiver.rejected)
    }

    @Test fun duplicateMiddleChunkDoesNotCorruptProgress() {
        val receiver = GarminProtobufReassembler()
        assertNull(receiver.accept(packet(7, 0, 6, byteArrayOf(1, 2))))
        val middle = packet(7, 2, 6, byteArrayOf(3, 4))
        assertNull(receiver.accept(middle))
        assertNull(receiver.accept(middle))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6),
            receiver.accept(packet(7, 4, 6, byteArrayOf(5, 6))))
        assertEquals(0, receiver.rejected)
    }

    @Test fun rejectsConflictingRetransmission() {
        val receiver = GarminProtobufReassembler()
        assertNull(receiver.accept(packet(7, 0, 6, byteArrayOf(1, 2))))
        assertNull(receiver.accept(packet(7, 2, 6, byteArrayOf(3, 4))))
        assertNull(receiver.accept(packet(7, 2, 6, byteArrayOf(9, 9))))
        assertEquals(1, receiver.rejected)
    }

    @Test fun rejectsGapAndDoesNotReturnPartialData() {
        val receiver = GarminProtobufReassembler()
        assertNull(receiver.accept(packet(7, 0, 5, byteArrayOf(1, 2))))
        assertNull(receiver.accept(packet(7, 3, 5, byteArrayOf(4, 5))))
        assertEquals(1, receiver.rejected)
        assertEquals(0, receiver.completed)
    }

    @Test fun rejectsOversizedTransferBeforeAllocation() {
        val receiver = GarminProtobufReassembler(16)
        assertNull(receiver.accept(packet(7, 0, 100000, byteArrayOf(1))))
        assertEquals(1, receiver.rejected)
    }

    @Test fun reportsSupersededIncompleteTransfer() {
        val receiver = GarminProtobufReassembler()
        assertNull(receiver.accept(packet(7, 0, 5, byteArrayOf(1, 2))))
        assertArrayEquals(byteArrayOf(9), receiver.accept(packet(8, 0, 1, byteArrayOf(9))))
        assertEquals(1, receiver.rejected)
        assertEquals(1, receiver.completed)
    }

    @Test fun ignoresCorruptCrc() {
        val receiver = GarminProtobufReassembler()
        val packet = packet(7, 0, 1, byteArrayOf(1))
        packet[14] = (packet[14].toInt() xor 1).toByte()
        assertNull(receiver.accept(packet))
        assertEquals(0, receiver.completed)
    }
}

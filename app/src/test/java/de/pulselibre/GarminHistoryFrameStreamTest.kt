package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminHistoryFrameStreamTest {
    private fun frame(type: Int = 5012): ByteArray {
        val bytes = byteArrayOf(6, 0, type.toByte(), (type ushr 8).toByte(), 0, 0)
        val crc = GarminFrameValidator.crc16Arc(bytes, 0, 4)
        bytes[4] = crc.toByte()
        bytes[5] = (crc ushr 8).toByte()
        return bytes
    }

    @Test fun fragmentedFrame() {
        val decoder = GarminHistoryFrameStream()
        val bytes = frame()
        assertTrue(decoder.accept(bytes.copyOfRange(0, 3)).isEmpty())
        assertEquals(1, decoder.accept(bytes.copyOfRange(3, 6)).size)
        assertFalse(decoder.failed)
    }

    @Test fun concatenatedFrames() {
        val decoder = GarminHistoryFrameStream()
        val all = frame(5011) + frame(5012)
        val result = decoder.accept(all)
        assertEquals(2, result.size)
        assertEquals(5011, GarminFrameValidator.inspectVerified(result[0])?.messageType)
        assertEquals(5012, GarminFrameValidator.inspectVerified(result[1])?.messageType)
    }

    @Test fun badCrcFailsClosed() {
        val decoder = GarminHistoryFrameStream()
        val bad = frame()
        bad[3] = (bad[3].toInt() xor 1).toByte()
        assertTrue(decoder.accept(bad).isEmpty())
        assertTrue(decoder.failed)
        assertTrue(decoder.accept(frame()).isEmpty())
        decoder.reset()
        assertEquals(1, decoder.accept(frame()).size)
    }

    @Test fun rejectsInvalidLengthAndOversize() {
        val decoder = GarminHistoryFrameStream(32)
        assertTrue(decoder.accept(byteArrayOf(0, 0)).isEmpty())
        assertTrue(decoder.failed)
        decoder.reset()
        assertTrue(decoder.accept(ByteArray(33)).isEmpty())
        assertTrue(decoder.failed)
    }
}

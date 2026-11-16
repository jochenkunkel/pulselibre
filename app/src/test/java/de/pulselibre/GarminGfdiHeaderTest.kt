package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminGfdiHeaderTest {
    private fun frame(type: Int, body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), (size ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun classifiesGfdiHeaders() {
        for (type in listOf(5024, 5050, 5011)) {
            val h = GarminGfdiHeader.inspect(frame(type, byteArrayOf(1, 2, 3)))
            assertNotNull(h)
            assertEquals(type, h!!.type)
            assertFalse(h.compact)
            assertNull(h.sequence)
            assertNull(h.responseTo)
        }
    }

    @Test fun rejectsInvalidCrc() {
        val bad = frame(5050, byteArrayOf(1, 2, 3))
        bad[bad.lastIndex] = (bad.last().toInt() xor 1).toByte()
        assertNull(GarminGfdiHeader.inspect(bad))
    }
}

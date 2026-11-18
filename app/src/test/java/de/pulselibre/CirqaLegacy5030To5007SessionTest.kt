package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacy5030To5007SessionTest {
    private fun frame(type: Int, outer: Int, inner: ByteArray): ByteArray {
        val prefix = byteArrayOf((9 + inner.size).toByte(), 0, 0x88.toByte(), 0x13,
            type.toByte(), (type ushr 8).toByte(), outer.toByte()) + inner
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun completes5030And5007SequenceThenStopsBefore5031() {
        val s = CirqaLegacy5030To5007Session()
        assertTrue(s.begin(true, 0))
        assertTrue(s.bootstrapReady(s.token, true, 1))
        assertTrue(s.claim5030(s.token, 2))
        assertTrue(s.writeCompleted(s.token, 5030, true, 3))
        val f5030 = GarminFileListRequest.cobs(frame(5030, 0, byteArrayOf(0)))
        f5030.forEachIndexed { i, b -> assertTrue(s.receive(s.token, byteArrayOf(b), 4L + i)) }
        assertEquals(CirqaLegacy5030To5007Session.Phase.WAITING_5007_WRITE, s.phase)
        assertTrue(s.claim5007(s.token, 20))
        assertTrue(s.writeCompleted(s.token, 5007, true, 21))
        val f5007 = GarminFileListRequest.cobs(frame(5007, 0, byteArrayOf(0)))
        f5007.forEachIndexed { i, b -> assertTrue(s.receive(s.token, byteArrayOf(b), 22L + i)) }
        assertEquals(CirqaLegacy5030To5007Session.Phase.OBSERVED, s.phase)
        assertTrue(s.summary().contains("stop-before-5031=true"))
    }

    @Test fun stopsOnNonZeroInnerResultOrGateDenial() {
        val s = CirqaLegacy5030To5007Session()
        assertTrue(s.begin(true, 0))
        assertTrue(s.bootstrapReady(s.token, true, 1))
        assertTrue(s.claim5030(s.token, 2))
        assertTrue(s.writeCompleted(s.token, 5030, true, 3))
        val bad = GarminFileListRequest.cobs(frame(5030, 0, byteArrayOf(1)))
        bad.forEachIndexed { i, b -> s.receive(s.token, byteArrayOf(b), 4L + i) }
        assertEquals(CirqaLegacy5030To5007Session.Phase.STOPPED, s.phase)

        val denied = CirqaLegacy5030To5007Session()
        assertFalse(denied.begin(false, 0))
    }
}

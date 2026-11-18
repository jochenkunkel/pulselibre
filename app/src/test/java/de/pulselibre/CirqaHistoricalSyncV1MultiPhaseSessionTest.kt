package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaHistoricalSyncV1MultiPhaseSessionTest {
    private fun frame(type: Int, outer: Int, inner: ByteArray): ByteArray {
        val prefix = byteArrayOf((9 + inner.size).toByte(), 0, 0x88.toByte(), 0x13,
            type.toByte(), (type ushr 8).toByte(), outer.toByte()) + inner
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    private fun feed(s: CirqaHistoricalSyncV1MultiPhaseSession, frameBytes: ByteArray, startMs: Long): Long {
        var t = startMs
        frameBytes.forEachIndexed { i, b ->
            assertTrue("byte $i", s.receive(s.token, byteArrayOf(b), t++))
        }
        return t
    }

    @Test fun multiPhaseSequenceExecutesPhasesAndStops() {
        val s = CirqaHistoricalSyncV1MultiPhaseSession()
        assertTrue(s.begin(true, 0))
        assertTrue(s.bootstrapReady(s.token, true, 1))

        // Phase A: 5030 -> 5007
        assertTrue(s.claim5030(s.token, 2))
        assertTrue(s.writeCompleted(s.token, 5030, true, 3))
        var t = feed(s, GarminFileListRequest.cobs(frame(5030, 0, byteArrayOf(0))), 4)
        assertEquals(CirqaHistoricalSyncV1MultiPhaseSession.Phase.WAITING_5007_WRITE, s.phase)

        assertTrue(s.claim5007(s.token, t++))
        assertTrue(s.writeCompleted(s.token, 5007, true, t++))
        t = feed(s, GarminFileListRequest.cobs(frame(5007, 0, byteArrayOf(0))), t)
        assertEquals(CirqaHistoricalSyncV1MultiPhaseSession.Phase.PHASE_A_COMPLETED, s.phase)
        assertTrue(s.summary().contains("phase-a-completed-stop"))
    }

    @Test fun rejectsNonZeroInnerResultAndWrongOrder() {
        val s = CirqaHistoricalSyncV1MultiPhaseSession()
        assertTrue(s.begin(true, 0))
        assertTrue(s.bootstrapReady(s.token, true, 1))
        assertTrue(s.claim5030(s.token, 2))
        assertTrue(s.writeCompleted(s.token, 5030, true, 3))
        val bad = GarminFileListRequest.cobs(frame(5030, 0, byteArrayOf(1)))
        var t = 4L
        bad.forEach { b -> s.receive(s.token, byteArrayOf(b), t++) }
        assertEquals(CirqaHistoricalSyncV1MultiPhaseSession.Phase.STOPPED, s.phase)
    }
}

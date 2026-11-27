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

    @Test fun supportedFileTypesRejectsTrailingBytesAfterCountedEntries() {
        val s = CirqaHistoricalSyncV1MultiPhaseSession()
        assertTrue(s.begin(true, 0))
        assertTrue(s.bootstrapReady(s.token, true, 1))
        assertTrue(s.claim5030(s.token, 2))
        assertTrue(s.writeCompleted(s.token, 5030, true, 3))
        var t = feed(s, GarminFileListRequest.cobs(frame(5030, 0, byteArrayOf(0))), 4)
        assertTrue(s.claim5007(s.token, t++))
        assertTrue(s.writeCompleted(s.token, 5007, true, t++))
        t = feed(s, GarminFileListRequest.cobs(frame(5007, 0, byteArrayOf(0))), t)
        assertTrue(s.resumeToPhaseB(s.token, t++))
        assertTrue(s.claim5031(s.token, t++))
        assertTrue(s.writeCompleted(s.token, 5031, true, t++))

        // One counted type/subtype pair followed by an unaccounted byte is malformed.
        var accepted = true
        for (b in GarminFileListRequest.cobs(frame(
                5031, 0, byteArrayOf(1, 128.toByte(), 4, 0)
            ))) {
            accepted = s.receive(s.token, byteArrayOf(b), t++)
            if (!accepted) break
        }
        assertFalse(accepted)
        assertEquals(CirqaHistoricalSyncV1MultiPhaseSession.Phase.STOPPED, s.phase)
        assertTrue(s.reason.contains("5031-types-parse-error"))
    }

    @Test fun wireBuildersProduceVerifiedFrames() {
        val handle = 100
        val sysFrame = CirqaHistoricalSyncV1Wire.gfdiFrame(5030, byteArrayOf(8, 0))
        assertNotNull(GarminFrameValidator.inspectVerified(sysFrame))
        assertEquals(5030, GarminGfdiHeader.inspect(sysFrame)?.type)
        assertEquals(12, CirqaHistoricalSyncV1Wire.systemEventComplete(handle).size)
        assertTrue(CirqaHistoricalSyncV1Wire.filterPendingUploads(handle).size > 0)
        assertTrue(CirqaHistoricalSyncV1Wire.supportedFileTypes(handle).size > 0)
        val dirFrame = CirqaHistoricalSyncV1Wire.gfdiFrame(5002, ByteArray(14))
        assertNotNull(GarminFrameValidator.inspectVerified(dirFrame))
        assertTrue(CirqaHistoricalSyncV1Wire.directoryRead(handle, 0).size > 0)
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacy5043AckCandidateTest {
    @Test fun buildsConnectShaped5000AckForProtobufChunk() {
        val frame = CirqaLegacy5043AckCandidate.gfdiFrame(0x1234, 0x00000040)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        val h = GarminGfdiHeader.inspect(frame)!!
        assertEquals(5000, h.type)
        assertEquals(5043, h.responseTo)
        assertEquals(0, h.responseStatus)
        assertArrayEquals(
            byteArrayOf(0x34, 0x12, 0x40, 0, 0, 0, 0, 0),
            frame.copyOfRange(7, frame.size - 2)
        )
    }

    private fun expectIae(block: () -> Unit) {
        try { block(); fail("expected IllegalArgumentException") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun rejectsOutOfRangeArguments() {
        expectIae { CirqaLegacy5043AckCandidate.gfdiFrame(-1, 0) }
        expectIae { CirqaLegacy5043AckCandidate.gfdiFrame(1, -1) }
    }
}

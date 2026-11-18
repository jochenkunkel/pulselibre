package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaSyncRequestCandidateTest {
    @Test fun buildsConnectShapedSyncRequest() {
        assertArrayEquals(byteArrayOf(0x6a, 0x02, 0x0a, 0x00), CirqaSyncRequestCandidate.smart())
        val frame = CirqaSyncRequestCandidate.gfdiFrame(7)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(5043, GarminGfdiHeader.inspect(frame)!!.type)
        val body = frame.copyOfRange(4, frame.size - 2)
        assertArrayEquals(CirqaSyncRequestCandidate.smart(), body.copyOfRange(14, body.size))
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CirqaLegacy5043AckObservationTest {
    @Test
    fun parsesOnlyCrcVerifiedTransportAckFields() {
        val frame = CirqaLegacy5043AckCandidate.gfdiFrame(
            requestId = 1,
            receivedOffset = 0,
            ok = true,
            status = 0
        )

        assertEquals(
            CirqaLegacy5043AckObservation.Ack(
                gfdiAck = 0,
                requestId = 1,
                receivedOffset = 0,
                successFlag = 0,
                status = 0
            ),
            CirqaLegacy5043AckObservation.parse(frame)
        )
    }

    @Test
    fun rejectsCorruptOrUnrelatedFrames() {
        val corrupt = CirqaLegacy5043AckCandidate.gfdiFrame(1, 0).also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        val unrelated = CirqaLegacy5043AckCandidate.gfdiFrame(1, 0, responseTo = 5044)

        assertNull(CirqaLegacy5043AckObservation.parse(corrupt))
        assertNull(CirqaLegacy5043AckObservation.parse(unrelated))
    }
}

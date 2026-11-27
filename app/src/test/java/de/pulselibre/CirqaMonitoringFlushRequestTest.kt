package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

/** #637: Smart{47:{3:{1:0}}} exactly as in the Garmin Connect captures; the answer is Smart{47:{4:{…}}}. */
class CirqaMonitoringFlushRequestTest {
    @Test fun smartPayloadIsTheCapturedRequest() {
        val outer = CirqaFileAccessItemListParser.fields(CirqaMonitoringFlushRequest.smart())!!
        assertEquals(1, outer.size)
        assertEquals(47, outer[0].number)
        val inner = CirqaFileAccessItemListParser.fields(outer[0].bytes!!)!!.single()
        assertEquals(3, inner.number)
        val leaf = CirqaFileAccessItemListParser.fields(inner.bytes!!)!!.single()
        assertEquals(1, leaf.number); assertEquals(0L, leaf.varint)
    }

    @Test fun mlValueIsAValidCobsFramedGfdi5043() {
        val ml = CirqaMonitoringFlushRequest.mlValue(7, 42)
        assertEquals(7, ml[0].toInt())
        val frame = CirqaRfcommStreamFramer().accept(ml.copyOfRange(1, ml.size)).single { it.size > 6 }
        assertEquals(5043, GarminFrameValidator.inspectVerified(frame)?.messageType)
    }

    @Test fun recognisesOnlyTheAnswer() {
        assertTrue(CirqaMonitoringFlushRequest.isResponse(byteArrayOf(0xFA.toByte(), 0x02, 0x04, 0x22, 0x02, 0x08, 0x00)))
        assertFalse(CirqaMonitoringFlushRequest.isResponse(CirqaMonitoringFlushRequest.smart()))
        assertFalse(CirqaMonitoringFlushRequest.isResponse(byteArrayOf(0x6a, 0x02, 0x0a, 0x00)))
    }
}

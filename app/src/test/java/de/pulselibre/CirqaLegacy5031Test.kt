package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaLegacy5031Test {
    @Test fun requestIsEmptyBodyAndValidGfdiFrame() {
        assertEquals(0, CirqaLegacy5031RequestCandidate.body().size)
        val frame = CirqaLegacy5031RequestCandidate.gfdiFrame()
        assertEquals(5031, GarminGfdiHeader.inspect(frame)?.type)
        assertTrue(GarminFrameValidator.inspectVerified(frame) != null)
        assertEquals(6, frame.size)
    }

    @Test fun separateAttemptGateCannotBeReused() {
        assertTrue(CirqaLegacy5031AttemptGate.canAttempt(false))
        assertFalse(CirqaLegacy5031AttemptGate.canAttempt(true))
    }

    @Test fun parsesSuccessfulSupportedTypesResponse() {
        val types = byteArrayOf(1, 0x80.toByte(), 4, 1, 'A'.code.toByte())
        val prefix = byteArrayOf(
            14, 0, 0x88.toByte(), 0x13, 0xB7.toByte(), 0x13, 0
        ) + types
        val crc = GarminFrameValidator.crc16Arc(prefix)
        val response = prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        val observer = CirqaLegacy5031ResponseObserver()
        assertEquals(
            CirqaLegacy5031ResponseObserver.Result.SUPPORTED,
            observer.observeMlPayload(GarminFileListRequest.cobs(response))
        )
        assertEquals(0, observer.responseStatus)
        assertEquals("A", observer.fileTypes?.single()?.name)
    }

    @Test fun rejectsUnsupportedMalformedAndUnrelatedResponses() {
        val unsupported = CirqaLegacy5031ResponseObserver()
        assertEquals(
            CirqaLegacy5031ResponseObserver.Result.NOT_SUPPORTED,
            unsupported.observeMlPayload(
                GarminFileListRequest.cobs(CirqaOfflineGfdiStatusCandidate.ackFrame(5031).let {
                    it.clone().also { frame ->
                        frame[6] = 2
                        val prefix = frame.copyOfRange(0, frame.size - 2)
                        val crc = GarminFrameValidator.crc16Arc(prefix)
                        frame[frame.lastIndex - 1] = crc.toByte()
                        frame[frame.lastIndex] = (crc ushr 8).toByte()
                    }
                })
            )
        )
        val malformedBody = byteArrayOf(1, 0x80.toByte(), 4, 2, 65)
        val malformedPrefix = byteArrayOf(14, 0, 0x88.toByte(), 0x13, 0xB7.toByte(), 0x13, 0) + malformedBody
        val crc = GarminFrameValidator.crc16Arc(malformedPrefix)
        val malformed = malformedPrefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        assertEquals(
            CirqaLegacy5031ResponseObserver.Result.MALFORMED,
            CirqaLegacy5031ResponseObserver().observeMlPayload(GarminFileListRequest.cobs(malformed))
        )
    }
}

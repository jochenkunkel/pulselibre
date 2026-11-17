package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacy5007RequestCandidateTest {
    @Test fun buildsOnlyTheDocumentedConnectBodyAndVerifiedGfdiEnvelope() {
        assertArrayEquals(byteArrayOf(0x03), CirqaLegacy5007RequestCandidate.body())
        val expected = byteArrayOf(
            0x07, 0x00, 0x8f.toByte(), 0x13, 0x03, 0xc9.toByte(), 0x1a
        )
        val frame = CirqaLegacy5007RequestCandidate.gfdiFrame()
        assertArrayEquals(expected, frame)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(5007, GarminGfdiHeader.inspect(frame)?.type)
    }

    @Test fun wrapsTheGfdiFrameAsOneCobsDelimitedMlrValueCandidate() {
        assertArrayEquals(
            byteArrayOf(0x00, 0x02, 0x07, 0x06, 0x8f.toByte(), 0x13, 0x03, 0xc9.toByte(), 0x1a, 0x00),
            CirqaLegacy5007RequestCandidate.cobsDelimitedFrame()
        )
        val value = CirqaLegacy5007RequestCandidate.mlGattValueCandidate(dynamicHandle = 31)
        assertArrayEquals(byteArrayOf(0x1f) + CirqaLegacy5007RequestCandidate.cobsDelimitedFrame(), value)
        assertTrue(value.size <= CirqaLegacy5007RequestCandidate.MAX_GATT_VALUE_BYTES_AT_DEFAULT_MTU)
    }

    @Test fun rejectsInvalidRuntimeHandles() {
        listOf(0, 128, 255).forEach { handle ->
            assertThrows(IllegalArgumentException::class.java) {
                CirqaLegacy5007RequestCandidate.mlGattValueCandidate(handle)
            }
        }
    }

    @Test fun connectResponseBodyStatusIsNotPresentedAsCirqaEvidence() {
        assertTrue(CirqaLegacy5007RequestCandidate.isConnectResponseSupported(byteArrayOf(0)))
        assertFalse(CirqaLegacy5007RequestCandidate.isConnectResponseSupported(byteArrayOf(1)))
        assertFalse(CirqaLegacy5007RequestCandidate.isConnectResponseSupported(byteArrayOf()))
        assertNull(CirqaLegacy5007RequestCandidate.connectResponseStatus(byteArrayOf()))
    }
}

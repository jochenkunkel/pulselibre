package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnect531SystemEventTest {
    @Test fun handshakeCompleteHasTwoByteBody() {
        val body = CirqaConnect531SystemEvent.body(
            CirqaConnect531SystemEvent.HANDSHAKE_COMPLETE)
        assertArrayEquals(byteArrayOf(8, 0), body)
        assertTrue(CirqaConnect531SystemEvent.isHandshakeComplete(body))
    }

    @Test fun foregroundEventIsDistinct() {
        val body = CirqaConnect531SystemEvent.body(
            CirqaConnect531SystemEvent.HOST_DID_ENTER_FOREGROUND)
        assertArrayEquals(byteArrayOf(6, 0), body)
        assertFalse(CirqaConnect531SystemEvent.isHandshakeComplete(body))
    }

    @Test fun malformedEventsAreNotAccepted() {
        assertNull(CirqaConnect531SystemEvent.inspect(byteArrayOf(8)))
        assertNull(CirqaConnect531SystemEvent.inspect(byteArrayOf(8, 1)))
        assertNull(CirqaConnect531SystemEvent.inspect(byteArrayOf(8, 0, 0)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun eventMustFitOneByte() {
        CirqaConnect531SystemEvent.body(256)
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminMlHandshakeTest {
    @Test fun registrationRequestMatchesLittleEndianWireLayout() {
        assertArrayEquals(byteArrayOf(0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
            GarminMlHandshake.registerRequest(1))
        assertArrayEquals(byteArrayOf(0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0),
            GarminMlHandshake.registerRequest(4))
    }
    @Test fun closeAllIsNeverImplicit() {
        val session = GarminMlHandshake.Session(false)
        assertArrayEquals(GarminMlHandshake.registerRequest(1), session.begin())
        assertEquals(GarminMlHandshake.Stage.WAIT_REGISTER, session.stage)
    }
    @Test fun closeAllWaitsForAcknowledgement() {
        val session = GarminMlHandshake.Session(true)
        assertArrayEquals(byteArrayOf(0, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0), session.begin())
        assertNull(session.onNotification(byteArrayOf(0, 1)))
        assertEquals(GarminMlHandshake.Stage.WAIT_CLOSE, session.stage)
        assertArrayEquals(GarminMlHandshake.registerRequest(1),
            session.onNotification(byteArrayOf(0, 6)))
    }
    @Test fun acceptsOnlyGfdiRegistrationAndValidHandle() {
        val session = GarminMlHandshake.Session(false)
        session.begin()
        val response = byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 2)
        assertNull(session.onNotification(response))
        assertEquals(GarminMlHandshake.Stage.WAIT_REGISTER, session.stage)
        response[10] = 1
        session.onNotification(response)
        assertEquals(GarminMlHandshake.Stage.COMPLETE, session.stage)
    }
    @Test fun refusalFailsClosed() {
        val session = GarminMlHandshake.Session(false)
        session.begin()
        session.onNotification(byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0))
        assertEquals(GarminMlHandshake.Stage.FAILED, session.stage)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidService() { GarminMlHandshake.registerRequest(0) }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050PassiveSessionTest {
    @Test fun completeSequenceObservesSignal() {
        val s = Cirqa5050PassiveSession()
        s.queued()
        s.writeConfirmed()
        s.freshAck()
        s.valid5037()
        assertEquals(Cirqa5050PassiveSession.State.SIGNAL, s.state)
        assertTrue(s.signalObserved)
        s.stop()
        assertEquals(Cirqa5050PassiveSession.State.STOPPED, s.state)
        assertTrue(s.signalObserved)
    }

    @Test fun early5037DoesNotCount() {
        val s = Cirqa5050PassiveSession()
        s.queued()
        s.valid5037()
        s.writeConfirmed()
        s.valid5037()
        assertFalse(s.signalObserved)
        s.freshAck()
        assertEquals(Cirqa5050PassiveSession.State.OBSERVING, s.state)
    }

    @Test fun invalidOrderFailsClosed() {
        val s = Cirqa5050PassiveSession()
        s.freshAck()
        assertEquals(Cirqa5050PassiveSession.State.STOPPED, s.state)
        s.queued()
        assertEquals(Cirqa5050PassiveSession.State.STOPPED, s.state)
    }

    @Test fun duplicateAckStops() {
        val s = Cirqa5050PassiveSession()
        s.queued()
        s.writeConfirmed()
        s.freshAck()
        s.freshAck()
        assertEquals(Cirqa5050PassiveSession.State.STOPPED, s.state)
    }
}

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaSyncDiagnosticSessionTest {
    private fun observing(): CirqaSyncDiagnosticSession =
        CirqaSyncDiagnosticSession().apply {
            assertEquals(CirqaSyncDiagnosticSession.State.WAITING_FOR_5050_ACK,
                on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
            on(CirqaSyncDiagnosticSession.Event.WRITE_CONFIRMED)
            assertEquals(CirqaSyncDiagnosticSession.State.OBSERVING,
                on(CirqaSyncDiagnosticSession.Event.CONFIGURATION_ACK))
        }

    @Test fun validSequenceRecognizesOnly5037Signal() {
        val session = observing()
        assertEquals(CirqaSyncDiagnosticSession.State.SYNC_SIGNAL,
            session.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037))
        assertTrue(session.signalObserved)
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.TIMEOUT))
        assertTrue(session.signalObserved)
    }

    @Test fun disconnectAfterSignalStopsSessionWithoutLosingEvidence() {
        val session = observing()
        session.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037)
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.CONNECTION_LOST))
        assertTrue(session.signalObserved)
    }

    @Test fun refuses5037BeforeAuthorizationOrAck() {
        val early = CirqaSyncDiagnosticSession()
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            early.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037))
        val beforeAck = CirqaSyncDiagnosticSession()
        beforeAck.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED)
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            beforeAck.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037))
    }

    @Test fun refusesAckBeforeWriteConfirmation() {
        val session = CirqaSyncDiagnosticSession()
        session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED)
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.CONFIGURATION_ACK))
    }

    @Test fun refusesRepeatedWritesAndRepeatedAck() {
        val session = CirqaSyncDiagnosticSession()
        session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED)
        session.on(CirqaSyncDiagnosticSession.Event.WRITE_CONFIRMED)
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.WRITE_CONFIRMED))
        val other = observing()
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            other.on(CirqaSyncDiagnosticSession.Event.CONFIGURATION_ACK))
    }

    @Test fun allUnsafeEventsStopSession() {
        val unsafe = listOf(
            CirqaSyncDiagnosticSession.Event.NEGATIVE_RESPONSE,
            CirqaSyncDiagnosticSession.Event.REPEATED_5050,
            CirqaSyncDiagnosticSession.Event.UNEXPECTED_FRAME,
            CirqaSyncDiagnosticSession.Event.CONNECTION_LOST,
            CirqaSyncDiagnosticSession.Event.TIMEOUT
        )
        unsafe.forEach { event ->
            val session = observing()
            assertEquals(CirqaSyncDiagnosticSession.State.STOPPED, session.on(event))
            assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
                session.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037))
        }
    }
    @Test fun authorizationCannotBeReusedOrRestartedAfterStop() {
        val session = CirqaSyncDiagnosticSession()
        assertEquals(CirqaSyncDiagnosticSession.State.WAITING_FOR_5050_ACK,
            session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
        assertFalse(session.signalObserved)
    }

    @Test fun signalNeverAuthorizesSecondSynchronizationOrIgnoresStop() {
        val session = observing()
        assertEquals(CirqaSyncDiagnosticSession.State.SYNC_SIGNAL,
            session.on(CirqaSyncDiagnosticSession.Event.CRC_VALID_5037))
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.NEGATIVE_RESPONSE))
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
        assertTrue(session.signalObserved)
    }

    @Test fun unsafeEventBeforeAuthorizationPermanentlyStopsSession() {
        val session = CirqaSyncDiagnosticSession()
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.CONNECTION_LOST))
        assertEquals(CirqaSyncDiagnosticSession.State.STOPPED,
            session.on(CirqaSyncDiagnosticSession.Event.AUTHORIZE_WITH_RECORDER_STOPPED))
        assertFalse(session.signalObserved)
    }

}

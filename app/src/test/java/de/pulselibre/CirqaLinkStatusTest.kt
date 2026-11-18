package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State
import de.pulselibre.CirqaLinkStatus.Kind
import org.junit.Assert.*
import org.junit.Test

class CirqaLinkStatusTest {
    private val now = 1_000_000L

    @Test fun runningServiceWithoutGattOrDataIsNotOnline() {
        val r = CirqaLinkStatus.evaluate(true, State.DISCONNECTED, null, now)
        assertFalse(r.online); assertEquals(Kind.RECONNECTING, r.kind)
    }
    @Test fun freshReadingKeepsIndicatorGreenDuringGattLag() {
        val r = CirqaLinkStatus.evaluate(true, State.DISCONNECTED, now - 10_000, now)
        assertTrue(r.online); assertEquals(Kind.ONLINE_RECEIVING, r.kind)
        assertTrue(r.label.contains("10 s"))
    }
    @Test fun staleReadingWithoutGattGoesOfflineAndShowsAge() {
        val r = CirqaLinkStatus.evaluate(true, State.DISCONNECTED, now - 300_000, now)
        assertFalse(r.online); assertTrue(r.label.contains("5 min"))
    }
    @Test fun freshReadingIgnoredWhenRecorderStopped() {
        val r = CirqaLinkStatus.evaluate(false, State.DISCONNECTED, now - 5_000, now)
        assertFalse(r.online); assertEquals(Kind.RECORDER_OFF_DISCONNECTED, r.kind)
    }
    @Test fun connectedGattWithoutDataIsOnlineWaiting() {
        val r = CirqaLinkStatus.evaluate(true, State.CONNECTED, null, now)
        assertTrue(r.online); assertEquals(Kind.ONLINE_WAITING, r.kind)
    }
    @Test fun futureTimestampIsNotEvidence() {
        assertFalse(CirqaLinkStatus.evaluate(true, State.DISCONNECTED, now + 5_000, now).online)
    }
    @Test fun unpairedOrNoPermissionIsUnavailable() {
        assertEquals(Kind.UNAVAILABLE, CirqaLinkStatus.evaluate(true, State.NOT_PAIRED, now, now).kind)
        assertEquals(Kind.UNAVAILABLE, CirqaLinkStatus.evaluate(true, State.PERMISSION_DENIED, now, now).kind)
    }
}

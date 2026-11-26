package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #528: the trace window is bounded, explicit and shows bytes only after the separate opt-in. */
class CommunicationTraceTest {
    private val t0 = 1_790_000_000_000L

    @Test fun windowNeedsAnExplicitStartAndEndsByItselfAfterFifteenMinutes() {
        assertEquals(CommunicationTrace.State.Idle, CommunicationTrace.state(null, null, t0))
        assertEquals(CommunicationTrace.State.Recording(t0, t0 + CommunicationTrace.MAX_WINDOW_MS),
            CommunicationTrace.state(t0, null, t0 + 60_000))
        assertEquals(CommunicationTrace.State.Finished(t0, t0 + CommunicationTrace.MAX_WINDOW_MS),
            CommunicationTrace.state(t0, null, t0 + CommunicationTrace.MAX_WINDOW_MS + 1))
        assertEquals(CommunicationTrace.State.Finished(t0, t0 + 30_000), CommunicationTrace.state(t0, t0 + 30_000, t0 + 60_000))
        // A stop stored after the limit cannot stretch the window.
        assertEquals(CommunicationTrace.State.Finished(t0, t0 + CommunicationTrace.MAX_WINDOW_MS),
            CommunicationTrace.state(t0, t0 + 2 * CommunicationTrace.MAX_WINDOW_MS, t0 + 3 * CommunicationTrace.MAX_WINDOW_MS))
    }

    @Test fun bytesStayHiddenWithoutOptInAndAreCappedWithIt() {
        val finished = CommunicationTrace.State.Finished(t0, t0 + 10_000)
        val hidden = CommunicationTrace.format(finished,
            CommunicationTrace.Trace(1, 20, listOf(CommunicationTrace.Entry(t0 + 1_500, 20, 0x05, null))), t0 + 20_000)
        assertTrue(hidden, hidden.contains("+1,5 s · 20 B · Kanal 0x05"))
        assertTrue(hidden.contains("nur nach separatem Opt-in"))
        assertFalse(Regex("Kanal 0x05 · [0-9A-F]{2}").containsMatchIn(hidden))

        val head = ByteArray(BleSafetyPolicy.maximumDiagnosticBytes) { 0xAB.toByte() }
        val shown = CommunicationTrace.format(finished,
            CommunicationTrace.Trace(1, 300, listOf(CommunicationTrace.Entry(t0, 300, 0xAB, head))), t0 + 20_000)
        assertTrue(shown.contains("AB AB AB"))
        assertTrue("truncated bytes are marked", shown.contains(" …"))
        assertTrue(shown.contains("Opt-in"))
    }

    @Test fun honestTextsForIdleEmptyWindowAndLimit() {
        assertTrue(CommunicationTrace.format(CommunicationTrace.State.Idle, null, t0).contains("Keine Aufzeichnung gestartet"))
        assertTrue(CommunicationTrace.format(CommunicationTrace.State.Idle, null, t0).contains("keine eigene Bluetooth-Verbindung"))
        // FIT 26.11.: the view must not suggest completeness or contain sent commands.
        assertTrue(CommunicationTrace.format(CommunicationTrace.State.Idle, null, t0).contains("Kein vollständiger Ablauf-Trace"))
        val rec = CommunicationTrace.State.Recording(t0, t0 + CommunicationTrace.MAX_WINDOW_MS)
        val empty = CommunicationTrace.format(rec, CommunicationTrace.Trace(0, 0, emptyList()), t0 + 60_000)
        assertTrue(empty, empty.contains("noch 14 min"))
        assertTrue(empty.contains("kein Sync"))
        val many = CommunicationTrace.format(rec, CommunicationTrace.Trace(100, 2000,
            List(CommunicationTrace.MAX_ENTRIES) { CommunicationTrace.Entry(t0 + it, 20, 1, null) }), t0 + 60_000)
        assertTrue(many.contains("36 weitere nicht angezeigt"))
    }
}

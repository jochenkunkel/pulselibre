package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #637: a progressing download is no longer cut after 120 s; idle runs and the hard cap still end it. */
class CirqaSyncSessionDeadlineTest {
    private val idle = CirqaSyncSessionDeadline.IDLE_LIMIT_MS

    @Test fun progressKeepsTheRunAliveBeyondTheOld120Seconds() {
        val d = CirqaSyncSessionDeadline(0)
        var t = 0L; var files = 0
        while (t < 10 * 60_000L) { // one more file every 60 s for 10 min
            assertNull("t=$t", d.check("files=$files", t))
            t += 60_000L; files++
        }
    }

    @Test fun noProgressEndsTheRunAfterTheIdleLimit() {
        val d = CirqaSyncSessionDeadline(0)
        assertNull(d.check("a", 0))
        assertNull(d.check("a", idle - 1))
        assertEquals(CirqaSyncSessionDeadline.Expiry.IDLE, d.check("a", idle))
    }

    @Test fun theHardCapEndsEvenAProgressingRun() {
        val d = CirqaSyncSessionDeadline(0)
        assertNull(d.check("x1", CirqaSyncSessionDeadline.MAX_SESSION_MS - 1))
        assertEquals(CirqaSyncSessionDeadline.Expiry.TOTAL, d.check("x2", CirqaSyncSessionDeadline.MAX_SESSION_MS))
    }

    @Test fun terminalCauseNamesTheHardCapAndKeepsTheIdleCause() {
        assertEquals("session-total-limit", CirqaSyncTerminalEvidence.cause("${CirqaSyncSessionDeadline.TOTAL_MESSAGE}; x", "none", false))
        assertEquals("session-time-limit", CirqaSyncTerminalEvidence.cause("${CirqaSyncSessionDeadline.IDLE_MESSAGE} ohne Fortschritt; x", "none", false))
    }

    @Test fun rejectionPolicySkipsSingleRejectionsAndStopsAfterThreeInARow() {
        assertFalse(CirqaPullRejectionPolicy.stopAfter(1))
        assertFalse(CirqaPullRejectionPolicy.stopAfter(2))
        assertTrue(CirqaPullRejectionPolicy.stopAfter(3))
    }
}

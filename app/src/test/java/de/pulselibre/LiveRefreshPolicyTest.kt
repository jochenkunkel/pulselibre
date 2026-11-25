package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRefreshPolicyTest {
    @Test fun newDataWaitsForTheMinimumInterval() {
        assertFalse(LiveRefreshPolicy.due(true, 1_000, LiveRefreshPolicy.DASHBOARD_MIN_MS))
        assertTrue(LiveRefreshPolicy.due(true, 5_000, LiveRefreshPolicy.DASHBOARD_MIN_MS))
        assertFalse(LiveRefreshPolicy.due(true, 14_000, LiveRefreshPolicy.HISTORY_MIN_MS))
    }

    @Test fun withoutNewDataOnlyTheIdleRefreshRuns() {
        assertFalse(LiveRefreshPolicy.due(false, 29_000, LiveRefreshPolicy.DASHBOARD_MIN_MS))
        assertTrue(LiveRefreshPolicy.due(false, 30_000, LiveRefreshPolicy.DASHBOARD_MIN_MS))
    }

    @Test fun aResetIntervalForcesARefresh() {
        // Callers reset the last refresh time to 0 (elapsed-realtime base): the interval is then far above every limit.
        assertTrue(LiveRefreshPolicy.due(false, 3_600_000L, LiveRefreshPolicy.HISTORY_MIN_MS))
    }

    @Test fun oneSecondLiveStreamReloadsTheOverviewAtMostEveryFiveSeconds() {
        var last = 0L; var refreshes = 0
        for (t in 1_000L..60_000L step 1_000L) if (LiveRefreshPolicy.due(true, t - last, LiveRefreshPolicy.DASHBOARD_MIN_MS)) { last = t; refreshes++ }
        assertEquals(12, refreshes)
    }
}

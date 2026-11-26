package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalSyncPolicyTest {
    // Owner 26.11.2026 (#637): daily-history files vanish from the band once Garmin Connect has pulled them.
    @Test fun appEntryFetchesHistoryAutomatically() {
        assertTrue(HistoricalSyncPolicy.AUTOMATIC_SYNC_ENABLED)
    }

    @Test fun neverSyncedIsDueAtAppEntry() {
        assertTrue(HistoricalSyncPolicy.isDue(100_000L, 0L, 0L))
    }

    @Test fun freshSuccessSuppressesSyncUntilFifteenMinutes() {
        val lastSuccess = 100_000L
        assertFalse(HistoricalSyncPolicy.isDue(
            lastSuccess + HistoricalSyncPolicy.FRESHNESS_MS - 1, 0L, lastSuccess
        ))
        assertTrue(HistoricalSyncPolicy.isDue(
            lastSuccess + HistoricalSyncPolicy.FRESHNESS_MS, 0L, lastSuccess
        ))
    }

    @Test fun attemptCooldownAppliesEvenWhenPreviousRunFailed() {
        assertFalse(HistoricalSyncPolicy.isDue(399_999L, 100_000L, 0L))
        assertTrue(HistoricalSyncPolicy.isDue(
            100_000L + HistoricalSyncPolicy.MIN_ATTEMPT_INTERVAL_MS, 100_000L, 0L
        ))
    }

    @Test fun appResumeDoesNotCauseAnAttemptWhileOneIsFresh() {
        val lastAttempt = 500_000L
        assertFalse(HistoricalSyncPolicy.isDue(
            lastAttempt + 1_000L, lastAttempt, lastAttempt - 1_000L
        ))
    }
}

package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalSyncPolicyTest {
    @Test fun neverSyncedIsDueAtAppEntry() {
        assertTrue(HistoricalSyncPolicy.isDue(100_000L, 0L, 0L))
    }

    @Test fun freshSuccessSuppressesSyncUntilFifteenMinutes() {
        assertFalse(HistoricalSyncPolicy.isDue(1_000_000L, 0L, 100_000L))
        assertTrue(HistoricalSyncPolicy.isDue(1_000_000L, 0L, 100_000L + HistoricalSyncPolicy.FRESHNESS_MS))
    }

    @Test fun attemptCooldownAppliesEvenWhenPreviousRunFailed() {
        assertFalse(HistoricalSyncPolicy.isDue(400_000L, 100_000L, 0L))
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

package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class HistorySyncRetryPolicyTest {
    private val gatt133 = "Sync-Beobachtung: runId=7b3a86da27f0; ML-Verbindung beendet (GATT 133, Zustand 0). Letzte Phase: " +
        "GATT-Verbindung angefordert; runDrained=false; coverage=device-history-unverified incompleteReason=sync-not-accepted; " +
        "accepted=false; itemListSent=false; pullSent=false"
    private val cccd = "Sync-Beobachtung: runId=25288ae2990f; CCCD-Fehler: 129; runDrained=false; " +
        "incompleteReason=sync-not-accepted; accepted=false; itemListSent=false; pullSent=false"

    @Test fun connectionSetupFailuresAreRepeatedOnce() {
        assertTrue(HistorySyncRetryPolicy.shouldRetry(gatt133, 1))
        assertTrue(HistorySyncRetryPolicy.shouldRetry(cccd, 1))
        assertFalse(HistorySyncRetryPolicy.shouldRetry(gatt133, 2))
    }

    @Test fun anythingAfterTheSyncWasAcceptedIsNeverRepeated() {
        assertFalse(HistorySyncRetryPolicy.shouldRetry(gatt133.replace("itemListSent=false", "itemListSent=true"), 1))
        assertFalse(HistorySyncRetryPolicy.shouldRetry(
            "Sync-Beobachtung: x; 120-s-Zeitlimit ohne Fortschritt; incompleteReason=session-time-limit; itemListSent=true", 1))
        assertFalse(HistorySyncRetryPolicy.shouldRetry(
            "Sync-Beobachtung: x; incompleteReason=next-transaction-followup-empty; itemListSent=true", 1))
    }
}

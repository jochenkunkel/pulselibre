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

    @Test fun bandBusyAfterActivityIsRetriedOnceLater() {
        val busy = "details=pull-rejected(result=3);pull-rejected(result=3);pull-rejected(result=3); " +
            "Pull-Antwort nicht erfolgreich; 3 Ablehnungen in Folge, weitere Abrufe gestoppt. files=[successful=0,notSuccessful=3]"
        org.junit.Assert.assertEquals(HistorySyncRetryPolicy.BUSY_DELAY_MS, HistorySyncRetryPolicy.delayFor(busy, 1))
        org.junit.Assert.assertNull(HistorySyncRetryPolicy.delayFor(busy, 2))
        // some files moved: no repeat
        org.junit.Assert.assertNull(HistorySyncRetryPolicy.delayFor(busy.replace("successful=0,", "successful=4,"), 1))
    }
}

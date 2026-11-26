package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner 26.11.2026: the Gerät page must say in words what the technical sync report means. Excerpts from S10 runs. */
class SyncReportSummaryTest {
    private val rejected = "Sync-Beobachtung: runId=49d2005da20a; Pull-Antwort nicht erfolgreich; weitere Abrufe gestoppt.; " +
        "runDrained=false; coverage=device-history-unverified incompleteReason=file-processing-failed; accepted=true; " +
        "files=[successful=0,notSuccessful=1,queued=37,selected=38]; terminal=[cause=file-processing-failed,listResponses=1," +
        "listed=38,selected=38,pendingQueue=37]; |pull#3:size=4841|result=3,transport=0,handle=0,path=false]"
    private val followupEmpty = "Sync-Beobachtung: runId=57379931f46b; Die letzte nextTransaction-Folgeantwort enthielt 0 Einträge; " +
        "runDrained=false; coverage=device-history-unverified incompleteReason=next-transaction-followup-empty; " +
        "files=[successful=38,notSuccessful=0,selected=38]; terminal=[listed=38,selected=38,pendingQueue=0]"

    @Test fun rejectedPullNamesCodeOfferedAndUnrequestedFiles() {
        assertEquals("Das Band hat den Abruf der ersten Datei abgelehnt (Code 3). Es hatte 38 Dateien angeboten; " +
            "37 weitere wurden nicht angefragt.", SyncReportSummary.explain(GermanStrings, rejected))
    }

    @Test fun emptyFollowupSaysTransferredButCompletenessUnproven() {
        val text = SyncReportSummary.explain(GermanStrings, followupEmpty)!!
        assertTrue(text, text.startsWith("38 von 38 angebotenen Dateien übertragen") && text.contains("nicht nachgewiesen"))
    }

    @Test fun drainedRunIsComplete() {
        val report = "Sync-Beobachtung: runId=x; ok; runDrained=true; incompleteReason=none; files=[successful=5,notSuccessful=0]"
        assertEquals("Abruf vollständig: 5 Dateien übertragen.", SyncReportSummary.explain(GermanStrings, report))
    }

    @Test fun unknownTextStaysAsItIsAndTheReportIsAlwaysKept() {
        assertNull(SyncReportSummary.explain(GermanStrings, "Abbruch durch Nutzer"))
        assertEquals("Abbruch durch Nutzer", SyncReportSummary.display(GermanStrings, "Abbruch durch Nutzer"))
        val shown = SyncReportSummary.display(GermanStrings, rejected)
        assertTrue(shown.endsWith(rejected))
        assertTrue(shown.contains("Technische Details (antippen zum Aufklappen):"))
    }
}

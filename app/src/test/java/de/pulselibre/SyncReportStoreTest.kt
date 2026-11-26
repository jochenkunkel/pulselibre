package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #637 (FIT request 26.11.2026): a long report must keep run id, terminal cause, counters and every pull result visible. */
class SyncReportStoreTest {
    /** Synthetic only: field names follow CirqaSyncRunReport, values are invented. */
    private fun longReport(items: Int, pulls: Int): String = buildString {
        append("Sync-Beobachtung: runId=000000000001; Pull-Antwort nicht erfolgreich; weitere Abrufe gestoppt.; runDrained=false; ")
        append("coverage=device-history-unverified incompleteReason=file-processing-failed; accepted=true; itemListSent=true; ")
        append("pullSent=true; files=[successful=1,notSuccessful=1,itemTiming=[")
        repeat(items) { append("item$it[archive:29ms,fit-decode:74ms,fit-import:5ms,sqlite-commit:125ms,storage:5ms,transfer:996ms];") }
        append("]]; terminal=[cause=file-processing-failed,listResponses=1,listed=9,selected=9,pendingQueue=7,currentItem=true]; ")
        append("items=[page=1,status=0,items=9]; ")
        repeat(pulls) { append("|pull#${it + 1}:size=100${it}|result=${it % 4},transport=0,handle=0,path=false,errorCode=none]") }
        append("; mlr=[chunks=0,bytes=0]")
    }

    @Test fun diagnosisComesBeforeTheLongDetailsAndNothingIsRemoved() {
        val report = longReport(items = 80, pulls = 3)
        assertTrue("fixture is longer than the old 4000 limit", report.length > 4000)
        val stored = SyncReportStore.forStorage(report)
        val first4000 = stored.take(4000)
        assertTrue(first4000.contains("runId=000000000001"))
        assertTrue(first4000.contains("incompleteReason=file-processing-failed"))
        assertTrue(first4000.contains("terminal=[cause=file-processing-failed,listResponses=1,listed=9,selected=9,pendingQueue=7,currentItem=true]"))
        for (n in 1..3) assertTrue("pull#$n", first4000.contains("pull#$n:size=100${n - 1}|result=${(n - 1) % 4}"))
        assertTrue("original report kept in full", stored.endsWith(report))
        assertTrue(stored.contains(SyncReportStore.PRIORITY_LABEL))
    }

    @Test fun overTheCapTheCutIsMarkedAndTheDiagnosisSurvives() {
        val report = longReport(items = 1_500, pulls = 40)
        assertTrue(report.length > SyncReportStore.MAX_CHARS)
        val stored = SyncReportStore.forStorage(report)
        assertEquals(SyncReportStore.MAX_CHARS, stored.length)
        assertTrue(stored.endsWith(SyncReportStore.TRUNCATED_MARK))
        assertTrue(stored.contains("terminal=[cause=file-processing-failed"))
        assertEquals(40, SyncReportStore.pullResults(stored.substringBefore("Vollständiger Bericht:")).size)
    }

    @Test fun reportsWithoutDiagnosisStayUnchanged() {
        val plain = "Sync-Beobachtung: runId=x; ok; runDrained=true; incompleteReason=none; files=[successful=2,notSuccessful=0]"
        assertEquals(plain, SyncReportStore.forStorage(plain))
        assertEquals("Abbruch", SyncReportStore.forStorage("Abbruch"))
    }

    @Test fun readableSummaryStillWorksOnTheStoredForm() {
        val stored = SyncReportStore.forStorage(longReport(items = 80, pulls = 1))
        assertTrue(SyncReportSummary.explain(GermanStrings, stored)!!.startsWith("Das Band hat Dateiabrufe abgelehnt (Code 0)"))
    }
}

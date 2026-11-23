package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The progress display never claims completeness or 100 % without proven coverage. */
class HistorySyncProgressTest {
    private val s = GermanStrings

    @Test fun everyStageHasAReadableTitle() {
        HistorySyncStage.values().forEach { assertTrue(it.name, HistorySyncProgressText.title(s, HistorySyncProgress(it)).isNotBlank()) }
        assertEquals("Dateiliste wird ermittelt …", HistorySyncProgressText.title(s, HistorySyncProgress(HistorySyncStage.LISTING_FILES)))
    }

    @Test fun finishedWithoutProofIsNeverCalledComplete() {
        val title = HistorySyncProgressText.title(s, HistorySyncProgress(HistorySyncStage.FINISHED, 12, 12))
        assertTrue(title, title.contains("Abdeckung nicht nachgewiesen"))
        assertFalse(title.contains("abgeschlossen"))
        assertTrue(HistorySyncProgressText.detail(s, HistorySyncProgress(HistorySyncStage.FINISHED, 12, 12))!!.contains("nicht als vollständig nachgewiesen"))
        assertEquals("Synchronisation abgeschlossen", HistorySyncProgressText.title(s, HistorySyncProgress(HistorySyncStage.FINISHED, 12, 12, coverageProven = true)))
    }

    @Test fun barIsCappedWithoutProofAndNullWithoutNumbers() {
        assertEquals(HistorySyncProgress.MAX_UNPROVEN, HistorySyncProgress(HistorySyncStage.TRANSFERRING, 10, 10).fraction()!!, 0f)
        assertEquals(1f, HistorySyncProgress(HistorySyncStage.FINISHED, 10, 10, coverageProven = true).fraction()!!, 0f)
        assertEquals(0.5f, HistorySyncProgress(HistorySyncStage.TRANSFERRING, 5, 10).fraction()!!, 0f)
        assertNull(HistorySyncProgress(HistorySyncStage.LISTING_FILES).fraction())
        assertNull(HistorySyncProgress(HistorySyncStage.TRANSFERRING, 3, 0).fraction())
        assertNull(HistorySyncProgress(HistorySyncStage.TRANSFERRING, -1, 4).fraction())
    }

    @Test fun fileCountsOnlyWhenRealAndClamped() {
        assertEquals("3 von 8 Dateien", HistorySyncProgressText.detail(s, HistorySyncProgress(HistorySyncStage.TRANSFERRING, 3, 8)))
        assertEquals("8 von 8 Dateien", HistorySyncProgressText.detail(s, HistorySyncProgress(HistorySyncStage.TRANSFERRING, 9, 8)))
        assertNull(HistorySyncProgressText.detail(s, HistorySyncProgress(HistorySyncStage.TRANSFERRING)))
        assertNull(HistorySyncProgressText.detail(s, HistorySyncProgress.IDLE))
    }

    @Test fun failedAndInterruptedKeepTheCoverageNote() {
        assertTrue(HistorySyncProgress(HistorySyncStage.FAILED).isTerminal)
        assertTrue(HistorySyncProgress(HistorySyncStage.CONNECTING).isRunning)
        assertTrue(HistorySyncProgressText.detail(s, HistorySyncProgress(HistorySyncStage.INTERRUPTED, 2, 5))!!.contains("2 von 5 Dateien"))
    }

    @Test fun reportMappingNeverUpgradesToProvenCoverage() {
        val drained = HistorySyncProgress.fromReport("x runDrained=true incompleteReason=none coverage=device-history-unverified")
        assertEquals(HistorySyncStage.FINISHED, drained.stage); assertFalse(drained.coverageProven)
        assertEquals(HistorySyncStage.FINISHED_INCOMPLETE, HistorySyncProgress.fromReport("runDrained=false incompleteReason=timeout").stage)
        assertEquals(HistorySyncStage.FAILED, HistorySyncProgress.fromReport("Bluetooth nicht verfügbar").stage)
    }

    @Test fun reportFileCountsAreReadOnlyWhenPresentAndFailuresMakeItIncomplete() {
        val ok = HistorySyncProgress.fromReport("runDrained=true; coverage=device-history-unverified incompleteReason=none; files=[successful=7,notSuccessful=0,stages=[],queued=0,selected=7,selectedBytes=10]")
        assertEquals(HistorySyncStage.FINISHED, ok.stage); assertEquals(7, ok.filesDone); assertEquals(7, ok.filesTotal)
        assertEquals(HistorySyncProgress.MAX_UNPROVEN, ok.fraction()!!, 0f)
        val failed = HistorySyncProgress.fromReport("runDrained=true incompleteReason=none; files=[successful=5,notSuccessful=2,selected=7]")
        assertEquals(HistorySyncStage.FINISHED_INCOMPLETE, failed.stage)
        assertEquals("5 von 7 Dateien", HistorySyncProgressText.detail(this.s, failed)!!.lines().first())
        val none = HistorySyncProgress.fromReport("runDrained=true incompleteReason=none; files=[successful=0,notSuccessful=0,selected=0]")
        assertNull(none.filesTotal)
    }
}

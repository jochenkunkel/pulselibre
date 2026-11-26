package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner feedback 25.11.2026: long backups show their step and an overall percentage that only moves forward. */
class BackupProgressTextTest {
    private val P = BackupProgress.Phase

    @Test fun backupPercentRisesThroughSnapshotWriteAndCopy() {
        val steps = listOf(P.SNAPSHOT to (0 to 1)) + (0..25).map { P.WRITE to (it to 25) } + (0..100 step 10).map { P.COPY to (it to 100) }
        val percents = steps.map { (phase, n) -> BackupProgressText.percent(phase, n.first, n.second)!! }
        assertEquals(percents, percents.sorted())
        assertEquals(0, percents.first()); assertEquals(100, percents.last())
    }

    @Test fun checkPercentRisesAndRestoreIsIndeterminate() {
        assertEquals(0, BackupProgressText.percent(P.EXTRACT, 0, 0))
        assertTrue(BackupProgressText.percent(P.VALIDATE, 10, 25)!! in 31..84)
        assertEquals(85, BackupProgressText.percent(P.REFERENCES, 0, 1))
        assertNull(BackupProgressText.percent(P.RESTORE, 0, 0))
        assertNull(BackupProgressText.percent(P.SETTINGS, 0, 0))
    }

    @Test fun germanTextsNameTheStep() {
        assertEquals("Tabelle 7 von 25 wird geschrieben … · 36 %", BackupProgressText.text(GermanStrings, P.WRITE, 7, 25))
        assertEquals("Eintrag 3 von 25 wird geprüft … · 36 %", BackupProgressText.text(GermanStrings, P.VALIDATE, 3, 25))
        assertEquals("Daten werden eingespielt (kann eine Minute dauern) …", BackupProgressText.text(GermanStrings, P.RESTORE, 0, 0))
    }
}

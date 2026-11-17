package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class CirqaTestLaboratoryTest {
    @Test fun initialHistoryStateIsExplicitlyUnprovenAndNotTested() {
        val history = CirqaTestLaboratory.initialRecords()
            .single { it.test == CirqaTestLaboratory.TestId.HISTORICAL_DOWNLOAD }
        assertEquals(CirqaTestLaboratory.Verdict.NOT_TESTED, history.verdict)
        assertEquals(CirqaTestLaboratory.EvidenceLevel.UNPROVEN, history.evidence)
        assertTrue(history.safeSummary.contains("Gesperrt"))
        val passiveProtocol = CirqaTestLaboratory.initialRecords()
            .single { it.test == CirqaTestLaboratory.TestId.PASSIVE_PROTOCOL }
        assertEquals(CirqaTestLaboratory.Verdict.NOT_TESTED, passiveProtocol.verdict)
        assertEquals(CirqaTestLaboratory.EvidenceLevel.UNPROVEN, passiveProtocol.evidence)
        assertTrue(passiveProtocol.safeSummary.contains("keine Anfrage"))
    }

    @Test fun reportAlwaysStatesHistoryGateAndContainsOnlyTheSafeSummary() {
        val records = CirqaTestLaboratory.initialRecords().map {
            if (it.test == CirqaTestLaboratory.TestId.PASSIVE_SCAN) it.copy(
                verdict = CirqaTestLaboratory.Verdict.PASSED,
                evidence = CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED,
                checkedAtEpochMillis = 1_700_000_000_000L,
                safeSummary = "Scan abgeschlossen; Ereignisse=3."
            ) else it
        }
        val report = CirqaTestLaboratory.report(records, 1_700_000_001_000L)
        assertTrue(report.contains("NICHT FREIGEGEBEN"))
        assertTrue(report.contains("keine Gerätekennung"))
        assertTrue(report.contains("Passiver BLE-Scan: Bestanden"))
        assertFalse(report.contains("00:11:22:33:44:55"))
        assertFalse(report.contains("123 bpm"))
    }

    @Test fun safReaderAcceptsAtLimitAndRejectsOneByteOver() {
        val atLimit = ByteArray(8) { it.toByte() }
        assertArrayEquals(atLimit, BoundedFitInput.read(ByteArrayInputStream(atLimit), 8))
        assertNull(BoundedFitInput.read(ByteArrayInputStream(atLimit + byteArrayOf(9)), 8))
        assertArrayEquals(byteArrayOf(1, 2, 3),
            BoundedFitInput.read(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 8))
    }
}

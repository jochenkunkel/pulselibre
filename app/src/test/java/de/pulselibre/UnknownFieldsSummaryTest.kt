package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnknownFieldsSummaryTest {
    private fun field(
        sha: String = "a".repeat(64), global: Int = 20, kind: StoredFitFieldKind = StoredFitFieldKind.STANDARD, number: Int = 85,
        base: Int? = 134, size: Int = 4, dev: Int? = null, cls: String = "FIELD_NOT_DECODED_BY_APP", messages: Long = 3,
        status: FitFieldInventoryStatus = FitFieldInventoryStatus.UNREVIEWED
    ) = StoredFitUnmappedField(sha, global, kind, number, base, size, dev, cls, 1, messages, null, null, "d1", "p1", status)

    @Test fun emptyInventoryProducesNoHint() {
        val s = UnknownFieldsSummary.summarize(emptyList())
        assertTrue(s.isEmpty)
        assertEquals(0, s.typeCount)
    }

    @Test fun sameFieldInSeveralFilesIsOneTypeWithSummedCounters() {
        val s = UnknownFieldsSummary.summarize(listOf(field(sha = "a".repeat(64), messages = 3), field(sha = "b".repeat(64), messages = 5)))
        assertEquals(1, s.typeCount)
        assertEquals("Unbekanntes Datenfeld entdeckt (1 Typ)", s.headline)
        assertEquals("8 Meldungen in 2 Dateien", s.entries.single().occurrence)
        assertEquals("FIT-Nachricht 20 · Feld 85", s.entries.single().title)
        assertEquals("Basistyp 134 · 4 Byte", s.entries.single().detail)
    }

    @Test fun differentCoordinatesAreDifferentTypesAndDeveloperFieldsAreLabelled() {
        val s = UnknownFieldsSummary.summarize(listOf(
            field(number = 85), field(number = 86),
            field(kind = StoredFitFieldKind.DEVELOPER, number = 1, base = null, dev = 2, cls = "DEVELOPER_FIELD_NOT_DECODED_BY_APP")
        ))
        assertEquals(3, s.typeCount)
        assertEquals("Unbekannte Datenfelder entdeckt (3 Typen)", s.headline)
        val dev = s.entries.single { it.title.contains("Developer-Feld") }
        assertEquals("Basistyp unbekannt · 4 Byte · Developer-Index 2", dev.detail)
        assertEquals("Developer-Feld nicht dekodiert", dev.classification)
    }

    @Test fun textsNeverClaimMeasurementsOrDiagnoses() {
        val s = UnknownFieldsSummary.summarize(listOf(field()))
        val all = (listOf(s.headline, s.explanation) + s.entries.flatMap { listOf(it.title, it.detail, it.occurrence, it.classification, it.status) })
            .joinToString(" ").lowercase()
        assertFalse(all.contains("blutdruck"))
        assertTrue(s.explanation.contains("keine Messwerte"))
    }

    @Test fun listIsBoundedAndReportsOmittedTypes() {
        val many = (0 until UnknownFieldsSummary.MAX_ENTRIES + 5).map { field(number = it % 256, global = 1000 + it) }
        val s = UnknownFieldsSummary.summarize(many)
        assertEquals(UnknownFieldsSummary.MAX_ENTRIES + 5, s.typeCount)
        assertEquals(UnknownFieldsSummary.MAX_ENTRIES, s.entries.size)
        assertEquals(5, s.omitted)
    }

    @Test fun statusIsShownInGerman() {
        val s = UnknownFieldsSummary.summarize(listOf(field(status = FitFieldInventoryStatus.INVESTIGATING)))
        assertEquals("in Prüfung", s.entries.single().status)
    }
}

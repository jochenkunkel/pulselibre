package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class RawDataExportTest {
    private fun record(time: Long, id: String = "band") =
        RawExportRecord(id, "spo2", time, 97.0, "%", "sample-1", time + 1000)

    @Test fun windowIncludesStartAndExcludesEnd() {
        val csv = exportRawCsv(listOf(record(1000), record(2000), record(3000)), ExportWindow(1000, 3000))
        assertEquals(3, csv.trim().split("\r\n").size)
        assertTrue(csv.contains("1970-01-01T00:00:01Z"))
        assertFalse(csv.contains("1970-01-01T00:00:03Z"))
    }
    @Test fun emptyExportStillHasHeader() {
        assertEquals(1, exportRawCsv(emptyList(), ExportWindow(0, 1)).trim().split("\r\n").size)
    }
    @Test fun invalidWindowRejected() {
        try { ExportWindow(100, 100); fail("Expected exception") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun csvEscapesCommasQuotesAndSpreadsheetFormulas() {
        val csv = exportRawCsv(listOf(record(100, "=2+2,\"x\"")), ExportWindow(0, 200))
        assertTrue(csv.contains("\"'=2+2,\"\"x\"\"\""))
    }
    @Test fun nonFiniteValuesAreExcluded() {
        val invalid = record(100).copy(value = Double.NaN)
        assertEquals(1, exportRawCsv(listOf(invalid), ExportWindow(0, 200)).trim().split("\r\n").size)
    }
    @Test fun exportIsChronologicalAndPreservesRepeatedSamples() {
        val csv = exportRawCsv(listOf(record(200), record(100), record(200)), ExportWindow(0, 300))
        assertEquals(4, csv.trim().split("\r\n").size)
        assertTrue(csv.indexOf("1970-01-01T00:00:00.100Z") < csv.indexOf("1970-01-01T00:00:00.200Z"))
    }
}

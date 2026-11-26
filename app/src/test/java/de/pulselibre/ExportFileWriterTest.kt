package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExportFileWriterTest {
    @Test fun writesUtf8HeaderAndOnlyRequestedMeasurements() {
        val out = ByteArrayOutputStream()
        writeRawExport(out, listOf(
            RawExportRecord("gerät-1", "hr", 1000, 70.0, "bpm"),
            RawExportRecord("gerät-1", "hr", 3000, 80.0, "bpm")
        ), ExportWindow(1000, 3000))
        val csv = out.toString("UTF-8")
        assertTrue(csv.contains("gerät-1"))
        assertTrue(csv.contains("70.0"))
        assertFalse(csv.contains("80.0"))
        assertEquals(2, csv.trim().split("\r\n").size)
    }

    @Test fun emptySelectionStillWritesHeader() {
        val out = ByteArrayOutputStream()
        writeRawExport(out, emptyList(), ExportWindow(0, 1))
        assertTrue(out.toString("UTF-8").startsWith("device_id,metric,"))
        assertEquals(1, out.toString("UTF-8").trim().split("\r\n").size)
    }

    @Test fun streamingWriterIsByteIdenticalToTheStringExport() {
        val records = (0 until 2_000).map { RawExportRecord("CIRQA", if (it % 3 == 0) "stress" else "bpm", 1_000L + it * 997L % 50_000L,
            if (it % 7 == 0) -100.0 else 60.0 + it % 40, "u", "s$it", if (it % 2 == 0) null else 5_000L + it) }
        val window = ExportWindow(0, 100_000)
        val out = ByteArrayOutputStream()
        writeRawExport(out, records, window)
        assertArrayEquals(exportRawCsv(records, window).toByteArray(Charsets.UTF_8), out.toByteArray())
    }
}

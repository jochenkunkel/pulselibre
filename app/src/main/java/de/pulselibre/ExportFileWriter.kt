package de.pulselibre

import java.io.OutputStream

/** Writes the complete CSV, including header for empty selections, as UTF-8. */
fun writeRawExport(output: OutputStream, records: List<RawExportRecord>, window: ExportWindow) {
    output.write(exportRawCsv(records, window).toByteArray(Charsets.UTF_8))
    output.flush()
}

package de.pulselibre

import java.io.OutputStream

/**
 * Writes the complete CSV, including header for empty selections, as UTF-8, row by row: the whole file is never built
 * as one string in memory (30 days of 1 Hz data are millions of rows). Output is byte-identical to [exportRawCsv].
 */
fun writeRawExport(output: OutputStream, records: List<RawExportRecord>, window: ExportWindow) {
    val writer = output.bufferedWriter(Charsets.UTF_8)
    writer.write(RAW_EXPORT_HEADER); writer.write("\r\n")
    for (row in exportRows(records, window)) { writer.write(row); writer.write("\r\n") }
    writer.flush()
}

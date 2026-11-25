package de.pulselibre

import java.time.Instant

/** Pure CSV exporter. No Android dependency, analytics, or data alteration. */
data class RawExportRecord(
    val deviceId: String,
    val metric: String,
    val measuredAtEpochMillis: Long,
    val value: Double,
    val unit: String,
    val sampleId: String? = null,
    val receivedAtEpochMillis: Long? = null
)

data class ExportWindow(val startInclusiveEpochMillis: Long, val endExclusiveEpochMillis: Long) {
    init {
        require(startInclusiveEpochMillis >= 0 && endExclusiveEpochMillis > startInclusiveEpochMillis) {
            "Export interval must be nonempty and valid"
        }
    }
}

/**
 * Text fields are guarded against spreadsheet formula injection (leading =, +, -, @, tab, CR get an apostrophe).
 * Numbers the app formats itself ([numeric]) are never prefixed: a negative device status code such as -100 must stay a
 * number in the export, not become the text '-100.
 */
private fun csvField(value: String, numeric: Boolean = false): String {
    val safe = if (!numeric && (value.startsWith("=") || value.startsWith("+") ||
        value.startsWith("-") || value.startsWith("@") ||
        value.startsWith("\t") || value.startsWith("\r"))) "'" + value else value
    return "\"" + safe.replace("\"", "\"\"") + "\""
}

internal const val RAW_EXPORT_HEADER = "device_id,metric,measured_at_utc,measured_at_epoch_ms,value,unit,sample_id,received_at_utc"

/** Valid records of [window] in export order; shared by [exportRawCsv] and the streaming writer. */
internal fun exportRows(records: List<RawExportRecord>, window: ExportWindow): Sequence<String> = records.asSequence()
    .filter { it.measuredAtEpochMillis in window.startInclusiveEpochMillis until window.endExclusiveEpochMillis }
    .filter { it.deviceId.isNotBlank() && it.metric.isNotBlank() && it.unit.isNotBlank() &&
        it.measuredAtEpochMillis >= 0 && it.value.isFinite() &&
        (it.receivedAtEpochMillis == null || it.receivedAtEpochMillis >= 0) }
    .sortedWith(compareBy<RawExportRecord> { it.measuredAtEpochMillis }
        .thenBy { it.deviceId }.thenBy { it.metric }.thenBy { it.sampleId ?: "" })
    .map { record ->
        listOf(
            csvField(record.deviceId), csvField(record.metric),
            csvField(Instant.ofEpochMilli(record.measuredAtEpochMillis).toString()),
            csvField(record.measuredAtEpochMillis.toString(), numeric = true), csvField(record.value.toString(), numeric = true),
            csvField(record.unit), csvField(record.sampleId ?: ""),
            csvField(record.receivedAtEpochMillis?.let { Instant.ofEpochMilli(it).toString() } ?: "")
        ).joinToString(",")
    }

fun exportRawCsv(records: List<RawExportRecord>, window: ExportWindow): String =
    (sequenceOf(RAW_EXPORT_HEADER) + exportRows(records, window)).joinToString("\r\n", postfix = "\r\n")

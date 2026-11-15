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

private fun csvField(value: String): String {
    val safe = if (value.startsWith("=") || value.startsWith("+") ||
        value.startsWith("-") || value.startsWith("@") ||
        value.startsWith("\t") || value.startsWith("\r")) "'" + value else value
    return "\"" + safe.replace("\"", "\"\"") + "\""
}

fun exportRawCsv(records: List<RawExportRecord>, window: ExportWindow): String {
    val header = "device_id,metric,measured_at_utc,measured_at_epoch_ms,value,unit,sample_id,received_at_utc"
    val rows = records.asSequence()
        .filter { it.measuredAtEpochMillis in window.startInclusiveEpochMillis until window.endExclusiveEpochMillis }
        .filter { it.deviceId.isNotBlank() && it.metric.isNotBlank() && it.unit.isNotBlank() &&
            it.measuredAtEpochMillis >= 0 && it.value.isFinite() &&
            (it.receivedAtEpochMillis == null || it.receivedAtEpochMillis >= 0) }
        .sortedWith(compareBy<RawExportRecord> { it.measuredAtEpochMillis }
            .thenBy { it.deviceId }.thenBy { it.metric }.thenBy { it.sampleId ?: "" })
        .map { record ->
            listOf(
                record.deviceId, record.metric,
                Instant.ofEpochMilli(record.measuredAtEpochMillis).toString(),
                record.measuredAtEpochMillis.toString(), record.value.toString(),
                record.unit, record.sampleId ?: "",
                record.receivedAtEpochMillis?.let { Instant.ofEpochMilli(it).toString() } ?: ""
            ).joinToString(",") { csvField(it) }
        }.toList()
    return (listOf(header) + rows).joinToString("\r\n", postfix = "\r\n")
}

package de.pulselibre

/** Device timestamps represent measurement time, never Bluetooth transfer time. */
data class HistoricalReading(
    val deviceId: String,
    val metric: String,
    val measuredAtEpochMillis: Long,
    val value: Double,
    val sampleId: String? = null
)

/** Immutable, deterministic merge. No Garmin protocol behavior is assumed. */
fun mergeHistoricalReadings(
    existing: List<HistoricalReading>,
    incoming: List<HistoricalReading>
): List<HistoricalReading> {
    val result = linkedMapOf<String, HistoricalReading>()
    (existing + incoming).forEach { reading ->
        if (reading.deviceId.isBlank() || reading.metric.isBlank() ||
            reading.measuredAtEpochMillis < 0 || !reading.value.isFinite()) return@forEach
        val key = listOf(reading.deviceId, reading.metric, reading.measuredAtEpochMillis.toString(),
            reading.sampleId ?: "").joinToString("|")
        result[key] = reading
    }
    return result.values.sortedWith(compareBy<HistoricalReading> { it.measuredAtEpochMillis }
        .thenBy { it.deviceId }.thenBy { it.metric }.thenBy { it.sampleId ?: "" })
}

/** A sync cursor only advances after a complete, acknowledged batch is durably stored. */
data class SyncCursor(val lastCommittedEpochMillis: Long? = null)

fun commitSyncedBatch(cursor: SyncCursor, committed: List<HistoricalReading>): SyncCursor {
    val newest = committed.maxOfOrNull { it.measuredAtEpochMillis } ?: return cursor
    return SyncCursor(maxOf(cursor.lastCommittedEpochMillis ?: newest, newest))
}

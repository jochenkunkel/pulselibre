package de.pulselibre

import android.content.Context
import java.time.Instant

/** Stable, schema-independent observation returned to dashboard and analysis consumers. */
data class HistoricalObservation(
    val deviceId: String,
    val metric: String,
    val measuredAt: Instant,
    val value: Double,
    val unit: String,
    val sampleId: String,
    val receivedAt: Instant?,
    val provenance: Provenance?
) {
    data class Provenance(
        val sourceFileSha256: String,
        val recordOrdinal: Int,
        val globalMessageNumber: Int,
        val fieldNumber: Int,
        val quality: String,
        val decoderVersion: String
    )
}

data class HistoricalObservationQuery(
    val startInclusive: Instant,
    val endExclusive: Instant,
    val metric: String? = null,
    val deviceId: String? = null,
    val limit: Int = 1_000,
    val after: Cursor? = null
) {
    data class Cursor(
        val measuredAtEpochMillis: Long,
        val deviceId: String,
        val metric: String,
        val sampleId: String
    )

    init {
        require(startInclusive.isBefore(endExclusive)) { "Observation window must be nonempty" }
        require(limit in 1..5_000) { "Page size must be between 1 and 5000" }
        require(metric == null || metric.isNotBlank()) { "Metric must not be blank" }
        require(deviceId == null || deviceId.isNotBlank()) { "Device ID must not be blank" }
    }
}

data class HistoricalObservationPage(
    val observations: List<HistoricalObservation>,
    val next: HistoricalObservationQuery.Cursor?
)

/**
 * Read-only, time-bounded access to canonical historical observations.
 *
 * The repository performs the additive legacy copy before reading, so callers
 * need not know which SQLite file originally held a migrated observation.
 */
class HistoricalObservationRepository(private val context: Context) {
    fun read(query: HistoricalObservationQuery): HistoricalObservationPage =
        CirqaDatabase(context).use { database ->
            database.ensureHistoricalStoreCopied()
            val where = StringBuilder("o.measured_at_ms >= ? AND o.measured_at_ms < ?")
            val args = mutableListOf(
                query.startInclusive.toEpochMilli().toString(),
                query.endExclusive.toEpochMilli().toString()
            )
            query.metric?.let {
                where.append(" AND o.metric = ?")
                args += it
            }
            query.deviceId?.let {
                where.append(" AND o.device_id = ?")
                args += it
            }
            query.after?.let { cursor ->
                where.append(
                    """ AND (o.measured_at_ms > ? OR
                        (o.measured_at_ms = ? AND (o.device_id > ? OR
                            (o.device_id = ? AND (o.metric > ? OR
                                (o.metric = ? AND o.sample_id > ?))))))"""
                )
                args += cursor.measuredAtEpochMillis.toString()
                args += cursor.measuredAtEpochMillis.toString()
                args += cursor.deviceId
                args += cursor.deviceId
                args += cursor.metric
                args += cursor.metric
                args += cursor.sampleId
            }
            args += (query.limit + 1).toString()

            val rows = database.readableDatabase.rawQuery(
                """SELECT o.device_id, o.metric, o.measured_at_ms, o.value, o.unit,
                          o.sample_id, o.received_at_ms,
                          p.source_sha256, p.record_ordinal, p.global_message_number,
                          p.field_number, p.quality, p.decoder_version
                   FROM observations AS o
                   LEFT JOIN fit_observation_provenance AS p
                     ON p.device_id = o.device_id AND p.metric = o.metric
                    AND p.measured_at_ms = o.measured_at_ms AND p.sample_id = o.sample_id
                   WHERE $where
                   ORDER BY o.measured_at_ms, o.device_id, o.metric, o.sample_id
                   LIMIT ?""",
                args.toTypedArray()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val provenance = if (cursor.isNull(7)) null else
                            HistoricalObservation.Provenance(
                                sourceFileSha256 = cursor.getString(7),
                                recordOrdinal = cursor.getInt(8),
                                globalMessageNumber = cursor.getInt(9),
                                fieldNumber = cursor.getInt(10),
                                quality = cursor.getString(11),
                                decoderVersion = cursor.getString(12)
                            )
                        add(HistoricalObservation(
                            deviceId = cursor.getString(0),
                            metric = cursor.getString(1),
                            measuredAt = Instant.ofEpochMilli(cursor.getLong(2)),
                            value = cursor.getDouble(3),
                            unit = cursor.getString(4),
                            sampleId = cursor.getString(5),
                            receivedAt = if (cursor.isNull(6)) null else Instant.ofEpochMilli(cursor.getLong(6)),
                            provenance = provenance
                        ))
                    }
                }
            }
            val hasMore = rows.size > query.limit
            val page = rows.take(query.limit)
            val next = if (hasMore) page.last().let {
                HistoricalObservationQuery.Cursor(
                    measuredAtEpochMillis = it.measuredAt.toEpochMilli(),
                    deviceId = it.deviceId,
                    metric = it.metric,
                    sampleId = it.sampleId
                )
            } else null
            HistoricalObservationPage(page, next)
        }
}

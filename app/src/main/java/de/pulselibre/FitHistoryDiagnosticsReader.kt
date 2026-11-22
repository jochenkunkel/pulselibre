package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Read-only, value-free summary of FIT observations for diagnosing an empty
 * history chart. It deliberately opens SQLite directly in read-only mode
 * instead of using [CirqaDatabase], whose helper may migrate the database.
 */
internal data class FitMetricCoverage(
    val metric: String,
    val unit: String,
    val count: Long,
    val firstMeasuredAt: Instant?,
    val lastMeasuredAt: Instant?
)

internal data class FitHeartRateWindows(
    val total: Long,
    val last24Hours: Long,
    val last7Days: Long,
    val last30Days: Long,
    val firstMeasuredAt: Instant?,
    val lastMeasuredAt: Instant?
)

internal data class FitHistoryDiagnostics(
    val available: Boolean,
    val sourceCount: Long?,
    val metrics: List<FitMetricCoverage>,
    val heartRate: FitHeartRateWindows?
) {
    companion object {
        fun unavailable() = FitHistoryDiagnostics(
            available = false,
            sourceCount = null,
            metrics = emptyList(),
            heartRate = null
        )
    }
}

internal object FitHistoryDiagnosticsReader {
    private const val DATABASE_NAME = "cirqa_measurements.db"

    fun read(context: Context, now: Instant = Instant.now()): FitHistoryDiagnostics {
        val path = context.getDatabasePath(DATABASE_NAME)
        if (!path.isFile || path.length() == 0L) return FitHistoryDiagnostics.unavailable()
        return try {
            SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                read(db, now)
            }
        } catch (_: Exception) {
            FitHistoryDiagnostics.unavailable()
        }
    }

    internal fun read(database: SQLiteDatabase, now: Instant): FitHistoryDiagnostics {
        val tables = database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'", null
        ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }

        val source = when {
            "fit_generation_observations" in tables && "fit_active_generations" in tables ->
                """SELECT g.metric, g.unit, g.measured_at_ms, g.value
                   FROM fit_generation_observations g
                   JOIN fit_active_generations a ON a.generation_id = g.generation_id"""
            "fit_observation_provenance" in tables && "observations" in tables ->
                """SELECT p.metric, o.unit, p.measured_at_ms, o.value
                   FROM fit_observation_provenance p
                   JOIN observations o ON o.sample_id = p.sample_id
                     AND o.device_id = p.device_id AND o.metric = p.metric
                     AND o.measured_at_ms = p.measured_at_ms"""
            else -> return FitHistoryDiagnostics.unavailable()
        }

        val metrics = database.rawQuery(
            """SELECT metric, unit, COUNT(*), MIN(measured_at_ms), MAX(measured_at_ms)
               FROM ($source)
               GROUP BY metric, unit
               ORDER BY metric, unit""",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FitMetricCoverage(
                            metric = cursor.getString(0),
                            unit = cursor.getString(1),
                            count = cursor.getLong(2),
                            firstMeasuredAt = cursor.getLong(3).takeIf { !cursor.isNull(3) }
                                ?.let(Instant::ofEpochMilli),
                            lastMeasuredAt = cursor.getLong(4).takeIf { !cursor.isNull(4) }
                                ?.let(Instant::ofEpochMilli)
                        )
                    )
                }
            }
        }

        val nowMs = now.toEpochMilli()
        fun countHeartRate(startMs: Long?): Long {
            val timePredicate = if (startMs == null) "measured_at_ms < ?" else
                "measured_at_ms >= ? AND measured_at_ms < ?"
            val args = if (startMs == null) {
                arrayOf(FitHistoryMapping.METRIC, "bpm", nowMs.toString())
            } else {
                arrayOf(FitHistoryMapping.METRIC, "bpm", startMs.toString(), nowMs.toString())
            }
            return database.rawQuery(
                """SELECT COUNT(*) FROM ($source)
                   WHERE metric = ? AND unit = ? AND value >= 0.5 AND value < 240.5
                     AND $timePredicate""",
                args
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        }
        val hrBounds = database.rawQuery(
            """SELECT MIN(measured_at_ms), MAX(measured_at_ms) FROM ($source)
               WHERE metric = ? AND unit = ? AND value >= 0.5 AND value < 240.5
                 AND measured_at_ms < ?""",
            arrayOf(FitHistoryMapping.METRIC, "bpm", nowMs.toString())
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0) && !cursor.isNull(1)) {
                Instant.ofEpochMilli(cursor.getLong(0)) to Instant.ofEpochMilli(cursor.getLong(1))
            } else null
        }

        val sourceCount = when {
            "fit_active_generations" in tables ->
                database.rawQuery("SELECT COUNT(*) FROM fit_active_generations", null)
                    .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            "fit_sources" in tables ->
                database.rawQuery("SELECT COUNT(*) FROM fit_sources", null)
                    .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            else -> null
        }

        val heartRateCount = countHeartRate(null)
        val heartRate = if (heartRateCount > 0L && hrBounds != null) {
            FitHeartRateWindows(
                total = heartRateCount,
                last24Hours = countHeartRate(now.minus(Duration.ofHours(24)).toEpochMilli()),
                last7Days = countHeartRate(now.minus(Duration.ofDays(7)).toEpochMilli()),
                last30Days = countHeartRate(now.minus(Duration.ofDays(30)).toEpochMilli()),
                firstMeasuredAt = hrBounds.first,
                lastMeasuredAt = hrBounds.second
            )
        } else null
        return FitHistoryDiagnostics(
            available = true,
            sourceCount = sourceCount,
            metrics = metrics,
            heartRate = heartRate
        )
    }
}

package de.pulselibre

import android.database.sqlite.SQLiteDatabase

/** Which stored series feeds one dashboard card and how its stored values are classified. */
data class ScienceSeriesSpec(
    val key: String,
    val origin: MeasurementOrigin,
    val metric: String,
    val unit: String,
    val seriesOrigin: SeriesOrigin,
    val timeBasis: SeriesTimeBasis,
    /** SQL predicate on `value` for values that count as measurements. */
    val validSql: String,
    /** Quality given to values matching [validSql] (SpO2 stays UNVERIFIED: numerically valid is not quality assured). */
    val validQuality: SeriesQuality,
    /** SQL predicate on `value` for documented status codes (not measurements, not invalid readings). */
    val statusSql: String? = null
)

/** Reads real series from `unified_measurements` as bucketed points (min/mean/max/count), so 30 days of 1 Hz data stay cheap. */
object ScienceSeriesLoader {
    const val TARGET_BUCKETS = 1000L
    private const val MAX_FLAGGED = 3000

    val HEART_RATE_BAND = ScienceSeriesSpec("hr_band", MeasurementOrigin.BAND, "heart_rate", "bpm", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 1 AND value <= 240", SeriesQuality.VALID)
    /** Band heart rate from FIT `record` messages, i.e. recorded activities; [HEART_RATE_BAND] is the all-day history. */
    val HEART_RATE_BAND_ACTIVITY = ScienceSeriesSpec("hr_band_activity", MeasurementOrigin.BAND, "heart_rate", "bpm", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 1 AND value <= 240", SeriesQuality.VALID)
    /** FIT-only (verified via [ScienceFitSeriesAdapter]); never read from unified_measurements. */
    val DISTANCE_BAND = ScienceSeriesSpec("distance_band", MeasurementOrigin.BAND, "distance", "m", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 0", SeriesQuality.VALID)
    val SPEED_BAND = ScienceSeriesSpec("speed_band", MeasurementOrigin.BAND, "speed", "m/s", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 0", SeriesQuality.VALID)
    // #696: canonical FIT values (Garmin profile 21.222.0). The decoder already dropped status codes, so these
    // predicates only guard the stored range; SpO2 stays UNVERIFIED (numerically valid is not quality assured).
    val SPO2_BAND = ScienceSeriesSpec("spo2_band", MeasurementOrigin.BAND, "spo2", "percent", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 1 AND value <= 100", SeriesQuality.UNVERIFIED)
    val RESPIRATION_BAND = ScienceSeriesSpec("resp_band", MeasurementOrigin.BAND, "respiration", "breaths/min", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value > 0 AND value <= 100", SeriesQuality.VALID)
    val STRESS_BAND = ScienceSeriesSpec("stress_band", MeasurementOrigin.BAND, "stress", "device_scale", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 1 AND value <= 100", SeriesQuality.VALID)
    // #150: device values from FIT hrv_value (371) and monitoring_hr_data (211); shown as written by the band.
    val HRV_BAND = ScienceSeriesSpec("hrv_band", MeasurementOrigin.BAND, "hrv", "ms", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value > 0 AND value <= 300", SeriesQuality.UNVERIFIED)
    val RESTING_HR_BAND = ScienceSeriesSpec("rhr_band", MeasurementOrigin.BAND, "resting_heart_rate", "bpm", SeriesOrigin.BAND,
        SeriesTimeBasis.BAND_CLOCK, "value >= 1 AND value <= 240", SeriesQuality.VALID)
    val HEART_RATE_LIVE = ScienceSeriesSpec("hr_live", MeasurementOrigin.LIVE, "bpm", "bpm", SeriesOrigin.LIVE,
        SeriesTimeBasis.PHONE_RECEPTION, "value >= 1 AND value <= 240", SeriesQuality.VALID)
    val SPO2_LIVE = ScienceSeriesSpec("spo2_live", MeasurementOrigin.LIVE, "spo2_percent", "%", SeriesOrigin.LIVE,
        SeriesTimeBasis.PHONE_RECEPTION, "value >= 1 AND value <= 100", SeriesQuality.UNVERIFIED)
    val RESPIRATION_LIVE = ScienceSeriesSpec("resp_live", MeasurementOrigin.LIVE, "breaths_per_min", "breaths/min", SeriesOrigin.LIVE,
        SeriesTimeBasis.PHONE_RECEPTION, "value >= 1 AND value <= 100", SeriesQuality.VALID, statusSql = "value IN (-100, -200, -300)")
    val STRESS_LIVE = ScienceSeriesSpec("stress_live", MeasurementOrigin.LIVE, "stress", "device_scale", SeriesOrigin.LIVE,
        SeriesTimeBasis.PHONE_RECEPTION, "value >= 0 AND value <= 100", SeriesQuality.VALID)

    /** Loads [spec] for [startMs, endMs]; an unreadable table yields the exception to the caller (shown as an error state). */
    fun load(db: SQLiteDatabase, spec: ScienceSeriesSpec, startMs: Long, endMs: Long): SeriesData {
        val span = (endMs - startMs).coerceAtLeast(1L)
        val bucketMs = (span / TARGET_BUCKETS).coerceAtLeast(1_000L)
        val points = ArrayList<SeriesPoint>()
        val args = arrayOf(spec.origin.code, spec.metric, spec.unit, startMs.toString(), endMs.toString())
        val where = "origin = ? AND metric = ? AND unit = ? AND measured_at_ms >= ? AND measured_at_ms <= ?"
        db.rawQuery(
            """SELECT (measured_at_ms - $startMs) / $bucketMs AS b, COUNT(*), MIN(value), MAX(value), AVG(value)
               FROM unified_measurements WHERE $where AND (${spec.validSql}) GROUP BY b ORDER BY b""", args
        ).use { c ->
            while (c.moveToNext()) {
                val t = startMs + c.getLong(0) * bucketMs + bucketMs / 2
                points += SeriesPoint(t, c.getDouble(4), spec.validQuality, null, c.getInt(1), c.getDouble(2), c.getDouble(3))
            }
        }
        // Everything that is neither a measurement nor a documented status code is an invalid reading (e.g. heart rate 0).
        val statusCase = spec.statusSql?.let { "CASE WHEN $it THEN 1 ELSE 0 END" } ?: "0"
        db.rawQuery(
            """SELECT measured_at_ms, value, $statusCase FROM unified_measurements
               WHERE $where AND NOT (${spec.validSql}) ORDER BY measured_at_ms LIMIT $MAX_FLAGGED""", args
        ).use { c ->
            while (c.moveToNext()) {
                val quality = if (c.getInt(2) == 1) SeriesQuality.STATUS else SeriesQuality.INVALID
                points += SeriesPoint(c.getLong(0), null, quality, c.getDouble(1).toLong())
            }
        }
        return SeriesData(spec.metric, spec.unit, spec.seriesOrigin, spec.timeBasis, startMs, endMs, null, points)
    }
}

package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Read-only view of the canonical CIRQA SQLite database. Never modifies the running
 * collection or invents missing readings. Reads stored SQLite events only.
 */
class CirqaDashboardReader(private val context: Context) {
    data class Sample(val value: String, val receivedAt: Instant)
    data class Summary(
        val metrics: List<DashboardMetric>,
        val samples: Int,
        val bytes: Long,
        val activity: StepActivityEstimate
    )

    fun read(now: Instant = Instant.now(), includeTotals: Boolean = true): Summary {
        val latest = mutableMapOf<Int, Pair<Instant, JSONObject>>()
        val stepObservations = mutableListOf<StepObservation>()
        val zone = ZoneId.systemDefault()
        val settings = ActivityDetectionPreference.read(context)
        val services = GarminRealtime.SUPPORTED_SERVICES.joinToString(",")
        val dayStart = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val windowStart = maxOf(dayStart, now.minusSeconds(settings.windowMinutes * 60L))
        val cutoff = now.plusSeconds(60)
        val records = mutableListOf<String>()
        val totals = CirqaDatabase(context).use { db ->
            val sql = db.readableDatabase
            val eligible = "kind = 'reading' AND service IN ($services) AND dashboard_time <= ?"
            for (service in GarminRealtime.SUPPORTED_SERVICES) {
                val query = """SELECT payload_json FROM events WHERE service = ? AND kind = 'reading'
                    AND dashboard_time <= ? ORDER BY dashboard_time DESC, id ASC LIMIT 1"""
                sql.rawQuery(query, arrayOf(service.toString(), CirqaDatabase.timeKey(cutoff))).use { cursor ->
                    if (cursor.moveToFirst()) records.add(cursor.getString(0))
                }
            }
            sql.rawQuery("""SELECT payload_json FROM events WHERE service = 7 AND kind = 'reading'
                AND dashboard_time BETWEEN ? AND ? ORDER BY id""",
                arrayOf(CirqaDatabase.timeKey(windowStart), CirqaDatabase.timeKey(now))).use { cursor ->
                while (cursor.moveToNext()) records.add(cursor.getString(0))
            }
            if (!includeTotals) return@use 0 to 0L
            val count = sql.rawQuery("SELECT COUNT(*) FROM events WHERE $eligible", arrayOf(CirqaDatabase.timeKey(cutoff))).use {
                it.moveToFirst()
                it.getInt(0)
            }
            count to db.eventBytes()
        }
        records.distinct().forEach { line ->
            val item = try { JSONObject(line) } catch (_: Exception) { return@forEach }
            if (item.optString("type") != "reading" || item.optInt("schema") != 1) return@forEach
            val time = try { Instant.parse(item.getString("received_at_utc")) } catch (_: Exception) { return@forEach }
            if (time.isAfter(now.plusSeconds(60))) return@forEach
            val service = item.optInt("service", -1)
            if (service !in GarminRealtime.SUPPORTED_SERVICES) return@forEach
            if (service == 7 && time.atZone(ZoneId.systemDefault()).toLocalDate() != now.atZone(ZoneId.systemDefault()).toLocalDate()) return@forEach
            if (service == 7 && !time.isAfter(now)) {
                val steps = item.optLong("steps", Long.MIN_VALUE)
                if (steps in 0L..0xFFFF_FFFFL) stepObservations += StepObservation(time.toEpochMilli(), steps)
            }
            if (latest[service]?.first?.isBefore(time) != false) latest[service] = time to item
        }
        val result = DashboardMeasurement.entries.map { measurement ->
            val pair = latest[measurement.service]
            val good = pair?.second?.let(measurement::value)
            val receivedAt = pair?.takeIf { !it.second.isNull(measurement.key) }?.first
            val unit = when (measurement) {
                DashboardMeasurement.STRESS, DashboardMeasurement.BODY_BATTERY -> "/ 100"
                DashboardMeasurement.RESPIRATION -> "/ min"
                else -> measurement.unit
            }
            DashboardMetric(measurement.title, good?.let { "$it $unit".trim() } ?: "Keine Messdaten",
                if (good == null) "Nicht gemessen oder ungültig" else "Quelle: CIRQA-Service ${measurement.service}",
                receivedAt)
        } + listOf(
            DashboardMetric("Schlaf", "Keine Messdaten", "Schlafphasen/-dauer erfordern validierte FIT-Historie"),
            DashboardMetric("Ground Rules", "Quellen & Grenzen", "Keine Diagnosen; keine geschätzten Werte; kein RR→RMSSD ohne Zeitreihe")
        )
        val activity = estimateStepActivity(stepObservations, now, zone, settings)
        return Summary(result, totals.first, totals.second, activity)
    }
}

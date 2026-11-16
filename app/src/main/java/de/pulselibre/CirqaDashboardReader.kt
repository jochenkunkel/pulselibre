package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Read-only view of the canonical CIRQA SQLite database. Never modifies the running
 * collection or invents missing readings. Only complete valid JSONL lines count.
 */
class CirqaDashboardReader(private val context: Context) {
    data class Sample(val value: String, val receivedAt: Instant)
    data class Summary(val metrics: List<DashboardMetric>, val samples: Int, val bytes: Long)

    fun read(now: Instant = Instant.now()): Summary {
        val latest = mutableMapOf<Int, Pair<Instant, JSONObject>>()
        var count = 0
        // Migrate legacy JSONL records idempotently, then read the SQLite index.
        // The original JSONL remains untouched and available for export.
        val records = CirqaDatabase(context).use { db ->
            db.importJsonl()
            db.readingLines()
        }
        records.forEach { line ->
            val item = try { JSONObject(line) } catch (_: Exception) { return@forEach }
            if (item.optString("type") != "reading" || item.optInt("schema") != 1) return@forEach
            val time = try { Instant.parse(item.getString("received_at_utc")) } catch (_: Exception) { return@forEach }
            if (time.isAfter(now.plusSeconds(60))) return@forEach
            val service = item.optInt("service", -1)
            if (service !in GarminRealtime.SUPPORTED_SERVICES) return@forEach
            count++
            if (service == 7 && time.atZone(ZoneId.systemDefault()).toLocalDate() != now.atZone(ZoneId.systemDefault()).toLocalDate()) return@forEach
            if (latest[service]?.first?.isBefore(time) != false) latest[service] = time to item
        }
        val fmt = DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault())
        fun metric(service: Int, title: String, key: String, unit: String, valid: (Long) -> Boolean): DashboardMetric {
            val pair = latest[service]
            val value = pair?.second?.takeIf { it.has(key) && !it.isNull(key) }?.optLong(key, Long.MIN_VALUE)
            val good = value?.takeIf(valid)
            return DashboardMetric(title, good?.let { "$it $unit".trim() } ?: "Keine Messdaten",
                if (good == null) "Nicht gemessen oder ungültig" else
                    "Letzter Empfang: ${fmt.format(pair.first)} · ${if (now.epochSecond - pair.first.epochSecond > 900) "älter als 15 min" else "aktuell empfangen"}")
        }
        val result = listOf(
            metric(6, "Herzfrequenz", "bpm", "bpm") { it in 1..240 },
            metric(6, "Ruhepuls", "resting_bpm", "bpm") { it in 1..240 },
            metric(7, "Schritte (Tageszähler)", "steps", "") { it >= 0 },
            metric(7, "Schrittziel", "goal", "") { it > 0 },
            metric(19, "Sauerstoffsättigung · SpO₂", "spo2_percent", "%") { it in 1..100 },
            metric(12, "RR-Intervall (kein RMSSD)", "rr_ms", "ms") { it > 0 },
            metric(13, "Stress (Geräteskala)", "stress", "/ 100") { it in 0..100 },
            metric(20, "Body Battery (Geräteskala)", "body_battery", "/ 100") { it in 0..100 },
            metric(21, "Atemfrequenz", "breaths_per_min", "/ min") { it in 1..100 },
            metric(8, "Kalorien gesamt (Gerätewert)", "total", "") { it >= 0 },
            metric(8, "Kalorien aktiv (Gerätewert)", "active", "") { it >= 0 },
            metric(10, "Moderate Intensitätsminuten", "moderate", "min") { it >= 0 },
            metric(10, "Intensive Intensitätsminuten", "vigorous", "min") { it >= 0 },
            DashboardMetric("Schlaf", "Keine Messdaten", "Schlafphasen/-dauer erfordern validierte FIT-Historie"),
            DashboardMetric("Ground Rules", "Quellen & Grenzen", "Keine Diagnosen; keine geschätzten Werte; kein RR→RMSSD ohne Zeitreihe")
        )
        val bytes = CirqaDatabase(context).use { it.eventBytes() }
        return Summary(result, count, bytes)
    }
}

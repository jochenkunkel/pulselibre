package de.pulselibre.positiontracking

import java.util.Locale

/**
 * GeoJSON (RFC 7946) of one track (#788): a FeatureCollection with the track as LineString ([lon, lat], WGS84) and one
 * Point feature per measurement carrying time, accuracy and provider. 0 points: no track feature; 1 point: Point.
 */
object PositionGeoJson {
    /** Health series by key ("heartRateBpm", "spo2Percent"): (time ms, value), sorted by time. */
    fun export(session: PositionSession, points: List<PositionPoint>, health: Map<String, List<Pair<Long, Double>>> = emptyMap()): String {
        val sorted = points.filter { it.isValid() }.sortedBy { it.timeMs }
        val features = mutableListOf<String>()
        val summary = PositionTrackSummary.of(sorted, health["heartRateBpm"].orEmpty().filter { (t, _) ->
            sorted.isNotEmpty() && t >= sorted.first().timeMs && t <= sorted.last().timeMs })
        val trackProps = "{\"kind\":\"track\",\"sessionId\":${session.id},\"startMs\":${session.startMs}," +
            "\"endMs\":${session.endMs ?: "null"},\"points\":${sorted.size}" +
            ",\"distanceMeters\":${num(summary.distanceMeters)},\"durationSeconds\":${summary.durationSeconds}" +
            (summary.averageSpeedMps?.let { ",\"averageSpeedMps\":${num(it)}" } ?: "") +
            (summary.averageHeartRate?.let { ",\"averageHeartRateBpm\":${num(it)}" } ?: "") +
            (summary.maxHeartRate?.let { ",\"maxHeartRateBpm\":${num(it)}" } ?: "") +
            (session.label?.let { ",\"label\":${str(it)}" } ?: "") + "}"
        when (sorted.size) {
            0 -> {}
            1 -> features += feature("{\"type\":\"Point\",\"coordinates\":${coord(sorted[0])}}", trackProps)
            else -> features += feature("{\"type\":\"LineString\",\"coordinates\":[${sorted.joinToString(",") { coord(it) }}]}", trackProps)
        }
        sorted.forEach { p ->
            val props = buildList {
                add("\"kind\":\"position\"")
                add("\"timeMs\":${p.timeMs}")
                add("\"time\":${str(java.time.Instant.ofEpochMilli(p.timeMs).toString())}")
                p.accuracyMeters?.let { add("\"accuracyMeters\":${num(it.toDouble())}") }
                p.provider?.let { add("\"provider\":${str(it)}") }
                p.altitudeMeters?.let { add("\"altitudeMeters\":${num(it)}") }
                p.speedMps?.let { add("\"speedMps\":${num(it.toDouble())}") }
                p.bearingDeg?.let { add("\"bearingDeg\":${num(it.toDouble())}") }
                // the health value measured nearest to this position (max. 10 s apart); nothing is interpolated
                health.forEach { (key, series) -> nearest(series, p.timeMs)?.let { add("\"$key\":${num(it)}") } }
            }.joinToString(",", "{", "}")
            features += feature("{\"type\":\"Point\",\"coordinates\":${coord(p)}}", props)
        }
        return "{\"type\":\"FeatureCollection\",\"features\":[${features.joinToString(",")}]}"
    }

    const val MAX_HEALTH_GAP_MS = 10_000L

    internal fun nearest(series: List<Pair<Long, Double>>, timeMs: Long): Double? {
        if (series.isEmpty()) return null
        var lo = 0; var hi = series.size - 1
        while (lo < hi) { val mid = (lo + hi) / 2; if (series[mid].first < timeMs) lo = mid + 1 else hi = mid }
        val best = listOfNotNull(series.getOrNull(lo), series.getOrNull(lo - 1)).minByOrNull { kotlin.math.abs(it.first - timeMs) } ?: return null
        return best.second.takeIf { kotlin.math.abs(best.first - timeMs) <= MAX_HEALTH_GAP_MS }
    }

    private fun feature(geometry: String, props: String) = "{\"type\":\"Feature\",\"geometry\":$geometry,\"properties\":$props}"
    private fun coord(p: PositionPoint) = "[${num(p.longitude)},${num(p.latitude)}]"
    private fun num(v: Double): String = String.format(Locale.ROOT, "%.7f", v).trimEnd('0').trimEnd('.')
    private fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

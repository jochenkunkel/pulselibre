package de.pulselibre.positiontracking

import java.util.Locale

/**
 * GeoJSON (RFC 7946) of one track (#788): a FeatureCollection with the track as LineString ([lon, lat], WGS84) and one
 * Point feature per measurement carrying time, accuracy and provider. 0 points: no track feature; 1 point: Point.
 */
object PositionGeoJson {
    fun export(session: PositionSession, points: List<PositionPoint>): String {
        val sorted = points.filter { it.isValid() }.sortedBy { it.timeMs }
        val features = mutableListOf<String>()
        val trackProps = "{\"kind\":\"track\",\"sessionId\":${session.id},\"startMs\":${session.startMs}," +
            "\"endMs\":${session.endMs ?: "null"},\"points\":${sorted.size}" +
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
            }.joinToString(",", "{", "}")
            features += feature("{\"type\":\"Point\",\"coordinates\":${coord(p)}}", props)
        }
        return "{\"type\":\"FeatureCollection\",\"features\":[${features.joinToString(",")}]}"
    }

    private fun feature(geometry: String, props: String) = "{\"type\":\"Feature\",\"geometry\":$geometry,\"properties\":$props}"
    private fun coord(p: PositionPoint) = "[${num(p.longitude)},${num(p.latitude)}]"
    private fun num(v: Double): String = String.format(Locale.ROOT, "%.7f", v).trimEnd('0').trimEnd('.')
    private fun str(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

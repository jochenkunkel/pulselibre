package de.pulselibre.positiontracking

/** Distance, duration, pace and heart rate of one track (#788); computed only from stored values. */
data class PositionTrackSummary(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val averageSpeedMps: Double?,
    val averageHeartRate: Double?,
    val maxHeartRate: Double?
) {
    companion object {
        private const val EARTH_RADIUS_M = 6_371_008.8

        fun haversine(a: PositionPoint, b: PositionPoint): Double {
            val p1 = Math.toRadians(a.latitude); val p2 = Math.toRadians(b.latitude)
            val dp = p2 - p1; val dl = Math.toRadians(b.longitude - a.longitude)
            val s1 = Math.sin(dp / 2); val s2 = Math.sin(dl / 2)
            val h = s1 * s1 + Math.cos(p1) * Math.cos(p2) * s2 * s2
            return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(h)))
        }

        fun of(sorted: List<PositionPoint>, heartRate: List<Pair<Long, Double>>): PositionTrackSummary {
            val distance = sorted.zipWithNext { a, b -> haversine(a, b) }.sum()
            val duration = if (sorted.size < 2) 0L else (sorted.last().timeMs - sorted.first().timeMs) / 1000
            val hr = heartRate.map { it.second }
            return PositionTrackSummary(
                distanceMeters = distance,
                durationSeconds = duration,
                averageSpeedMps = if (duration > 0) distance / duration else null,
                averageHeartRate = hr.takeIf { it.isNotEmpty() }?.average(),
                maxHeartRate = hr.maxOrNull()
            )
        }
    }
}

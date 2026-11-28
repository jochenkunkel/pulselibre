package de.pulselibre.positiontracking

/** One position as Android delivered it (#788). Optional values stay null when the provider did not report them. */
data class PositionPoint(
    val timeMs: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDeg: Float? = null
) {
    /** Only real coordinates are kept; nothing is invented or clamped. */
    fun isValid(): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 && timeMs > 0 &&
        !(latitude == 0.0 && longitude == 0.0)
}

data class PositionSession(val id: Long, val startMs: Long, val endMs: Long?, val label: String?)

package de.pulselibre

/** Step-derived display context; this is an experimental activity proxy, not a clinical category. */
enum class PulseContext(val label: String) {
    UNKNOWN("Modus unbekannt"), REST("Ruhe"), MOVEMENT("Leichte Bewegung"), SPORT("Sport");
}

/** Fixed display scale only. Endpoints are not medical thresholds or a maximum heart rate. */
data class HeartRateGaugeModel(val bpm: Int?) {
    val fraction: Float? get() = bpm?.let { ((it - SCALE_MIN) / (SCALE_MAX - SCALE_MIN).toFloat()).coerceIn(0f, 1f) }
    val scaleNote: String get() = when {
        bpm == null -> "Keine Messdaten"
        bpm < 40 -> "Unterhalb der Anzeigeskala"
        bpm > 180 -> "Oberhalb der Anzeigeskala"
        else -> "Messwertanzeige · keine Zielzone"
    }
    companion object {
        const val SCALE_MIN = 40
        const val SCALE_MAX = 180
        const val ARC_START_DEGREES = 170f
        const val ARC_SWEEP_DEGREES = 200f
        const val GAUGE_HEIGHT_PER_WIDTH = 0.90f
        const val DIAL_CENTER_Y_PER_HEIGHT = 0.43f
        const val DIAL_RADIUS_PER_WIDTH = 0.27f
        const val MAJOR_LABEL_OUTSET_PER_WIDTH = 0.075f
        const val MODE_Y_PER_HEIGHT = 0.59f
        const val VALUE_Y_PER_HEIGHT = 0.73f
        const val UNIT_Y_PER_HEIGHT = 0.85f

        fun needleAngle(fraction: Float): Float = ARC_START_DEGREES + fraction.coerceIn(0f, 1f) * ARC_SWEEP_DEGREES
        fun pivotOffsetFromCircleCenter(radius: Float): Float =
            kotlin.math.sin(Math.toRadians(ARC_START_DEGREES.toDouble())).toFloat() * radius

        fun from(metric: DashboardMetric): HeartRateGaugeModel {
            val value = Regex("^([0-9]+) bpm$").matchEntire(metric.value)?.groupValues?.get(1)?.toIntOrNull()
            return HeartRateGaugeModel(value?.takeIf { it in 1..240 })
        }
    }
}

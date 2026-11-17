package de.pulselibre

/** Manually selected context, not an inferred activity or a clinical category. */
enum class PulseContext(val label: String) {
    UNKNOWN("Modus unbekannt"), REST("Ruhe"), MOVEMENT("Leichte Bewegung"), SPORT("Sport");
    companion object {
        fun from(value: String?): PulseContext = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Fixed display scale only. Endpoints are not medical thresholds or a maximum heart rate. */
data class HeartRateGaugeModel(val bpm: Int?) {
    val fraction: Float? get() = bpm?.let { ((it - 40) / 140f).coerceIn(0f, 1f) }
    val scaleNote: String get() = when {
        bpm == null -> "Keine Messdaten"
        bpm < 40 -> "Unterhalb der Anzeigeskala"
        bpm > 180 -> "Oberhalb der Anzeigeskala"
        else -> "Messwertanzeige · keine Zielzone"
    }
    companion object {
        fun from(metric: DashboardMetric): HeartRateGaugeModel {
            val value = Regex("^([0-9]+) bpm$").matchEntire(metric.value)?.groupValues?.get(1)?.toIntOrNull()
            return HeartRateGaugeModel(value?.takeIf { it in 1..240 })
        }
    }
}

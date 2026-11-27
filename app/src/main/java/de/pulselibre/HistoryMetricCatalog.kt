package de.pulselibre

/**
 * One chart per measure (#763): every spec lists the live and band metric keys of `unified_measurements` that mean
 * the same physical quantity, so heart rate, SpO₂, stress … each get exactly one diagram whatever the origin.
 * Metrics whose live and band semantics differ (band daily counters, activity totals) are deliberately left out.
 */
object HistoryMetricCatalog {
    data class Spec(
        val key: String,
        /** German title; translated via [DomainText] and used for the icon. */
        val title: String,
        val unit: String,
        val metrics: Set<String>,
        /** Plausible value range; values outside are not plotted (sentinels, status codes). */
        val minValid: Double,
        val maxValid: Double,
        val hardMaximum: Int,
        /** Expanded when the user never chose otherwise (#764). */
        val defaultExpanded: Boolean,
        /** Shown even without data, so the main measures never silently vanish. */
        val alwaysShown: Boolean = false
    )

    val SPECS: List<Spec> = listOf(
        Spec("heart_rate", "Herzfrequenz", "bpm", setOf("bpm", "heart_rate"), 1.0, 240.0, 240, true, alwaysShown = true),
        Spec("spo2", "Sauerstoffsättigung · SpO₂", "%", setOf("spo2_percent", "spo2"), 50.0, 100.0, 100, true, alwaysShown = true),
        Spec("stress", "Stress", "", setOf("stress"), 0.0, 100.0, 100, false),
        Spec("respiration", "Atemfrequenz", "/min", setOf("breaths_per_min", "respiration"), 1.0, 60.0, 60, false),
        Spec("resting_heart_rate", "Ruhepuls", "bpm",
            setOf("resting_bpm", "resting_heart_rate", "current_day_resting_heart_rate"), 1.0, 240.0, 240, false),
        Spec("hrv", "HRV (ms)", "ms", setOf("hrv"), 1.0, 300.0, 300, false),
        Spec("body_battery", "Body Battery", "", setOf("body_battery"), 0.0, 100.0, 100, false)
    )

    val ALL_METRICS: Set<String> = SPECS.flatMap { it.metrics }.toSet()

    fun specFor(metric: String): Spec? = SPECS.firstOrNull { metric in it.metrics }

    /** Groups (metric, time, value) rows into one ascending, range-checked series per spec; one pass, no extra query. */
    fun group(rows: List<Triple<String, Long, Double>>): Map<String, List<HistoryPoint>> {
        val out = LinkedHashMap<String, MutableList<HistoryPoint>>()
        for ((metric, time, value) in rows) {
            val spec = specFor(metric) ?: continue
            if (value.isNaN() || value < spec.minValid || value > spec.maxValid) continue
            out.getOrPut(spec.key) { ArrayList() }.add(HistoryPoint(java.time.Instant.ofEpochMilli(time), Math.round(value).toInt()))
        }
        out.values.forEach { list -> list.sortBy { it.receivedAt } }
        return out
    }
}

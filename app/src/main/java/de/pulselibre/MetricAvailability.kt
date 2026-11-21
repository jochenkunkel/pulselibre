package de.pulselibre

/**
 * Availability state for analysis tiles (#40). Separates "never received", "too little to
 * compute", "usable" and "experimental source"; an empty series is never a zero value.
 */
internal enum class MetricAvailability(val labelRes: Int) {
    NOT_AVAILABLE(R.string.ma_not_available),
    INSUFFICIENT_DATA(R.string.ma_insufficient),
    EXPERIMENTAL(R.string.ma_experimental),
    AVAILABLE(R.string.ma_available);

    companion object {
        /** Minimum share of elapsed minutes with observations for an analysis to be computed. */
        const val MIN_COVERAGE_PERCENT = 50.0

        /** Metrics whose source or semantics are device-proprietary or unvalidated; never medical. */
        val EXPERIMENTAL_METRICS: Set<DashboardMeasurement> = setOf(
            DashboardMeasurement.OXYGEN, DashboardMeasurement.RESTING_PULSE,
            DashboardMeasurement.STRESS, DashboardMeasurement.BODY_BATTERY,
            DashboardMeasurement.RESPIRATION, DashboardMeasurement.CALORIES_TOTAL,
            DashboardMeasurement.CALORIES_ACTIVE
        )

        fun of(metric: DashboardMeasurement, quality: DayDataQuality?, validObservations: Int): MetricAvailability {
            if (validObservations <= 0) return NOT_AVAILABLE
            val coverage = quality?.coveragePercent
            if (coverage == null || coverage < MIN_COVERAGE_PERCENT) return INSUFFICIENT_DATA
            return if (metric in EXPERIMENTAL_METRICS) EXPERIMENTAL else AVAILABLE
        }
    }
}

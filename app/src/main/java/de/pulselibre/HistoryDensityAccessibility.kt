package de.pulselibre

/** Screen-reader text for the canvas-drawn density chart (audit #6, TalkBack). No values are invented or smoothed. */
internal object HistoryDensityAccessibility {
    fun summary(t: Strings, density: HistoryDensity, axis: AxisScale): String {
        val total = density.columns.size
        val withData = density.columns.count { it.sampleCount > 0 }
        val samples = density.columns.sumOf { it.sampleCount }
        if (total == 0 || withData == 0) return t.get(R.string.hd_density_empty)
        return t.get(R.string.hd_density_cd, samples, withData, total, axis.minimum, axis.maximum)
    }
}

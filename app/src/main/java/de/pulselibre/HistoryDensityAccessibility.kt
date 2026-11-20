package de.pulselibre

/** Screen-reader text for the canvas-drawn density chart (audit #6, TalkBack). No values are invented or smoothed. */
internal object HistoryDensityAccessibility {
    fun summary(density: HistoryDensity, axis: AxisScale): String {
        val total = density.columns.size
        val withData = density.columns.count { it.sampleCount > 0 }
        val samples = density.columns.sumOf { it.sampleCount }
        if (total == 0 || withData == 0) return "Verteilungsdiagramm: keine Messwerte im gewählten Zeitraum."
        return "Verteilungsdiagramm: $samples Messwerte in $withData von $total Zeitfenstern, " +
            "Wertebereich der Achse ${axis.minimum} bis ${axis.maximum}. " +
            "Doppeltippen auf das Diagramm öffnet die Einzelwerte des angetippten Zeitfensters; " +
            "die Farbe zeigt die Häufigkeit, Zeitfenster ohne Werte bleiben leer."
    }
}

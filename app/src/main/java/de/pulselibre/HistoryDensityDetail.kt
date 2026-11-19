package de.pulselibre

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Detail text for one time window of the density chart (#219). Statistics come from the raw points of exactly this window
 * (same population as the colours): sample size, median, mean, min/max, the most frequent value bin, and what was excluded.
 */
internal object HistoryDensityDetail {
    private val format = DateTimeFormatter.ofPattern("d.M. HH:mm", Locale.GERMANY)
    private val one = java.text.NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 1 }

    fun describe(column: HistoryDensityColumn, points: List<HistoryPoint>, unit: String, zone: ZoneId, isLast: Boolean): String {
        val inWindow = points.filter { it.receivedAt >= column.start && (it.receivedAt < column.end || (isLast && it.receivedAt == column.end)) }
        val window = "${column.start.atZone(zone).format(format)} – ${column.end.atZone(zone).format(format)}"
        if (inWindow.isEmpty()) return "$window\nKeine Messwerte in diesem Zeitfenster (Lücke, nichts interpoliert)."
        val values = inWindow.map { it.value }.sorted()
        val median = if (values.size % 2 == 1) values[values.size / 2].toDouble() else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        val peak = column.bins.maxByOrNull { it.count }?.takeIf { it.count > 0 }
        val peakText = peak?.let {
            val upper = if (it.upperBoundInclusive) "–${it.upperBound}" else "–<${it.upperBound}"
            "Häufigstes Intervall: ${it.lowerInclusive}$upper $unit (${Math.round(it.probability * 100)} % der ${column.sampleCount} Werte im Achsenbereich)"
        } ?: "Alle Werte liegen außerhalb der Achse."
        return buildString {
            appendLine(window)
            appendLine("Stichprobe: ${inWindow.size} Werte")
            appendLine("Median ${one.format(median)} $unit · Mittelwert ${one.format(values.average())} $unit")
            appendLine("Beobachtet: ${values.first()}–${values.last()} $unit")
            appendLine(peakText)
            if (column.excludedOutsideAxis > 0) appendLine("${column.excludedOutsideAxis} Werte außerhalb der gezeigten Achse nicht eingefärbt.")
            append("Relative Verteilung innerhalb dieses Fensters; keine Diagnose.")
        }
    }

    const val LEGEND = "Verteilung je Zeitfenster: Farbe = relative Häufigkeit der Messwerte innerhalb desselben Fensters " +
        "(rot häufig · gelb mittel · grün selten, kräftiger = häufiger), gemeinsame Skala für alle Fenster. " +
        "Zeigt die Form der Verteilung, nicht die Anzahl der Messwerte. Leer = keine Messwerte. Antippen für Details."

    const val LEGEND_COLOR_SAFE = "Verteilung je Zeitfenster (farbsicher): Farbe = relative Häufigkeit der Messwerte innerhalb desselben Fensters " +
        "(gelb häufig · türkis mittel · blau selten, kräftiger = häufiger), gemeinsame Skala für alle Fenster. " +
        "Ohne Rot/Grün-Unterscheidung. Leer = keine Messwerte. Antippen für Details."
}

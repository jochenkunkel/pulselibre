package de.pulselibre

/** Central mapping of legacy information-row markers to PulsLibre SVG icons.
 * Markers are internal identifiers only; they are never drawn as text.
 */
internal object MetricInfoIconMapping {
    fun forSymbol(symbol: String): PlIcon = when (symbol) {
        "▬" -> PlIcon.INFO_RANGE
        "▰" -> PlIcon.INFO_BAND
        "↗" -> PlIcon.INFO_SOURCE
        "◷" -> PlIcon.INFO_TIME
        "⚠" -> PlIcon.WARNING
        "⏳" -> PlIcon.HOURGLASS
        "●" -> PlIcon.SIGNAL
        "i" -> PlIcon.INFO
        "■" -> PlIcon.GENERIC_METRIC
        "▱" -> PlIcon.INFO_WINDOW
        else -> PlIcon.INFO
    }
}

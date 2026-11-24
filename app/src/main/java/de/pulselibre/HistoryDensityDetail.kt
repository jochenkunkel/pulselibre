package de.pulselibre

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Detail text for one time window of the density chart (#219). Statistics come from the raw points of exactly this window
 * (same population as the colours): sample size, median, mean, min/max, the most frequent value bin, and what was excluded.
 */
internal object HistoryDensityDetail {
    /** [locale] is the app language (decimal separator, day-month order); the default keeps the German form used by the unit tests. */
    fun describe(
        t: Strings, column: HistoryDensityColumn, points: List<HistoryPoint>, unit: String, zone: ZoneId, isLast: Boolean,
        locale: Locale = Locale.GERMANY
    ): String {
        // 24-h time in every language; only the order of day and month follows the language.
        val format = DateTimeFormatter.ofPattern(
            when (locale.language) { "de" -> "d.M. HH:mm"; "en" -> "M/d HH:mm"; else -> "d/M HH:mm" }, locale)
        val one = java.text.NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
        val inWindow = points.filter { it.receivedAt >= column.start && (it.receivedAt < column.end || (isLast && it.receivedAt == column.end)) }
        val window = "${column.start.atZone(zone).format(format)} – ${column.end.atZone(zone).format(format)}"
        if (inWindow.isEmpty()) return t.get(R.string.hd_window_empty, window)
        val values = inWindow.map { it.value }.sorted()
        val median = if (values.size % 2 == 1) values[values.size / 2].toDouble() else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
        val peak = column.bins.maxByOrNull { it.count }?.takeIf { it.count > 0 }
        val peakText = peak?.let {
            val upper = if (it.upperBoundInclusive) "–${it.upperBound}" else "–<${it.upperBound}"
            t.get(R.string.hd_peak, "${it.lowerInclusive}$upper", unit, Math.round(it.probability * 100).toInt(), column.sampleCount)
        } ?: t.get(R.string.hd_peak_none)
        return buildString {
            appendLine(window)
            appendLine(t.get(R.string.hd_sample, inWindow.size))
            appendLine(t.get(R.string.hd_median_mean, one.format(median), one.format(values.average()), unit))
            appendLine(t.get(R.string.hd_observed, values.first(), values.last(), unit))
            appendLine(peakText)
            if (column.excludedOutsideAxis > 0) appendLine(t.get(R.string.hd_excluded, column.excludedOutsideAxis))
            append(t.get(R.string.hd_footer))
        }
    }

    fun legend(t: Strings, colorSafe: Boolean): String = t.get(if (colorSafe) R.string.hd_legend_safe else R.string.hd_legend)
}

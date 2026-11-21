package de.pulselibre

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Spoken time span of a history chart axis (audit #6, TalkBack). The drawn axis labels only show `HH:mm` in the day
 * range, which reads "15:24 bis 15:24" for a full 24-hour window; speech therefore always names the date as well.
 */
internal object HistoryAxisSpeech {
    /** [dayPattern]/[datePattern] default to the German layout (the unit-tested identifier form); views pass the locale's own. */
    fun span(t: Strings, start: Instant, end: Instant, range: HistoryRange, zone: ZoneId,
             dayPattern: String = "dd.MM. HH:mm", datePattern: String = "dd.MM."): String {
        val pattern = if (range == HistoryRange.DAY) dayPattern else datePattern
        val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone)
        return t.get(R.string.hd_axis_span, formatter.format(start), formatter.format(end))
    }
}

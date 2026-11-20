package de.pulselibre

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Spoken time span of a history chart axis (audit #6, TalkBack). The drawn axis labels only show `HH:mm` in the day
 * range, which reads "15:24 bis 15:24" for a full 24-hour window; speech therefore always names the date as well.
 */
internal object HistoryAxisSpeech {
    fun span(start: Instant, end: Instant, range: HistoryRange, zone: ZoneId): String {
        val pattern = if (range == HistoryRange.DAY) "dd.MM. HH:mm" else "dd.MM."
        val formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone)
        return "${formatter.format(start)} bis ${formatter.format(end)}"
    }
}

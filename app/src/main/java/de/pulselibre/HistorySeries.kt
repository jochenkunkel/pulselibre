package de.pulselibre

import java.time.Duration
import java.time.Instant

/** Rolling ranges use actual elapsed time, not inferred calendar-day/week summaries. */
enum class HistoryRange(val label: String, val duration: Duration) {
    DAY("24 Stunden", Duration.ofHours(24)),
    WEEK("7 Tage", Duration.ofDays(7)),
    MONTH("30 Tage", Duration.ofDays(30));

    fun startAt(end: Instant): Instant = end.minus(duration)
}

/** One locally received original observation; timestamp is phone reception time. */
data class HistoryPoint(val receivedAt: Instant, val value: Int)

data class HistorySnapshot(
    val range: HistoryRange,
    val start: Instant,
    val end: Instant,
    val heartRate: List<HistoryPoint>,
    val oxygen: List<HistoryPoint>
)

/** Plot position only; values outside the selected time range are excluded. */
fun historyTimeFraction(time: Instant, start: Instant, end: Instant): Float? {
    if (end <= start || time < start || time > end) return null
    val span = Duration.between(start, end).toMillis()
    if (span <= 0L) return null
    return (Duration.between(start, time).toMillis().toDouble() / span).toFloat().coerceIn(0f, 1f)
}

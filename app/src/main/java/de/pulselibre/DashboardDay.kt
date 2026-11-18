package de.pulselibre

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A phone-local calendar day, with real elapsed time through daylight-saving transitions. */
internal data class DashboardDay(val date: LocalDate, val zone: ZoneId) {
    val start: Instant = date.atStartOfDay(zone).toInstant()
    val end: Instant = date.plusDays(1).atStartOfDay(zone).toInstant()
    private val seconds = Duration.between(start, end).seconds

    fun fraction(time: Instant): Float =
        if (seconds == 0L) 0f else (Duration.between(start, time).toMillis() / (seconds * 1000.0)).toFloat()

    fun axisFractions(): List<Float> = listOf(0f) +
        listOf(6, 12, 18).map { hour -> fraction(date.atTime(hour, 0).atZone(zone).toInstant()) } +
        1f

    /** Minute buckets that could contain an observation at or before now. */
    fun availableMinutes(now: Instant): Int {
        if (now < start || start >= end) return 0
        val lastIncluded = minOf(now, end.minusNanos(1))
        return Duration.between(start, lastIncluded).toMinutes().toInt() + 1
    }

    fun includes(time: Instant, now: Instant): Boolean = time >= start && time < end && time <= now
}

internal data class DayObservation(val time: Instant, val value: Long)

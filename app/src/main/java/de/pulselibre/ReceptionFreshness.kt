package de.pulselibre

import java.time.Duration
import java.time.Instant

/** Reception recency only; these colors say nothing about measurement quality or health. */
enum class ReceptionFreshness {
    UNKNOWN, FRESH, RECENT, DELAYED, OLD;

    companion object {
        fun of(receivedAt: Instant?, now: Instant): ReceptionFreshness {
            if (receivedAt == null || receivedAt.isAfter(now)) return UNKNOWN
            val ageSeconds = Duration.between(receivedAt, now).seconds
            return when {
                ageSeconds <= 180 -> FRESH
                ageSeconds <= 900 -> RECENT
                ageSeconds <= 3600 -> DELAYED
                else -> OLD
            }
        }
    }
}

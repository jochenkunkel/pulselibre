package de.pulselibre

import android.content.SharedPreferences

/** Bounded persistent gate for explicitly confirmed, no-pull item-list tests. */
internal object CirqaItemListRepeatabilityAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    const val PREFS_NAME = "cirqa_item_list_repeatability"
    const val ATTEMPT_COUNT_KEY = "cirqaItemListRepeatabilityAttemptCountV1"
    const val SUMMARY_KEY = "cirqaItemListRepeatabilitySummary"
    const val MAX_ATTEMPTS = 40

    fun attemptsUsed(prefs: SharedPreferences): Int =
        prefs.getInt(ATTEMPT_COUNT_KEY, 0).coerceIn(0, MAX_ATTEMPTS)

    fun hasAttemptsRemaining(prefs: SharedPreferences): Boolean =
        RELEASED_FOR_DEVICE_TEST && attemptsUsed(prefs) < MAX_ATTEMPTS

    /** Consume before connecting so a failed run cannot silently retry. */
    fun consume(prefs: SharedPreferences): Boolean {
        if (!hasAttemptsRemaining(prefs)) return false
        return prefs.edit().putInt(ATTEMPT_COUNT_KEY, attemptsUsed(prefs) + 1).commit()
    }
}

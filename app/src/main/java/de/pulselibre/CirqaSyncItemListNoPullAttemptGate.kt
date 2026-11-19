package de.pulselibre

import android.content.SharedPreferences

/** Bounded gate for explicitly confirmed sync_request + one-list, no-pull probes. */
internal object CirqaSyncItemListNoPullAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    // Share the S25's bounded live-attempt budget with direct Item-List probes.
    const val PREFS_NAME = CirqaItemListRepeatabilityAttemptGate.PREFS_NAME
    const val ATTEMPT_COUNT_KEY = CirqaItemListRepeatabilityAttemptGate.ATTEMPT_COUNT_KEY
    const val SUMMARY_KEY = "summary"
    const val MAX_ATTEMPTS = CirqaItemListRepeatabilityAttemptGate.MAX_ATTEMPTS

    fun attemptsUsed(prefs: SharedPreferences): Int =
        attemptsUsed(prefs.getInt(ATTEMPT_COUNT_KEY, 0))

    fun attemptsUsed(storedCount: Int): Int = storedCount.coerceIn(0, MAX_ATTEMPTS)

    fun hasAttemptsRemaining(storedCount: Int): Boolean =
        RELEASED_FOR_DEVICE_TEST && attemptsUsed(storedCount) < MAX_ATTEMPTS

    fun hasAttemptsRemaining(prefs: SharedPreferences): Boolean =
        hasAttemptsRemaining(attemptsUsed(prefs))

    /** Consume before connecting so a failed run cannot silently retry. */
    fun consume(prefs: SharedPreferences): Boolean {
        if (!hasAttemptsRemaining(prefs)) return false
        return prefs.edit().putInt(ATTEMPT_COUNT_KEY, attemptsUsed(prefs) + 1).commit()
    }
}

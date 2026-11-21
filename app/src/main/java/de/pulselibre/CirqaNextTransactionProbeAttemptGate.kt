package de.pulselibre

import android.content.SharedPreferences

/** Independent bounded gate; does not reset or extend the exhausted shared Item-List budget. */
internal object CirqaNextTransactionProbeAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    const val PREFS_NAME = "cirqa_next_transaction_probe"
    const val ATTEMPT_COUNT_KEY = "attemptCountV1"
    const val SUMMARY_KEY = "summary"
    // Two authorized probes are already consumed. This permits exactly one
    // separately authorized third run without resetting its persisted count.
    const val MAX_ATTEMPTS = 3

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

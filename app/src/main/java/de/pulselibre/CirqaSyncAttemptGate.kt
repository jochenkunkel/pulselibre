package de.pulselibre

import android.content.SharedPreferences

/** Fresh persistent one-shot gate for the Connect-style sync_request test. */
object CirqaSyncAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    const val PREFERENCE_KEY = "cirqaSyncAttemptedV23"
    const val SUMMARY_KEY = "cirqaSyncSummary"
    const val PREFS_NAME = "cirqa_sync_attempt"

    fun available(prefs: SharedPreferences): Boolean =
        !prefs.getBoolean(PREFERENCE_KEY, false)

    fun consume(prefs: SharedPreferences): Boolean {
        if (!RELEASED_FOR_DEVICE_TEST || !available(prefs)) return false
        return prefs.edit().putBoolean(PREFERENCE_KEY, true).commit()
    }
}

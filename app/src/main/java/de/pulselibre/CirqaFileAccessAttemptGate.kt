package de.pulselibre

import android.content.SharedPreferences

/** Fresh persistent one-shot gate for the 5043 FileAccess item-list test. */
object CirqaFileAccessAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    const val PREFERENCE_KEY = "cirqaFileAccessAttemptedV1"
    const val SUMMARY_KEY = "cirqaFileAccessSummary"
    const val PREFS_NAME = "cirqa_file_access"

    fun available(prefs: SharedPreferences): Boolean =
        !prefs.getBoolean(PREFERENCE_KEY, false)

    fun consume(prefs: SharedPreferences): Boolean {
        if (!RELEASED_FOR_DEVICE_TEST || !available(prefs)) return false
        return prefs.edit().putBoolean(PREFERENCE_KEY, true).commit()
    }
}

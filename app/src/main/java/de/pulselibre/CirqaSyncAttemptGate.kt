package de.pulselibre

import android.content.SharedPreferences

/** Persistent one-shot authorization for the explicitly approved V27 sync test. */
object CirqaSyncAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    /** New key preserves the consumed V25/V26 attempt; never clear or re-arm it. */
    const val PREFERENCE_KEY = "cirqaSyncAttemptedV27"
    const val SUMMARY_KEY = "cirqaSyncSummaryV27"
    const val PREFS_NAME = "cirqa_sync_attempt"

    fun available(prefs: SharedPreferences): Boolean =
        available(prefs.getBoolean(PREFERENCE_KEY, false))

    internal fun available(alreadyAttempted: Boolean): Boolean = !alreadyAttempted

    fun consume(prefs: SharedPreferences): Boolean {
        if (!RELEASED_FOR_DEVICE_TEST || !available(prefs)) return false
        return prefs.edit().putBoolean(PREFERENCE_KEY, true).commit()
    }
}

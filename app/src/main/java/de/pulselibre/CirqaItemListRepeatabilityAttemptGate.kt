package de.pulselibre

import android.content.SharedPreferences

/** One-shot gate for the explicit, no-pull, repeated FileAccess item-list test. */
internal object CirqaItemListRepeatabilityAttemptGate {
    const val RELEASED_FOR_DEVICE_TEST = true
    const val PREFS_NAME = "cirqa_item_list_repeatability"
    const val PREFERENCE_KEY = "cirqaItemListRepeatabilityAttemptedV1"
    const val SUMMARY_KEY = "cirqaItemListRepeatabilitySummary"

    fun consume(prefs: SharedPreferences): Boolean {
        if (!RELEASED_FOR_DEVICE_TEST || prefs.getBoolean(PREFERENCE_KEY, false)) return false
        return prefs.edit().putBoolean(PREFERENCE_KEY, true).commit()
    }
}

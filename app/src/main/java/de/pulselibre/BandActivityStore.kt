package de.pulselibre

import android.content.SharedPreferences

/**
 * #737: last activity status the band reported during a history download, kept so the device tab and the overview can
 * show "activity running" between downloads. Only codes seen in a report are stored; an answered subscription without
 * any state means "no activity" (the band sends 30.3 only while an activity exists, S10 run 28.11.2026).
 */
object BandActivityStore {
    const val PREFS = "band_activity"
    private const val KEY_CODE = "code"
    private const val KEY_AT = "at_ms"
    const val NONE = 0
    /** A stale "running" is not shown: the band may have been stopped on the wrist without a new download. */
    const val MAX_AGE_MS = 6L * 60 * 60 * 1000

    /** State code from a sync report: the number, [NONE] for "subscribed", null if the report says nothing. */
    fun fromReport(report: String): Int? {
        val value = Regex("bandActivity=([^,;\\s]+)").find(report)?.groupValues?.get(1) ?: return null
        return value.toIntOrNull() ?: if (value == "subscribed") NONE else null
    }

    fun record(prefs: SharedPreferences, report: String, nowMs: Long) {
        val code = fromReport(report) ?: return
        prefs.edit().putInt(KEY_CODE, code).putLong(KEY_AT, nowMs).apply()
    }

    /**
     * True while the last known state is an unfinished activity. Observed: 3 running, 9 running (walk, S10 28.11.2026),
     * 1 stopped, 5 saving; only 4 (saved/ready) and "no state" mean finished. Unknown codes count as running.
     */
    fun isActive(code: Int?, atMs: Long, nowMs: Long): Boolean =
        code != null && code != NONE && code != READY && nowMs - atMs in 0..MAX_AGE_MS

    const val READY = 4

    fun isActive(prefs: SharedPreferences, nowMs: Long): Boolean =
        isActive(if (prefs.contains(KEY_CODE)) prefs.getInt(KEY_CODE, NONE) else null, prefs.getLong(KEY_AT, 0L), nowMs)

    fun code(prefs: SharedPreferences): Int? = if (prefs.contains(KEY_CODE)) prefs.getInt(KEY_CODE, NONE) else null
    fun atMs(prefs: SharedPreferences): Long = prefs.getLong(KEY_AT, 0L)
}

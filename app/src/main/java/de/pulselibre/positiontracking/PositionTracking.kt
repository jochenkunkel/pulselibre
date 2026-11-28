package de.pulselibre.positiontracking

import android.content.Context

/** Setting "GPS Spur zu aktiver Tätigkeit" (#788), default off, and the running session id. */
object PositionTracking {
    private const val PREFS = "position_tracking"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ACTIVE = "active_session"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isEnabled(c: Context) = prefs(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, on: Boolean) { prefs(c).edit().putBoolean(KEY_ENABLED, on).apply() }
    fun activeSession(c: Context): Long? = prefs(c).getLong(KEY_ACTIVE, -1L).takeIf { it > 0 }
    internal fun setActive(c: Context, id: Long?) {
        prefs(c).edit().apply { if (id == null) remove(KEY_ACTIVE) else putLong(KEY_ACTIVE, id) }.apply()
    }
}

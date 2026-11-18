package de.pulselibre

/**
 * "Recorder runs" must not rely on a flag alone (#113): after a process kill the flag stays true. The service writes a
 * heartbeat while it is alive; a flag without a fresh heartbeat counts as not running.
 */
internal object RecorderStatus {
    const val HEARTBEAT_INTERVAL_MS = 20_000L
    /** Three missed heartbeats. */
    const val STALE_AFTER_MS = 3 * HEARTBEAT_INTERVAL_MS + 5_000L

    fun isRunning(flag: Boolean, heartbeatMs: Long?, nowMs: Long): Boolean {
        if (!flag || heartbeatMs == null || heartbeatMs < 0) return false
        return nowMs - heartbeatMs in 0..STALE_AFTER_MS
    }

    fun isRunning(prefs: android.content.SharedPreferences, nowMs: Long = System.currentTimeMillis()): Boolean =
        isRunning(
            prefs.getBoolean(CirqaContinuousRecordingService.KEY_RUNNING, false),
            prefs.getLong(CirqaContinuousRecordingService.KEY_HEARTBEAT_MS, -1L).takeIf { it >= 0 }, nowMs
        )
}

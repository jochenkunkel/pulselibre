package de.pulselibre

import android.content.Context

/**
 * Opt-in setting "Aufzeichnung beim App-Start automatisch starten".
 *
 * Default is OFF: starting the recording service actively connects to the band, so it needs the user's explicit choice.
 * The flag lives in its own store (not in the profile) so that "Profil löschen" does not silently change it.
 * Only the decision to *start* the existing service is made here; the service itself is not touched.
 */
object AutoStartRecording {
    private const val STORE = "recording_settings"
    private const val KEY = "auto_start_on_app_launch"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun save(context: Context, enabled: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).apply()
    }

    /**
     * "Already running" for the autostart decision (#212). The recorder flag + heartbeat survive a process kill for up to
     * [RecorderStatus.STALE_AFTER_MS] (e.g. Android force-stops the service on an app update), so they alone must not block
     * a fresh start: the service has to be alive as well.
     */
    fun isAlreadyRunning(recorderFlagFresh: Boolean, serviceAlive: Boolean): Boolean = recorderFlagFresh && serviceAlive

    /** True when the recording service is actually alive in this app (own services only; works on API 26+). */
    @Suppress("DEPRECATION")
    fun isServiceAlive(context: Context): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.getRunningServices(Int.MAX_VALUE).any { it.service.className == CirqaContinuousRecordingService::class.java.name }
    } catch (_: Exception) { false }

    /**
     * Pure decision, unit-tested. Starts only on a fresh app launch (not on rotation / recreate), only when all
     * permissions are already granted and Bluetooth is already on (the app never turns Bluetooth on itself or asks
     * for permissions here), and never twice if the service is already running.
     */
    fun shouldStart(enabled: Boolean, freshLaunch: Boolean, permissionsGranted: Boolean,
                    bluetoothOn: Boolean, alreadyRunning: Boolean): Boolean =
        enabled && freshLaunch && permissionsGranted && bluetoothOn && !alreadyRunning
}

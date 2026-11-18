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
     * Pure decision, unit-tested. Starts only on a fresh app launch (not on rotation / recreate), only when all
     * permissions are already granted and Bluetooth is already on (the app never turns Bluetooth on itself or asks
     * for permissions here), and never twice if the service is already running.
     */
    fun shouldStart(enabled: Boolean, freshLaunch: Boolean, permissionsGranted: Boolean,
                    bluetoothOn: Boolean, alreadyRunning: Boolean): Boolean =
        enabled && freshLaunch && permissionsGranted && bluetoothOn && !alreadyRunning
}

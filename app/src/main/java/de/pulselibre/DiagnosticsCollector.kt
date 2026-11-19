package de.pulselibre

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.File

/** Gathers [DiagnosticsSnapshot] without any BLE access or side effects. Call on a worker thread. */
internal object DiagnosticsCollector {
    // Fixed whitelist: never interpolate anything else into SQL.
    private val TABLES = listOf("events", "observations", "fit_sources", "fit_observation_provenance",
        "fit_device_metadata", "raw_device_metadata", "fit_unmapped_fields")

    fun collect(context: Context, nowMs: Long = System.currentTimeMillis()): DiagnosticsSnapshot {
        val prefs = context.getSharedPreferences(CirqaContinuousRecordingService.PREFS, Context.MODE_PRIVATE)
        val flagFresh = RecorderStatus.isRunning(prefs, nowMs)
        val lastReading = prefs.getLong(CirqaContinuousRecordingService.KEY_LAST_READING_MS, -1L).takeIf { it in 0..nowMs }
        val heartbeat = prefs.getLong(CirqaContinuousRecordingService.KEY_HEARTBEAT_MS, -1L).takeIf { it in 0..nowMs }
        var integrity = "nicht geprüft"
        val counts = LinkedHashMap<String, Long?>()
        var dbBytes = 0L
        try {
            CirqaDatabase(context.applicationContext).use { database ->
                val db = database.readableDatabase
                integrity = try { db.rawQuery("PRAGMA quick_check", null).use { if (it.moveToFirst()) it.getString(0) else "keine Antwort" } }
                catch (_: Exception) { "Prüfung fehlgeschlagen" }
                TABLES.forEach { table ->
                    counts[table] = try { db.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getLong(0) else null } }
                    catch (_: Exception) { null }
                }
                dbBytes = try { File(db.path).length() } catch (_: Exception) { 0L }
            }
        } catch (_: Exception) { integrity = "Datenbank nicht lesbar" }
        val fitDir = File(context.filesDir, "verified_fit")
        val fitFiles = fitDir.listFiles()?.filter { it.isFile && it.name.endsWith(".fit") } ?: emptyList()
        fun granted(permission: String) = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        val bt = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter?.isEnabled
        } catch (_: SecurityException) { null }
        return DiagnosticsSnapshot(
            appVersion = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" },
            androidRelease = Build.VERSION.RELEASE, sdkInt = Build.VERSION.SDK_INT, deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            dbIntegrity = integrity, dbBytes = dbBytes, tableCounts = counts,
            verifiedFitFiles = fitFiles.size, verifiedFitBytes = fitFiles.sumOf { it.length() },
            recorderFlagFresh = flagFresh, serviceAlive = AutoStartRecording.isServiceAlive(context),
            lastReadingAgeSeconds = lastReading?.let { (nowMs - it) / 1000 }, heartbeatAgeSeconds = heartbeat?.let { (nowMs - it) / 1000 },
            bluetoothEnabled = bt,
            permissionConnect = Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_CONNECT),
            permissionScan = Build.VERSION.SDK_INT < 31 || granted(Manifest.permission.BLUETOOTH_SCAN),
            permissionNotifications = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)
        )
    }
}

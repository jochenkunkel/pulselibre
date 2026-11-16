package de.pulselibre

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Explicitly user-started, foreground BLE sampling experiment.
 * Each connection is bounded by the existing 20-second probe and closes.
 * No historical FIT downloads yet; Android may still stop the service.
 */
class CirqaOvernightService : Service() {
    companion object {
        const val ACTION_START = "de.pulselibre.START_OVERNIGHT"
        const val ACTION_STOP = "de.pulselibre.STOP_OVERNIGHT"
        const val PREFS = "cirqa_recorder_state"
        const val KEY_RUNNING = "running"
        private const val CHANNEL = "cirqa_collection"
        private const val ID = 1401
        private const val INTERVAL_MS = 120_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var probe: CirqaMlHandshakeProbe? = null
    private var running = false
    private lateinit var log: CirqaLocalLog
    private var attempts = 0
    private var readings = 0
    private var generation = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        log = CirqaLocalLog(this)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(CHANNEL, "CIRQA-Datensammlung", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCollection("user_stop")
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (running) return START_NOT_STICKY
        val notification = notification("Lokal sammeln · noch keine Messwerte")
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else startForeground(ID, notification)
        } catch (_: Exception) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, true).apply()
        if (!log.append("session_start", mapOf("interval_seconds" to INTERVAL_MS / 1000, "mode" to "realtime_poll"))) {
            stopCollection("storage_limit")
            return START_NOT_STICKY
        }
        sample()
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun sample() {
        if (!running) return
        attempts++
        val token = ++generation
        var completed = false
        val attemptNumber = attempts
        if (!log.append("attempt_start", mapOf("number" to attempts))) {
            stopCollection("storage_limit")
            return
        }
        // The polling cadence is independent of completion; timeout is a separate
        // cancellable callback so a successful attempt cannot retain a stale timeout.
        val timeout = Runnable {
            if (!running || token != generation || completed) return@Runnable
            completed = true
            probe?.close()
            probe = null
            if (!log.append("attempt_timeout", mapOf("number" to attemptNumber, "reason" to "no_completion_callback"))) {
                stopCollection("storage_limit")
            }
        }
        handler.postDelayed(timeout, INTERVAL_MS)
        handler.postDelayed({
            if (running && token == generation) sample()
        }, INTERVAL_MS)
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            completed = true
            handler.removeCallbacks(timeout)
            if (!log.append("attempt", mapOf("number" to attemptNumber, "result" to "bluetooth_off"))) stopCollection("storage_limit")
            return
        }
        val current = CirqaMlHandshakeProbe(this, adapter)
        probe = current
        current.onReading = readingCallback@{ service, reading ->
            if (!running || token != generation || completed) return@readingCallback
            readings++
            if (!log.reading(service, reading)) stopCollection("storage_limit")
            else updateNotification("Lokal: $readings Messwerte · $attempts Versuche")
        }
        current.startAllMetrics { result ->
            if (!running || token != generation || completed || result.startsWith("ML-Test: Verbinde")) return@startAllMetrics
            completed = true
            handler.removeCallbacks(timeout)
            if (!log.append("attempt", mapOf("number" to attemptNumber, "result" to result.take(600)))) {
                stopCollection("storage_limit")
            } else {
                probe = null
            }
        }
    }

    private fun stopCollection(reason: String) {
        if (running) log.append("session_end", mapOf("reason" to reason, "attempts" to attempts, "readings" to readings))
        running = false
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()
        generation++
        handler.removeCallbacksAndMessages(null)
        probe?.close()
        probe = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("NotificationPermission") // Runtime permission checked immediately below; foreground service remains active without it.
    private fun updateNotification(message: String) {
        // Foreground-service notification is handled by startForeground; optional updates
        // must respect the Android 13+ notification runtime permission.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(ID, notification(message))
    }

    private fun notification(message: String): Notification {
        val stop = PendingIntent.getService(this, 0,
            Intent(this, CirqaOvernightService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("PulseLibre · CIRQA-Datensammlung")
            .setContentText(message)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Sammlung beenden", stop).build())
            .build()
    }

    override fun onDestroy() {
        if (running) stopCollection("service_destroyed")
        super.onDestroy()
    }
}

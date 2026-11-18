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
 * An established GATT notification session stays open until stop/disconnect.
 * No historical FIT downloads yet; Android may still stop the service.
 */
class CirqaContinuousRecordingService : Service() {
    companion object {
        const val ACTION_START = "de.pulselibre.START_OVERNIGHT"
        const val ACTION_STOP = "de.pulselibre.STOP_OVERNIGHT"
        const val PREFS = "cirqa_recorder_state"
        const val KEY_RUNNING = "running"
        const val KEY_LAST_READING_MS = "last_reading_ms"
        private const val CHANNEL = "cirqa_collection"
        private const val ID = 1401
    }

    private val handler = Handler(Looper.getMainLooper())
    private var probe: CirqaMlHandshakeProbe? = null
    private var running = false
    private lateinit var log: CirqaLocalLog
    private var attempts = 0
    private var readings = 0
    private var generation = 0
    private var lastReadingPersistedMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        log = CirqaLocalLog(this)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(CHANNEL, "CIRQA · Kontinuierliche Aufzeichnung", NotificationManager.IMPORTANCE_LOW))
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
        val bluetooth = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bluetooth == null || !bluetooth.isEnabled) {
            updateNotification("Bluetooth ausgeschaltet – bitte einschalten")
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, true).apply()
        if (!log.append("session_start", mapOf("mode" to "continuous_notifications"))) {
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
        val attemptNumber = attempts
        if (!log.append("attempt_start", mapOf("number" to attemptNumber))) {
            stopCollection("storage_limit")
            return
        }
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            scheduleReconnect(token, "bluetooth_off")
            return
        }
        val current = CirqaMlHandshakeProbe(this, adapter)
        probe = current
        current.onReading = readingCallback@{ service, reading ->
            if (!running || token != generation) return@readingCallback
            readings++
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastReadingPersistedMs >= 5_000L) { // throttled liveness marker for the UI status
                lastReadingPersistedMs = nowMs
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(KEY_LAST_READING_MS, nowMs).apply()
            }
            if (!log.reading(service, reading)) stopCollection("storage_limit")
            else updateNotification("Lokal: $readings Messwerte · CIRQA-Daten empfangen")
        }
        // The probe owns one GATT connection and receives notifications until stopped
        // or disconnected. No periodic disconnect/reconnect while the session is alive.
        current.startContinuousAllMetrics { result ->
            if (!running || token != generation) return@startContinuousAllMetrics
            if (result.startsWith("ML-Test: Verbinde")) {
                updateNotification("CIRQA-Verbindung wird aufgebaut")
                return@startContinuousAllMetrics
            }
            probe = null
            if (!log.append("attempt", mapOf("number" to attemptNumber, "result" to result.take(600)))) {
                stopCollection("storage_limit")
            } else {
                scheduleReconnect(token, "connection_ended")
            }
        }
    }

    private fun scheduleReconnect(token: Int, reason: String) {
        if (!running || token != generation) return
        // Bounded backoff: 2, 4, 8, 16, 32, then 60 seconds.
        val shift = (attempts - 1).coerceIn(0, 5)
        val delayMs = (2_000L shl shift).coerceAtMost(60_000L)
        updateNotification("CIRQA getrennt · neuer Versuch in ${delayMs / 1000}s")
        log.append("reconnect_scheduled", mapOf("reason" to reason, "delay_ms" to delayMs))
        handler.postDelayed({
            if (running && token == generation) sample()
        }, delayMs)
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
            Intent(this, CirqaContinuousRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("PulseLibre · Kontinuierliche Aufzeichnung")
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

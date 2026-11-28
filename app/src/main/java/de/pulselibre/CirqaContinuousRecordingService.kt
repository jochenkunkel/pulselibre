package de.pulselibre

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.pm.PackageManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
        const val KEY_HEARTBEAT_MS = "heartbeat_ms"
        private const val CHANNEL = "cirqa_collection"
        private const val ID = 1401

        /** Test seam (instrumentation only; null in production): replaces the real band connection. */
        @Volatile internal var linkFactory: ((Context) -> RecorderLink)? = null
        /** Test seam: shortened stall window; production uses [RecorderStall.STALL_AFTER_MS]. */
        @Volatile internal var stallAfterMs: Long = RecorderStall.STALL_AFTER_MS
        /** Test seam: heartbeat period; production uses [RecorderStatus.HEARTBEAT_INTERVAL_MS]. */
        @Volatile internal var heartbeatIntervalMs: Long = RecorderStatus.HEARTBEAT_INTERVAL_MS
    }

    private val handler = Handler(Looper.getMainLooper())
    /**
     * #773: every log write opens SQLite and may wait for another connection's lock (S10 28.11.: ANR, main thread in
     * the busy handler). Writes run in order on this one thread; a failed write stops collection on the main thread.
     */
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()

    private fun write(type: String, fields: Map<String, Any?> = emptyMap(), stopOnFailure: Boolean = false) {
        val l = log
        writer.execute {
            if (!l.append(type, fields) && stopOnFailure) handler.post { if (running) stopCollection("storage_limit") }
        }
    }

    private fun writeReading(service: Int, reading: GarminRealtime.Reading) {
        val l = log
        writer.execute { if (!l.reading(service, reading)) handler.post { if (running) stopCollection("storage_limit") } }
    }
    private var probe: RecorderLink? = null
    private var running = false
    private var stopPending = false
    private lateinit var log: CirqaLocalLog
    private var attempts = 0
    /** Attempts since the last reading: drives the backoff, so a long good connection starts again at 2 s. */
    private var attemptsSinceReading = 0
    private var readings = 0
    private val attemptGate = RecorderAttemptGate()
    private var lastReadingPersistedMs = 0L
    private var attemptStartedMs = 0L
    private var lastReadingMs = 0L
    private var currentToken = 0
    private var bluetoothReceiver: BroadcastReceiver? = null

    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(AppLanguageSettings.wrap(newBase))

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        log = CirqaLocalLog(this)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.svc_channel), NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCollection("user_stop")
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (running || stopPending) return START_NOT_STICKY
        val notification = notification(getString(R.string.svc_collecting))
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
        if (linkFactory == null && (bluetooth == null || !bluetooth.isEnabled)) {
            updateNotification(getString(R.string.svc_bt_off))
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, true)
            .putLong(KEY_HEARTBEAT_MS, System.currentTimeMillis()).apply()
        heartbeat()
        registerBluetoothStateReceiver()
        write("session_start", mapOf("mode" to "continuous_notifications"), stopOnFailure = true)
        sample()
        return START_NOT_STICKY
    }

    /** Main-thread receiver (system broadcast, never exported) so state stays confined like every other callback. */
    private fun registerBluetoothStateReceiver() {
        if (bluetoothReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED || !running) return
                onBluetoothState(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR))
            }
        }
        bluetoothReceiver = receiver
        ContextCompat.registerReceiver(this, receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterBluetoothStateReceiver() {
        val receiver = bluetoothReceiver ?: return
        bluetoothReceiver = null
        try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) { /* already gone */ }
    }

    private fun onBluetoothState(state: Int) {
        when (RecorderBluetoothPolicy.actionFor(state)) {
            RecorderBluetoothPolicy.Action.NONE -> Unit
            RecorderBluetoothPolicy.Action.ABANDON_ATTEMPT -> {
                // The framework closed our GATT client without a callback: drop the dead attempt and invalidate every
                // pending callback and reconnect timer of it (new generation), then wait for Bluetooth to return.
                probe?.close()
                probe = null
                attemptGate.start()
                write("bluetooth_state", mapOf("state" to "off"))
                updateNotification(getString(R.string.svc_bt_off))
            }
            RecorderBluetoothPolicy.Action.RESTART_ATTEMPT -> {
                probe?.close()
                probe = null
                write("bluetooth_state", mapOf("state" to "on"))
                sample()
            }
        }
    }

    /** Liveness marker for the UI; stops with the service (a killed process stops writing it). */
    private fun heartbeat() {
        if (!running) return
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(KEY_HEARTBEAT_MS, System.currentTimeMillis()).apply()
        recycleIfStalled()
        handler.postDelayed({ heartbeat() }, heartbeatIntervalMs)
    }

    /** #25: connection open but no reading for [RecorderStall.STALL_AFTER_MS]: recycle our own connection via the bounded reconnect. */
    private fun recycleIfStalled() {
        val current = probe ?: return
        val now = System.currentTimeMillis()
        if (!RecorderStall.isStalled(attemptStartedMs, lastReadingMs, now, stallAfterMs)) return
        val reconnectToken = attemptGate.finish(currentToken, current) ?: return
        probe = null
        write("stall_detected", mapOf("attempt" to attempts, "silent_ms" to now - maxOf(attemptStartedMs, lastReadingMs)))
        current.close()
        scheduleReconnect(reconnectToken, "stall")
    }

    @SuppressLint("MissingPermission")
    private fun sample() {
        if (!running) return
        attempts++
        attemptsSinceReading++
        val token = attemptGate.start()
        currentToken = token
        attemptStartedMs = System.currentTimeMillis()
        val attemptNumber = attempts
        write("attempt_start", mapOf("number" to attemptNumber), stopOnFailure = true)
        val factory = linkFactory
        val current: RecorderLink = if (factory != null) factory(this) else {
            val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (adapter == null || !adapter.isEnabled) {
                scheduleReconnect(token, "bluetooth_off")
                return
            }
            CirqaMlHandshakeProbe(this, adapter)
        }
        probe = current
        check(attemptGate.attach(token, current))
        // GATT callbacks can arrive on binder threads. Serialize all service state
        // changes onto the main looper and reject stale callbacks by generation
        // AND probe ownership, including callbacks queued before a stop/reconnect.
        current.onReading = { service, reading ->
            handler.post {
                if (attemptGate.accepts(token, current)) {
                    readings++
                    attemptsSinceReading = 0
                    val nowMs = System.currentTimeMillis()
                    lastReadingMs = nowMs
                    if (nowMs - lastReadingPersistedMs >= 5_000L) {
                        lastReadingPersistedMs = nowMs
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putLong(KEY_LAST_READING_MS, nowMs).apply()
                    }
                    writeReading(service, reading)
                    else updateNotification(getString(R.string.svc_received, readings))
                }
            }
        }
        // Only the first terminal callback for this exact probe may end an attempt.
        // Nonterminal "Verbinde" notifications do not consume the attempt.
        current.startContinuousAllMetrics { result ->
            handler.post {
                if (attemptGate.accepts(token, current)) {
                    if (result.startsWith("ML-Test: Verbinde")) {
                        updateNotification(getString(R.string.svc_connecting))
                    } else {
                        // Consume this attempt before any side effects or retry.
                        val reconnectToken = attemptGate.finish(token, current)
                            ?: return@post
                        probe = null
                        write("attempt", mapOf("number" to attemptNumber, "result" to result.take(600)), stopOnFailure = true)
                        scheduleReconnect(reconnectToken, "connection_ended")
                    }
                }
            }
        }
    }

    private fun scheduleReconnect(token: Int, reason: String) {
        if (!attemptGate.canRetry(token)) return
        // Bounded backoff: 2, 4, 8, 16, 32, then 60 seconds, counted since the last reading (not since service start).
        val delayMs = RecorderBackoff.delayMs(attemptsSinceReading.coerceAtLeast(1))
        updateNotification(getString(R.string.svc_reconnect, delayMs / 1000))
        write("reconnect_scheduled", mapOf("reason" to reason, "delay_ms" to delayMs))
        handler.postDelayed({
            if (attemptGate.canRetry(token)) sample()
        }, delayMs)
    }

    private fun stopCollection(reason: String) {
        if (stopPending) return
        if (running) write("session_end", mapOf("reason" to reason, "attempts" to attempts, "readings" to readings))
        stopPending = true
        running = false
        attemptGate.stop()
        unregisterBluetoothStateReceiver()
        handler.removeCallbacksAndMessages(null)
        val closingProbe = probe
        probe = null
        if (closingProbe == null) {
            completeStop()
        } else {
            closingProbe.closeWhenQuiescent { handler.post { completeStop() } }
        }
    }

    /** Publish the terminal state only after the link has fenced and drained its writer work. */
    private fun completeStop() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, false).apply()
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
            .setContentTitle(getString(R.string.svc_title))
            .setContentText(message)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.svc_stop), stop).build())
            .build()
    }

    override fun onDestroy() {
        if (running) stopCollection("service_destroyed")
        writer.shutdown() // queued writes (e.g. session_end) still complete
        super.onDestroy()
    }
}

package de.pulselibre.positiontracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import de.pulselibre.R

/**
 * Own `location` foreground service for the position track (#788); the CIRQA recording service stays connectedDevice.
 * Uses every Android provider that is available (GNSS, network, fused) and stores each fix with its own accuracy.
 */
class PositionTrackingService : Service() {
    private var thread: HandlerThread? = null
    private var store: PositionTrackStore? = null
    private var sessionId: Long? = null
    private val listener = LocationListener { onLocation(it) }
    private var handler: Handler? = null
    /** Most accurate fix of the current 5-s window (owner 28.11.: one point, the most precise, every 5 s). */
    private var best: PositionPoint? = null
    private val flush = object : Runnable {
        override fun run() {
            val id = sessionId ?: return
            best?.let { try { store?.add(id, it) } catch (_: Exception) {} }
            best = null
            handler?.postDelayed(this, WINDOW_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopTracking(); return START_NOT_STICKY }
            ACTION_START -> startTracking()
            else -> { stopSelf(); return START_NOT_STICKY }
        }
        return START_NOT_STICKY
    }

    private fun granted() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // granted() is checked at the start of this function
    private fun startTracking() {
        if (sessionId != null) return
        if (!PositionTracking.isEnabled(this) || !granted()) { stopSelf(); return }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.pos_channel), NotificationManager.IMPORTANCE_LOW))
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(ID, notification())
        } catch (_: Exception) { stopSelf(); return }
        val db = PositionTrackStore(this).also { store = it }
        db.closeDangling()
        val id = db.startSession(System.currentTimeMillis(), null)
        sessionId = id
        PositionTracking.setActive(this, id)
        val t = HandlerThread("position-track").also { it.start(); thread = it }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val wanted = buildList {
            add(LocationManager.GPS_PROVIDER); add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
        }
        val h = Handler(t.looper).also { handler = it }
        h.postDelayed(flush, WINDOW_MS)
        h.post {
            for (p in wanted) {
                try {
                    if (lm.allProviders.contains(p)) lm.requestLocationUpdates(p, INTERVAL_MS, 0f, listener, t.looper)
                } catch (_: SecurityException) {} catch (_: IllegalArgumentException) {}
            }
        }
    }

    private fun onLocation(l: Location) {
        if (sessionId == null) return
        val point = PositionPoint(
            timeMs = l.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            latitude = l.latitude, longitude = l.longitude,
            accuracyMeters = if (l.hasAccuracy()) l.accuracy else null,
            provider = l.provider,
            altitudeMeters = if (l.hasAltitude()) l.altitude else null,
            speedMps = if (l.hasSpeed()) l.speed else null,
            bearingDeg = if (l.hasBearing()) l.bearing else null
        )
        if (!point.isValid()) return
        best = PositionBest.pick(best, point)
    }

    private fun stopTracking() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try { lm.removeUpdates(listener) } catch (_: Exception) {}
        handler?.removeCallbacks(flush)
        val pending = best; best = null
        sessionId?.let { id -> pending?.let { try { store?.add(id, it) } catch (_: Exception) {} } }
        sessionId?.let { id -> try { store?.endSession(id, System.currentTimeMillis()) } catch (_: Exception) {} }
        sessionId = null
        PositionTracking.setActive(this, null)
        handler = null
        thread?.quitSafely(); thread = null
        store?.close(); store = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (sessionId != null) stopTracking()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(this, 1, Intent(this, PositionTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.pos_notification_title))
            .setContentText(getString(R.string.pos_notification_text))
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.pos_stop), stop).build())
            .build()
    }

    companion object {
        const val ACTION_START = "de.pulselibre.position.START"
        const val ACTION_STOP = "de.pulselibre.position.STOP"
        private const val CHANNEL = "position_track"
        private const val ID = 1788
        private const val INTERVAL_MS = 1_000L
        private const val WINDOW_MS = 5_000L
    }
}

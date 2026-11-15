package de.pulselibre

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: BluetoothAdapter
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var devices: LinearLayout
    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var scanGeneration = 0
    private var destroyed = false
    private var sessionState = BleSessionState.IDLE
    private fun applyEvent(event: BleEvent) {
        sessionState = transition(sessionState, event)
        status.text = "Bluetooth: ${sessionState.name}"
    }
    private val found = mutableSetOf<String>()
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        val violet = Color.rgb(81, 39, 201)
        val ink = Color.rgb(35, 29, 59)
        val muted = Color.rgb(109, 104, 127)
        val surface = Color.rgb(248, 247, 252)
        fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
        fun rounded(color: Int, radius: Int = 20) = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
        }
        fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false) =
            TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                includeFontPadding = false
            }
        fun card(): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.WHITE)
            elevation = dp(1).toFloat()
        }
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
            fitsSystemWindows = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(violet, Color.rgb(94, 65, 187))
            )
        }
        header.addView(text("PulseLibre", 28f, Color.WHITE, true))
        header.addView(text("Deine Daten. Deine Kontrolle. · Offline", 13f, Color.WHITE))
        screen.addView(header)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        screen.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val overview = mutableListOf<android.view.View>()
        val deviceViews = mutableListOf<android.view.View>()
        fun overviewView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            overview.add(view)
        }
        fun deviceView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            deviceViews.add(view)
            view.visibility = android.view.View.GONE
        }
        overviewView(text("Gesundheit im Blick", 22f, ink, true), 5)
        overviewView(text("Deine Werte bleiben auf diesem Gerät.", 13f, muted), 16)
        val metrics = dashboardMetrics(HealthSnapshot())
        // Keep all six semantic cards directly in the scroll root for accessibility and regression tests.
        metrics.forEachIndexed { index, metric ->
            val item = card().apply {
                tag = "metric_$index"
                contentDescription = "${metric.title}: ${metric.value}. ${metric.note}"
            }
            item.addView(text(metric.title, 14f, violet, true))
            item.addView(text(metric.value, 19f, ink, true),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            item.addView(text(metric.note, 12f, muted),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
            overviewView(item)
        }
        val deviceHeading = text("Bluetooth · Geräte", 22f, ink, true)
        deviceView(deviceHeading)
        deviceView(text("Garmin CIRQA · Sichere BLE-Diagnose ohne Neu-Kopplung", 13f, muted))
        status = text("Offline · bereit", 14f, muted)
        deviceView(status)
        fun actionButton(label: String, primary: Boolean = true, action: () -> Unit) =
            TextView(this).apply {
                text = label
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(if (primary) Color.WHITE else violet)
                background = rounded(if (primary) violet else Color.rgb(237, 231, 253), 14)
                minHeight = dp(52)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        deviceView(actionButton("Bluetooth-Geräte suchen") { startScan() })
        deviceView(actionButton("Trennen", false) { disconnect() })
        deviceView(actionButton("Verbindungsfehler simulieren", false) { simulateFailure() })
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        deviceView(devices)
        output = text("Keine Geräte verbunden.", 13f, muted).apply {
            setTextIsSelectable(true)
        }
        deviceView(output)
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        val tabs = mutableListOf<TextView>()
        fun selectTab(index: Int) {
            overview.forEach { it.visibility = if (index == 0) android.view.View.VISIBLE else android.view.View.GONE }
            deviceViews.forEach { it.visibility = if (index == 1) android.view.View.VISIBLE else android.view.View.GONE }
            tabs.forEachIndexed { i, tab ->
                tab.setTextColor(if (i == index) violet else muted)
                tab.background = if (i == index) rounded(Color.rgb(241, 235, 255), 12) else null
            }
            scroll.scrollTo(0, 0)
        }
        listOf("Übersicht", "Geräte", "Export").forEachIndexed { index, label ->
            val tab = text(label, 14f, muted, true).apply {
                gravity = android.view.Gravity.CENTER
                minHeight = dp(52)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (index == 2) startActivity(Intent(this@MainActivity, ExportActivity::class.java))
                    else selectTab(index)
                }
            }
            nav.addView(tab, LinearLayout.LayoutParams(0, -2, 1f))
            tabs.add(tab)
        }
        screen.addView(nav)
        setContentView(screen)
        selectTab(0)
        // Permission prompts are deferred until an explicit BLE action.
    }

    /** Local simulation only; never touches Bluetooth hardware or Garmin Connect. */
    private fun simulateFailure() {
        stopScan()
        disconnect()
        applyEvent(BleEvent.RESET)
        applyEvent(BleEvent.START_SCAN)
        applyEvent(BleEvent.CONNECT)
        applyEvent(BleEvent.FAILED)
        output.append("\n[DEMO] Verbindungsfehler simuliert; keine echte Bluetooth-Verbindung.")
    }

    private fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun ensurePermissions(): Boolean {
        val missing = permissions().filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!ensurePermissions()) return
        if (!adapter.isEnabled) { status.text = "Bitte Bluetooth aktivieren"; return }
        if (scanning) return
        val scanner = adapter.bluetoothLeScanner ?: run { status.text = "BLE-Scanner nicht verfügbar"; return }
        devices.removeAllViews(); found.clear()
        try {
            scanner.startScan(callback)
        } catch (_: SecurityException) {
            status.text = "Bluetooth-Berechtigung fehlt"; return
        } catch (_: IllegalStateException) {
            status.text = "Bluetooth-Scanner momentan nicht bereit"; return
        }
        scanning = true
        val generation = ++scanGeneration
        applyEvent(BleEvent.START_SCAN)
        status.text = "Suche läuft (10 Sekunden) …"
        handler.postDelayed({ if (generation == scanGeneration && !destroyed) stopScan() }, BleSafetyPolicy.scanDurationMillis)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (!scanning) return
        try { adapter.bluetoothLeScanner?.stopScan(callback) }
        catch (_: SecurityException) { /* Permissions may have been revoked mid-scan. */ }
        catch (_: IllegalStateException) { /* Bluetooth may have been switched off. */ }
        scanning = false
        scanGeneration++
        applyEvent(BleEvent.STOP_SCAN)
        status.text = "Suche abgeschlossen · ${found.size} Geräte"
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread { if (scanning && !destroyed) showDevice(result) }
        }
        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                if (!destroyed) {
                    scanning = false
                    scanGeneration++
                    applyEvent(BleEvent.FAILED)
                    status.text = "Scan-Fehler: $errorCode"
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDevice(result: ScanResult) {
        val device = result.device
        if (!found.add(device.address)) return
        val label = device.name ?: result.scanRecord?.deviceName ?: "Unbekanntes Gerät"
        val button = Button(this).apply {
            text = "$label · RSSI ${result.rssi} dBm"
            setOnClickListener { connect(device) }
        }
        devices.addView(button)
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (!ensurePermissions()) return
        stopScan(); disconnect()
        applyEvent(BleEvent.CONNECT)
        status.text = "Verbinde mit ${device.name ?: "BLE-Gerät"} …"
        try { gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE) }
        catch (_: SecurityException) { applyEvent(BleEvent.FAILED); status.text = "Bluetooth-Berechtigung fehlt" }
        catch (_: IllegalStateException) { applyEvent(BleEvent.FAILED); status.text = "Bluetooth-Verbindung nicht möglich" }
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        try { gatt?.disconnect() } catch (_: SecurityException) { }
        try { gatt?.close() } catch (_: Exception) { }
        gatt = null
        applyEvent(BleEvent.DISCONNECTED)
    }

    private fun log(message: String) {
        runOnUiThread {
            if (!destroyed) output.append("\n$message")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, statusCode: Int, newState: Int) {
            if (gatt !== g || destroyed) { g.close(); return }
            if (statusCode == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { applyEvent(BleEvent.CONNECTED) }
                log("Verbunden. Dienste werden gesucht …")
                try { if (!g.discoverServices()) log("Dienstsuche konnte nicht gestartet werden") }
                catch (_: SecurityException) { log("Bluetooth-Berechtigung fehlt") }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || statusCode != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread { applyEvent(if (statusCode == BluetoothGatt.GATT_SUCCESS) BleEvent.DISCONNECTED else BleEvent.FAILED) }
                log("Getrennt (Status $statusCode)")
                g.close()
                if (gatt === g) gatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            if (gatt !== g || destroyed) return
            if (statusCode != BluetoothGatt.GATT_SUCCESS) { log("Dienstsuche fehlgeschlagen: $statusCode"); return }
            log("Dienste: ${g.services.size}")
            g.services.forEach { service ->
                log("Service ${service.uuid}")
                service.characteristics.forEach { ch ->
                    log("  Characteristic ${ch.uuid} · ${describeProperties(ch.properties)} · Lesen: ${BleSafetyPolicy.mayReadCharacteristic(ch.properties)}")
                }
            }
            log("Sicherheitsmodus: nur Diensterkennung, keine Kopplung, Schreibbefehle oder Abonnements.")
            log("Hinweis: Proprietäre Garmin-Werte werden noch nicht dekodiert.")
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            log("Notify ${characteristic.uuid}: ${BleSafetyPolicy.safeHex(value)}")
        }
        @Deprecated("Android legacy callback")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            log("Notify ${characteristic.uuid}: ${characteristic.value?.let { BleSafetyPolicy.safeHex(it) } ?: ""}")
        }
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        stopScan(); disconnect()
        super.onDestroy()
    }
}

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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(249, 247, 254))
        }
        val title = TextView(this).apply {
            text = "PulseLibre"
            textSize = 27f
            setTextColor(violet)
            setTypeface(null, Typeface.BOLD)
            contentDescription = "PulseLibre Gesundheitsübersicht"
        }
        val subtitle = TextView(this).apply {
            text = "Deine Daten. Deine Kontrolle. · Offline"
            textSize = 14f
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(Button(this).apply {
            text = "Einstellungen · Datenexport"
            setOnClickListener { startActivity(Intent(this@MainActivity, ExportActivity::class.java)) }
        })
        val metrics = dashboardMetrics(HealthSnapshot())
        metrics.forEachIndexed { index, metric ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 22f
                }
                contentDescription = "${metric.title}: ${metric.value}. ${metric.note}"
                tag = "metric_$index"
            }
            card.addView(TextView(this).apply {
                text = metric.title
                textSize = 17f
                setTextColor(violet)
                setTypeface(null, Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = metric.value
                textSize = 21f
                setTextColor(Color.rgb(34, 34, 34))
            })
            card.addView(TextView(this).apply {
                text = metric.note
                textSize = 12f
            })
            root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })
        }
        root.addView(TextView(this).apply {
            text = "Bluetooth · Geräte (experimentell)"
            textSize = 20f
            setTextColor(violet)
        })
        status = TextView(this).apply { text = "Offline · bereit" }
        val scan = Button(this).apply { text = "Bluetooth-Geräte suchen"; setOnClickListener { startScan() } }
        val simulate = Button(this).apply { text = "Demo: Verbindungsfehler simulieren"; setOnClickListener { simulateFailure() } }
        val disconnect = Button(this).apply { text = "Trennen"; setOnClickListener { disconnect() } }
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        output = TextView(this).apply { text = "Keine Geräte verbunden."; setTextIsSelectable(true) }
        root.addView(status)
        root.addView(scan)
        root.addView(simulate)
        root.addView(disconnect)
        root.addView(devices)
        root.addView(output)
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
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

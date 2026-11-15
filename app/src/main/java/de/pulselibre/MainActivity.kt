package de.pulselibre

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
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
    private val found = mutableSetOf<String>()
    private val cccd = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 30, 20, 20) }
        val title = TextView(this).apply { text = "PulseLibre · BLE Explorer"; textSize = 23f }
        status = TextView(this).apply { text = "Offline · bereit" }
        val scan = Button(this).apply { text = "Bluetooth-Geräte suchen"; setOnClickListener { startScan() } }
        val disconnect = Button(this).apply { text = "Trennen"; setOnClickListener { disconnect() } }
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        output = TextView(this).apply { text = "Wähle ein gefundenes Gerät."; setTextIsSelectable(true) }
        val scroll = ScrollView(this).apply { addView(output) }
        root.addView(title); root.addView(status); root.addView(scan); root.addView(disconnect)
        root.addView(devices)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        ensurePermissions()
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
        scanner.startScan(callback)
        scanning = true
        status.text = "Suche läuft (10 Sekunden) …"
        handler.postDelayed({ stopScan() }, 10_000)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (!scanning) return
        adapter.bluetoothLeScanner?.stopScan(callback)
        scanning = false
        status.text = "Suche abgeschlossen · ${found.size} Geräte"
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread { showDevice(result) }
        }
        override fun onScanFailed(errorCode: Int) {
            runOnUiThread { scanning = false; status.text = "Scan-Fehler: $errorCode" }
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
        status.text = "Verbinde mit ${device.name ?: "BLE-Gerät"} …"
        gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private fun log(message: String) {
        runOnUiThread {
            output.append("\n$message")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, statusCode: Int, newState: Int) {
            if (statusCode == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                log("Verbunden. Dienste werden gesucht …")
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || statusCode != BluetoothGatt.GATT_SUCCESS) {
                log("Getrennt (Status $statusCode)")
                g.close()
                if (gatt === g) gatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            if (statusCode != BluetoothGatt.GATT_SUCCESS) { log("Dienstsuche fehlgeschlagen: $statusCode"); return }
            log("Dienste: ${g.services.size}")
            g.services.forEach { service ->
                log("Service ${service.uuid}")
                service.characteristics.forEach { ch ->
                    log("  Characteristic ${ch.uuid} · ${describeProperties(ch.properties)}")
                }
            }
            log("Hinweis: Proprietäre Garmin-Werte werden noch nicht dekodiert.")
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            log("Notify ${characteristic.uuid}: ${value.toHex()}")
        }
        @Deprecated("Android legacy callback")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            log("Notify ${characteristic.uuid}: ${characteristic.value?.toHex() ?: ""}")
        }
    }

    override fun onDestroy() {
        stopScan(); disconnect()
        super.onDestroy()
    }
}

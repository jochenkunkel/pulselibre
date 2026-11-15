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

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: BluetoothAdapter
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var devices: LinearLayout
    private lateinit var cirqaCard: TextView
    private var scanning = false
    private var scanGeneration = 0
    private var destroyed = false
    private var sessionState = BleSessionState.IDLE
    private fun applyEvent(event: BleEvent) {
        sessionState = transition(sessionState, event)
        status.text = "Bluetooth: ${sessionState.name}"
    }
    private val found = mutableSetOf<String>()
    private val deviceRows = mutableMapOf<String, TextView>()
    private val diagnosticRows = mutableMapOf<String, String>()
    private val bondedRows = mutableListOf<String>()
    private var cirqaBondAddress: String? = null
    private var cirqaScanMatch = false
    private var cirqaGattState = "Nicht abgefragt"
    private val cirqaConnectionHistory = CirqaConnectionHistory()
    private var lastScanDiagnostic = "Noch kein Scan gestartet."

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
        deviceView(text("Garmin CIRQA · Passive Diagnose ohne Verbindungsaufbau", 13f, muted))
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
        cirqaCard = text("CIRQA Smart Band", 15f, ink, true).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.WHITE)
        }
        deviceView(cirqaCard)
        deviceView(actionButton("Bluetooth-Geräte suchen") { startScan() })
        deviceView(actionButton("Diagnose teilen", false) { shareDiagnostics() })
        deviceView(actionButton("Gekoppelte Geräte anzeigen", false) { showBondedDevices() })
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
            if (index == 1) refreshCirqaCard()
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

    private fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun ensurePermissions(): Boolean {
        val missing = permissions().filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            status.text = "Bluetooth-Berechtigung benötigt: Geräte in der Android-Abfrage zulassen"
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && ::status.isInitialized) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            status.text = if (granted) "Bluetooth-Berechtigung erteilt · Scan erneut starten"
                else "Bluetooth-Berechtigung abgelehnt · Einstellungen → Apps → PulseLibre → Berechtigungen → Geräte in der Nähe"
            lastScanDiagnostic = status.text.toString()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!ensurePermissions()) return
        if (!adapter.isEnabled) { status.text = "Bitte Bluetooth aktivieren"; lastScanDiagnostic = status.text.toString(); return }
        if (scanning) return
        val scanner = adapter.bluetoothLeScanner ?: run { status.text = "BLE-Scanner nicht verfügbar"; return }
        devices.removeAllViews(); found.clear(); deviceRows.clear(); diagnosticRows.clear()
        cirqaScanMatch = false
        refreshCirqaCard()
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
        status.text = "Suche läuft (60 Sekunden) · passive Erkennung …"
        lastScanDiagnostic = "Scan gestartet; Android hat den Startaufruf angenommen."
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
        lastScanDiagnostic = if (found.isEmpty()) "60 Sekunden ohne BLE-Werbeergebnis; kein Beweis für fehlende Kopplung." else "Scan abgeschlossen mit ${found.size} Geräten."
        refreshCirqaCard()
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
                    lastScanDiagnostic = "Android BLE ScanCallback Fehlercode: $errorCode"
                }
            }
        }
    }

    /** Reads Android bond metadata only; no GATT connection or bond changes. */
    @SuppressLint("MissingPermission")
    private fun refreshCirqaCard() {
        if (Build.VERSION.SDK_INT >= 31 && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            cirqaBondAddress = null
            cirqaCard.text = "CIRQA Smart Band\nBluetooth-Berechtigung erforderlich"
            return
        }
        try {
            val matched = adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
            cirqaBondAddress = matched?.address
            cirqaGattState = if (matched == null) "Kein gekoppeltes CIRQA" else try {
                val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                when (manager.getConnectionState(matched, BluetoothProfile.GATT)) {
                    BluetoothProfile.STATE_CONNECTED -> "Android GATT: verbunden (App unbekannt)"
                    BluetoothProfile.STATE_CONNECTING -> "Android GATT: verbindet"
                    BluetoothProfile.STATE_DISCONNECTING -> "Android GATT: trennt"
                    BluetoothProfile.STATE_DISCONNECTED -> "Android GATT: nicht verbunden"
                    else -> "Android GATT: unbekannt"
                }
            } catch (_: SecurityException) {
                "Android GATT: Berechtigung fehlt"
            } catch (_: IllegalArgumentException) {
                "Android GATT: Status nicht verfügbar"
            }
            cirqaConnectionHistory.observe(System.currentTimeMillis(), when {
                matched == null -> CirqaConnectionHistory.State.NOT_PAIRED
                cirqaGattState.contains("nicht verbunden") -> CirqaConnectionHistory.State.DISCONNECTED
                cirqaGattState.contains("verbindet") -> CirqaConnectionHistory.State.CONNECTING
                cirqaGattState.contains("trennt") -> CirqaConnectionHistory.State.DISCONNECTING
                cirqaGattState.contains("verbunden") -> CirqaConnectionHistory.State.CONNECTED
                cirqaGattState.contains("Berechtigung") -> CirqaConnectionHistory.State.PERMISSION_DENIED
                else -> CirqaConnectionHistory.State.UNKNOWN
            })
            val message = if (matched != null)
                "CIRQA Smart Band\n✓ Mit Android gekoppelt\n" +
                (if (cirqaScanMatch) "✓ Gleiche BLE-Adresse im Scan erkannt\n" else "BLE-Werbeadresse nicht bestätigt (wechselnde Adressen möglich)\n") +
                "${cirqaGattState}\nGarmin Connect bleibt unverändert\nKeine PulseLibre-Verbindung aufgebaut"
            else "CIRQA Smart Band\nNicht in Android-Kopplungen gefunden\nKeine Verbindung hergestellt"
            cirqaCard.text = message
            cirqaCard.contentDescription = message
        } catch (_: SecurityException) {
            cirqaCard.text = "CIRQA Smart Band\nBluetooth-Berechtigung fehlt"
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBondedDevices() {
        if (!ensurePermissions()) return
        refreshCirqaCard()
        bondedRows.clear()
        try {
            adapter.bondedDevices.orEmpty().sortedBy { it.name ?: "" }.forEach { device ->
                bondedRows.add("${device.name ?: "Unbenannt"} · bereits gekoppelt (Android)")
            }
            output.text = "Android-Kopplungen: ${bondedRows.size}\n" +
                bondedRows.joinToString("\n") +
                "\nHinweis: Kopplung bedeutet nicht, dass BLE-Werbedaten sichtbar sind."
        } catch (_: SecurityException) {
            output.text = "Bluetooth-Berechtigung für gekoppelte Geräte fehlt."
        }
    }

    private fun shareDiagnostics() {
        refreshCirqaCard()
        val report = buildString {
            appendLine("PulseLibre · Passive BLE-Diagnose")
            appendLine("Gefundene Werbegeräte: ${diagnosticRows.size}")
            appendLine("Scan-Status: $lastScanDiagnostic")
            appendLine("CIRQA GATT (Android-Systemstatus, keine eigene Verbindung): $cirqaGattState")
            appendLine("CIRQA-Statuswechsel (nur bei manuellen Abfragen): ${cirqaConnectionHistory.snapshot().size}")
            cirqaConnectionHistory.snapshot().forEach { entry ->
                appendLine("  ${entry.timestampMillis} · ${entry.state}")
            }
            appendLine("CIRQA-Kopplung: " + if (cirqaBondAddress != null) "vorhanden" else "nicht bestätigt")
            appendLine("CIRQA-Adressabgleich: " + if (cirqaScanMatch) "Treffer (lokal geprüft; Adresse nicht exportiert)" else "Kein bestätigter Treffer; wechselnde BLE-Adressen möglich")
            diagnosticRows.values.forEachIndexed { index, item ->
                appendLine("\nGerät ${index + 1}:\n$item")
            }
            appendLine("\nAndroid-Kopplungen: " + if (bondedRows.isEmpty()) "Liste nicht explizit abgefragt" else "Liste explizit abgefragt")
            bondedRows.forEach { appendLine(it) }
            appendLine("\nKeine Verbindung, Kopplung oder Schreiboperation durch diesen Scan.")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
        }, "BLE-Diagnose teilen"))
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        stopScan()
        super.onDestroy()
    }
    @SuppressLint("MissingPermission")
    private fun showDevice(result: ScanResult) {
        val device = result.device
        val address = device.address
        if (cirqaBondAddress != null && address.equals(cirqaBondAddress, ignoreCase = true)) {
            cirqaScanMatch = true
            refreshCirqaCard()
        }
        val record = result.scanRecord
        val label = try { device.name } catch (_: SecurityException) { null }
        val companyIds = mutableListOf<Int>()
        record?.manufacturerSpecificData?.let { data ->
            for (index in 0 until data.size()) companyIds.add(data.keyAt(index))
        }
        val services = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
        val serviceData = record?.serviceData?.entries?.joinToString("; ") { (uuid, bytes) ->
            "${uuid.uuid}: ${BleSafetyPolicy.safeHex(bytes)}"
        } ?: "keine"
        val description = BleAdvertisementIdentity.describe(
            label ?: record?.deviceName, companyIds, services, result.rssi
        )
        val isCirqaMatch = cirqaBondAddress != null && address.equals(cirqaBondAddress, ignoreCase = true)
        val details = (if (isCirqaMatch) "✓ CIRQA: BLE-Adresse stimmt mit Android-Kopplung überein\n" else "") +
            "$description\nService-Daten: $serviceData"
        diagnosticRows[address] = details
        val row = deviceRows[address]
        if (row != null) {
            row.text = details
            row.contentDescription = details
            return
        }
        if (!found.add(address)) return
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()
        val item = TextView(this).apply {
            text = details
            textSize = 14f
            setTextColor(Color.rgb(35, 29, 59))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            contentDescription = details
            isClickable = false
        }
        deviceRows[address] = item
        devices.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
}

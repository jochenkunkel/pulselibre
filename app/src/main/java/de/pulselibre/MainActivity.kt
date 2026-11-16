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
    private var dashboardRefresh: (() -> Unit)? = null
    private var dashboardVisible = true
    private var devicesVisible = false
    private var debugDevicesVisible = false
    private var recorderRefresh: (() -> Unit)? = null
    private var dashboardPolling = false
    private var dashboardLastCount = -1L
    private val dashboardPoll = object : Runnable {
        override fun run() {
            if (!dashboardPolling || destroyed) return
            recorderRefresh?.invoke()
            if (dashboardVisible) {
                try {
                    val count = CirqaDatabase(this@MainActivity).use { it.eventCount() }
                    if (count != dashboardLastCount) {
                        dashboardLastCount = count
                        dashboardRefresh?.invoke()
                    }
                } catch (_: Exception) {
                    // A temporary database read error must not crash the dashboard.
                }
            }
            if (dashboardPolling && !destroyed) handler.postDelayed(this, 1000L)
        }
    }
    private fun startDashboardPolling() {
        if (dashboardPolling) return
        dashboardPolling = true
        dashboardLastCount = -1L
        handler.post(dashboardPoll)
    }
    private fun stopDashboardPolling() {
        dashboardPolling = false
        handler.removeCallbacks(dashboardPoll)
    }
    override fun onResume() {
        super.onResume()
        startDashboardPolling()
        if (::adapter.isInitialized && ::status.isInitialized && !adapter.isEnabled) {
            status.text = "Bluetooth ist ausgeschaltet – CIRQA-Messwerte können nicht empfangen werden."
        }
    }
    override fun onPause() {
        stopDashboardPolling()
        super.onPause()
    }

    private val exportCirqaLog = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/x-ndjson")
    ) { uri ->
        if (uri != null) try {
            CirqaLocalLog(this).copyTo(this, uri)
            Toast.makeText(this, "CIRQA-Protokoll exportiert", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Export fehlgeschlagen", Toast.LENGTH_LONG).show()
        }
    }
    private lateinit var adapter: BluetoothAdapter
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var devices: LinearLayout
    private lateinit var cirqaCard: TextView
    private lateinit var cirqaEvidenceView: TextView
    private val cirqaScanEvidence = CirqaScanEvidence()
    private var cirqaGattProbe: CirqaGattProbe? = null
    private var cirqaMlProbe: CirqaMlHandshakeProbe? = null
    private var lastHistoryDiagnostic: String? = null
    private val passiveRssiTracker = PassiveRssiTracker()
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
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        headerRow.addView(text("PulseLibre", 28f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        // Personal Easter egg: the dedicated humerus SVG-matched vector replaces the gear.
        // Keep a 48dp touch target and an explicit accessibility label.
        val avatarButton = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_humerus_settings)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = "Einstellungen und persönliches Profil öffnen"
            setPadding(dp(2), dp(2), dp(2), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { PersonalSettings.show(this@MainActivity) {} }
        }
        headerRow.addView(avatarButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(headerRow)
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
        // Compact personal body card: user-entered profile, not measured BLE data.
        val bodyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            background = rounded(Color.WHITE)
        }
        bodyCard.addView(text("MEIN KÖRPERPROFIL  ·  Tippen zum Bearbeiten", 12f, violet, true))
        val bodyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val bodyFigure = TextView(this).apply {
            text = PersonalSettings.bodyAvatar(this@MainActivity)
            textSize = 76f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.rgb(185, 164, 239))
            contentDescription = "Symbolische Personendarstellung"
        }
        bodyRow.addView(bodyFigure, LinearLayout.LayoutParams(0, dp(112), 1f))
        val bodyValues = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        bodyRow.addView(bodyValues, LinearLayout.LayoutParams(0, -2, 2f))
        val healthInfo = TextView(this).apply {
            text = "ⓘ"
            textSize = 26f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.rgb(30, 135, 70))
            minWidth = dp(48)
            minHeight = dp(48)
            contentDescription = "Informationen zu empfangenen Messungen und lokaler Speicherung"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val summary = CirqaDashboardReader(this@MainActivity).read()
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Deine Messdaten")
                    .setMessage("${summary.samples} empfangene Messungen\n" +
                        "Originalprotokoll: ${summary.bytes / 1024} KiB\n\n" +
                        "Deine Werte bleiben auf diesem Gerät. Keine Cloud.\n" +
                        "Messzeit = Empfangszeit. Keine medizinische Diagnose.")
                    .setPositiveButton("Schließen", null)
                    .show()
            }
        }
        // Align the information button with the mannequin, not the dashboard heading.
        bodyRow.addView(healthInfo, LinearLayout.LayoutParams(dp(48), dp(48)))
        bodyCard.addView(bodyRow)
        fun refreshBodyCard() {
            val p = getSharedPreferences("local_personal_profile", MODE_PRIVATE)
            bodyValues.removeAllViews()
            bodyFigure.text = PersonalSettings.bodyAvatar(this@MainActivity)
            bodyFigure.contentDescription = "Körperprofil-Avatar: ${p.getString("gender", "Keine Angabe")}"
            listOf(
                Triple("↕", "Größe", p.getString("height_cm", "").orEmpty().let { if (it.isBlank()) "–" else it + " cm" }),
                Triple("⚖", "Gewicht", p.getString("weight_kg", "").orEmpty().let { if (it.isBlank()) "–" else it + " kg" }),
                Triple("◷", "Alter", p.getString("age_years", "").orEmpty().let { if (it.isBlank()) "–" else it + " Jahre" })
            ).forEach { (icon, title, value) ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                row.addView(text(icon + "  ", 17f, violet, true))
                row.addView(text(title + "  ", 12f, muted))
                row.addView(text(value, 15f, ink, true))
                bodyValues.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
            bodyCard.contentDescription = "Mein Körperprofil. " +
                (0 until bodyValues.childCount).joinToString(", ") { i ->
                    val row = bodyValues.getChildAt(i) as LinearLayout
                    (0 until row.childCount).joinToString(" ") { j -> (row.getChildAt(j) as TextView).text }
                }
        }
        refreshBodyCard()
        bodyCard.isClickable = true
        bodyCard.isFocusable = true
        bodyCard.setOnClickListener {
            PersonalSettings.show(this) {
                refreshBodyCard()
            }
        }
        overviewView(bodyCard, 14)
        // One compact dashboard heading; privacy details remain accessible via the info button.
        overviewView(text("Gesundheit · CIRQA", 17f, ink, true), 6)
        val cirqaDashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun updateCirqaDashboard() {
            val summary = CirqaDashboardReader(this).read()
            cirqaDashboard.removeAllViews()
            // The first four cards are high-priority measurements, not derived health claims.
            val primary = summary.metrics.take(4)
            val tiles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            primary.chunked(2).forEach { pair ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                pair.forEachIndexed { index, metric ->
                    val tile = card().apply {
                        contentDescription = "${metric.title}: ${metric.value}. ${metric.note}"
                        addView(text(metric.title, 12f, muted, true))
                        addView(text(metric.value, 22f, ink, true),
                            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
                        addView(text(metric.note, 10f, muted),
                            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
                    }
                    row.addView(tile, LinearLayout.LayoutParams(0, -2, 1f).apply {
                        if (index == 0) rightMargin = dp(8)
                    })
                }
                tiles.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
            cirqaDashboard.addView(tiles)

            val detail = card()
            detail.addView(text("WEITERE GESUNDHEITSWERTE", 11f, violet, true))
            summary.metrics.drop(4).forEach { metric ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(13), 0, dp(13))
                    contentDescription = "${metric.title}: ${metric.value}. ${metric.note}"
                }
                item.addView(text(metric.title, 13f, muted, true))
                item.addView(text(metric.value, 19f, ink, true),
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
                item.addView(text(metric.note, 11f, muted),
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
                detail.addView(item)
                detail.addView(android.view.View(this).apply {
                    setBackgroundColor(Color.rgb(236, 233, 244))
                }, LinearLayout.LayoutParams(-1, dp(1)))
            }
            cirqaDashboard.addView(detail, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        updateCirqaDashboard()
        dashboardRefresh = { updateCirqaDashboard() }
        overviewView(cirqaDashboard)
        val deviceHeading = text("Bluetooth · Geräte", 22f, ink, true)
        deviceView(deviceHeading)
        val debugDeviceViews = mutableListOf<android.view.View>()
        fun debugView(view: android.view.View, margin: Int = 12) {
            deviceView(view, margin)
            debugDeviceViews.add(view)
        }
        deviceHeading.setOnLongClickListener {
            debugDevicesVisible = !debugDevicesVisible
            debugDeviceViews.forEach { it.visibility =
                if (devicesVisible && debugDevicesVisible) android.view.View.VISIBLE else android.view.View.GONE }
            Toast.makeText(this, if (debugDevicesVisible) "Debug-Funktionen sichtbar" else "Debug-Funktionen ausgeblendet", Toast.LENGTH_SHORT).show()
            true
        }
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
        cirqaEvidenceView = text(cirqaScanEvidence.summary(), 13f, muted).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(Color.WHITE)
        }
        debugView(cirqaEvidenceView)
        // Foreground service samples periodically; it is not a continuous raw BLE stream.
        val recorderState = text("○  Aufzeichnung aus", 15f, muted, true)
        deviceView(recorderState, 8)
        val recorderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val playButton = actionButton("▶  Starten") { /* Click handler installed below. */ }
        val stopButton = actionButton("■  Stoppen", false) {
            try {
                startService(Intent(this, CirqaOvernightService::class.java)
                    .setAction(CirqaOvernightService.ACTION_STOP))
                recorderState.text = "◌  Aufzeichnung wird beendet …"
            } catch (_: Exception) {
                output.text = "Stopp konnte nicht angefordert werden."
            }
        }
        playButton.setOnClickListener {
            if (!ensurePermissions()) return@setOnClickListener
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("CIRQA-Aufzeichnung starten")
                .setMessage("Garmin Connect vorher beenden. PulsLibre liest etwa alle zwei Minuten für bis zu 20 Sekunden verfügbare Echtzeitmesswerte und speichert sie lokal. Keine kontinuierliche Rohdatenaufzeichnung. Starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Starten") { _, _ ->
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(
                            this@MainActivity,
                            Intent(this@MainActivity, CirqaOvernightService::class.java)
                                .setAction(CirqaOvernightService.ACTION_START)
                        )
                        recorderState.text = "◌  Aufzeichnung wird gestartet …"
                    } catch (_: Exception) {
                        output.text = "Start fehlgeschlagen. Bluetooth- und Android-Berechtigungen prüfen."
                    }
                }.show()
        }
        recorderRow.addView(playButton, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        recorderRow.addView(stopButton, LinearLayout.LayoutParams(0, -2, 1f))
        deviceView(recorderRow, 12)
        recorderRefresh = {
            val running = getSharedPreferences(CirqaOvernightService.PREFS, MODE_PRIVATE)
                .getBoolean(CirqaOvernightService.KEY_RUNNING, false)
            recorderState.text = if (running) "●  Aufzeichnung läuft · lokale Messwerte" else "○  Aufzeichnung aus"
            recorderState.setTextColor(if (running) Color.rgb(30, 135, 70) else muted)
            playButton.alpha = if (running) 0.45f else 1f
            stopButton.alpha = if (running) 1f else 0.45f
            playButton.isEnabled = !running
            stopButton.isEnabled = running
        }
        recorderRefresh?.invoke()
        debugView(actionButton("Bluetooth-Geräte suchen") { startScan() })
        debugView(actionButton("Diagnose teilen", false) { shareDiagnostics() })
        debugView(actionButton("Gekoppelte Geräte anzeigen", false) { showBondedDevices() })
        debugView(actionButton("CIRQA: GATT-Dienste prüfen (aktive Verbindung)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Aktiver CIRQA-Verbindungstest")
                .setMessage("Garmin Connect vorher beenden. PulseLibre verbindet sich kurz mit dem gekoppelten CIRQA, liest nur die GATT-Dienststruktur und trennt die Verbindung wieder. Keine Schreibbefehle, kein CLOSE_ALL. Fortfahren?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Verbinden") { _, _ ->
                    if (cirqaGattProbe == null) cirqaGattProbe = CirqaGattProbe(this, adapter)
                    cirqaGattProbe?.start { message ->
                        runOnUiThread { output.text = message }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Historien-Diagnose (Phase 1)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Historien-Diagnose")
                .setMessage("Garmin Connect vorher vollständig beenden. Der Test registriert nur den Garmin-GFDI-Dienst, beobachtet 20 Sekunden lang Antwort-Metadaten und trennt die Verbindung. Es wird noch keine Dateiliste angefordert. Kein CLOSE_ALL, keine Dateiänderung, keine Rohdaten im Bericht. Starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Diagnose starten") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startHistoryDiagnostic { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Historien-Diagnose abgeschlossen")
                                    .setMessage(message)
                                    .setPositiveButton("Schließen", null)
                                    .setNeutralButton("Bericht teilen") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(Intent.createChooser(intent, "CIRQA-Diagnose teilen"))
                                    }
                                    .show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Bootstrap-ACK-Test (experimentell)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Aktiver Bootstrap-Test")
                .setMessage("Garmin Connect und laufende Aufzeichnungen vorher beenden. Nach einer gültigen DeviceInformation (5024) wird genau EIN experimenteller, identitätsfreier ACK-Kandidat gesendet. Keine Dateiliste, keine Dateien, keine Änderungen am Gerät. Abbruch nach 20 Sekunden. Das CIRQA-Verhalten ist unbekannt. Starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("ACK einmalig testen") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startBootstrapAckTest { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Bootstrap-ACK-Test abgeschlossen")
                                    .setMessage(message)
                                    .setPositiveButton("Schließen", null)
                                    .setNeutralButton("Bericht teilen") { _, _ ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(android.content.Intent.createChooser(intent, "Diagnosebericht teilen"))
                                    }
                                    .show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Bootstrap-Rohdaten (experimentell)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Rohdaten-Diagnose")
                .setMessage("Nur für Dummy-Daten: Dieser Test sendet EIN experimentelles DeviceInformation-ACK und erfasst bis zu 32 vollständige, CRC-geprüfte GFDI-Frames (maximal 8 KiB) als Hex im Ergebnisbericht. Enthalten sein können Gesundheitswerte, Seriennummern und Gerätekennungen. Die Daten werden nicht automatisch hochgeladen; beim Teilen bestimmst du selbst den Empfänger. Nach 20 Sekunden wird getrennt. Fortfahren?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Rohdaten erfassen") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startBootstrapRawCapture { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Bootstrap-Rohdaten abgeschlossen")
                                    .setMessage("Bis zu 32 vollständige GFDI-Frames wurden erfasst. Der Bericht kann Kennungen und Gesundheitswerte enthalten. Nur an vertrauenswürdige Empfänger weitergeben.")
                                    .setNegativeButton("Schließen", null)
                                    .setPositiveButton("Bericht teilen") { _, _ ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(android.content.Intent.createChooser(intent, "Rohdatenbericht teilen"))
                                    }
                                    .show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Dateiliste lesen (experimentell)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Dateiliste lesen (experimentell)")
                .setMessage("Garmin Connect und PulsLibre-Recorder vorher beenden. Es wird EIN FileSync-V2-Dateilistenbefehl an das Band gesendet. Keine Dateien werden heruntergeladen, gelöscht oder als synchronisiert markiert. Die CIRQA-Kompatibilität ist unbestätigt. Der Versuch endet nach 20 Sekunden. Fortfahren?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Dateiliste anfragen") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startFileListQuery { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Dateilisten-Test abgeschlossen")
                                    .setMessage(message)
                                    .setPositiveButton("Schließen", null)
                                    .setNeutralButton("Bericht teilen") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(Intent.createChooser(intent, "CIRQA-Dateilisten-Test teilen"))
                                    }.show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("Letzte Historien-Diagnose teilen", false) {
            val report = lastHistoryDiagnostic
            if (report == null) {
                output.text = "Noch keine abgeschlossene Historien-Diagnose in dieser App-Sitzung."
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                startActivity(Intent.createChooser(intent, "CIRQA-Diagnose teilen"))
            }
        })
        debugView(actionButton("CIRQA: Garmin-ML-Handshake testen", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Garmin Multi-Link Handshake")
                .setMessage("Garmin Connect vorher vollständig beenden. Der Test verbindet sich mit CIRQA, aktiviert GATT-Benachrichtigungen, sendet eine GFDI-Service-Registrierung und trennt die Verbindung. Keine Gesundheitsdaten werden gelesen. CLOSE_ALL ist optional.")
                .setNegativeButton("Abbrechen", null)
                .setNeutralButton("Mit CLOSE_ALL") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    cirqaMlProbe?.start(true) { message -> runOnUiThread { output.text = message } }
                }
                .setPositiveButton("Ohne CLOSE_ALL") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    cirqaMlProbe?.start(false) { message -> runOnUiThread { output.text = message } }
                }.show()
        })
        debugView(actionButton("CIRQA: Puls und Schritte live testen", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA Echtzeit-Messwerte")
                .setMessage("Garmin Connect vorher beenden. PulseLibre registriert GFDI, Puls und Schritte ohne CLOSE_ALL, wartet maximal 20 Sekunden auf echte Messwerte und trennt die Verbindung. Empfangene Messwerte werden lokal für die Übersicht gespeichert. Keine Kopplungsänderung. Fortfahren?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Live-Test starten") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    cirqaMlProbe?.onReading = { service, reading ->
                        CirqaLocalLog(this).reading(service, reading)
                    }
                    cirqaMlProbe?.startRealtime { message -> runOnUiThread {
                        output.text = message
                        if (!message.startsWith("ML-Test: Verbinde")) updateCirqaDashboard()
                    } }
                }.show()
        })
        debugView(actionButton("CIRQA: Alle Garmin-Messwerte testen", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA: Weitere Messwerte")
                .setMessage("Garmin Connect vorher beenden. PulseLibre versucht zusätzlich SpO₂, Atmung, HRV, Stress, Body Battery, Kalorien und Intensitätsminuten einzeln zu registrieren. Nicht unterstützte Dienste werden gemeldet. Maximal 20 Sekunden, kein CLOSE_ALL. Empfangene Messwerte werden lokal für die Übersicht gespeichert.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Messwerte prüfen") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    cirqaMlProbe?.onReading = { service, reading ->
                        CirqaLocalLog(this).reading(service, reading)
                    }
                    cirqaMlProbe?.startAllMetrics { message -> runOnUiThread {
                        output.text = message
                        if (!message.startsWith("ML-Test: Verbinde")) updateCirqaDashboard()
                    } }
                }.show()
        })
        debugView(actionButton("CIRQA: Bewegungssensor (Service 16) prüfen", false) {
            if (!ensurePermissions()) return@actionButton
            if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
            output.text = "Service 16: einmalige, zeitbegrenzte Prüfung · keine Rohdaten gespeichert"
            cirqaMlProbe?.startMotionDiscovery { message ->
                runOnUiThread {
                    output.text = message
                    android.app.AlertDialog.Builder(this)
                        .setTitle("CIRQA · Bewegungssensor-Test")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        })
        debugView(actionButton("CIRQA: Bewegungssensor – MousePods Start + Lesen", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · MousePods-Vergleichstest")
                .setMessage("Garmin Connect vorher vollständig beenden. Anders als beim normalen Service-16-Test registriert PulseLibre den Dienst und sendet anschließend das Startkommando 0x01 auf dem bestätigten Handle. Maximal 20 Sekunden; Paketanzahl wird angezeigt, keine Rohbewegungsdaten gespeichert. Beim Beenden wird 0x00 zum Stoppen versucht. Kein CLOSE_ALL, keine Kopplungsänderung. Jetzt testen?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("MousePods-Test starten") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    output.text = "MousePods-Vergleichstest läuft · maximal 20 Sekunden"
                    cirqaMlProbe?.startMousePodsMotion { message ->
                        runOnUiThread {
                            output.text = message
                            if (!message.startsWith("ML-Test: Verbinde")) {
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("CIRQA · MousePods-Ergebnis")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Nachtaufzeichnung starten (lokal)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lokale CIRQA-Nachtaufzeichnung")
                .setMessage("Garmin Connect vorher beenden. Die App prüft ungefähr alle 2 Minuten die Garmin-Echtzeitdienste (jeweils höchstens 20 Sekunden), schreibt Zeitstempel und echte Messwerte in eine private JSONL-Datei (max. 10 MB) und zeigt eine dauerhafte Benachrichtigung mit Stopp-Funktion. Keine historischen FIT-Daten, keine Cloud. Android kann die Sammlung bei Akkuoptimierung oder Bluetooth-Problemen unterbrechen. Jetzt starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Starten") { _, _ ->
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(
                            this, Intent(this, CirqaOvernightService::class.java)
                                .setAction(CirqaOvernightService.ACTION_START))
                        output.text = "CIRQA-Nachtaufzeichnung angefordert. Laufstatus in Android-Benachrichtigung; Daten bleiben lokal."
                    } catch (_: Exception) {
                        output.text = "Nachtaufzeichnung konnte nicht gestartet werden (Android-Berechtigung oder Hintergrundbeschränkung)."
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Nachtaufzeichnung stoppen", false) {
            startService(Intent(this, CirqaOvernightService::class.java)
                .setAction(CirqaOvernightService.ACTION_STOP))
            output.text = "Stopp der CIRQA-Nachtaufzeichnung angefordert."
        })
        debugView(actionButton("CIRQA: Lokale Dateistatistik", false) {
            val log = CirqaLocalLog(this)
            output.text = "CIRQA JSONL lokal: ${log.sizeBytes()} Bytes, ${log.lines()} Zeilen. Keine automatische Aggregation; Originalmessungen und Versuchsergebnisse bleiben erhalten."
        })
        debugView(actionButton("CIRQA: Nachtprotokoll exportieren", false) {
            exportCirqaLog.launch("pulselibre-cirqa-overnight.jsonl")
        })
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
            dashboardVisible = index == 0
            devicesVisible = index == 1
            if (dashboardVisible) dashboardLastCount = -1L
            overview.forEach { it.visibility = if (index == 0) android.view.View.VISIBLE else android.view.View.GONE }
            deviceViews.forEach { it.visibility = if (index == 1 && (debugDevicesVisible || it !in debugDeviceViews)) android.view.View.VISIBLE else android.view.View.GONE }
            if (index == 1) recorderRefresh?.invoke()
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
        requestBluetoothPermissionOnFirstLaunch()
    }

    private fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Ask once on first launch; never turn Bluetooth on or initiate a connection. */
    private fun requestBluetoothPermissionOnFirstLaunch() {
        val missing = permissions().filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) return
        val preferences = getSharedPreferences("bluetooth_permission", MODE_PRIVATE)
        if (!BluetoothPermissionPromptPolicy.shouldPromptOnLaunch(
                preferences.getBoolean("initial_prompt_shown", false), missing.size)) return
        preferences.edit().putBoolean("initial_prompt_shown", true).apply()
        ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
    }

    private fun ensureBluetoothEnabled(): Boolean {
        if (adapter.isEnabled) return true
        val message = "Bluetooth ist ausgeschaltet. Bitte aktiviere Bluetooth, damit PulsLibre CIRQA-Messwerte empfangen kann."
        status.text = message
        if (::output.isInitialized) output.text = message
        lastScanDiagnostic = message
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bluetooth ausgeschaltet")
            .setMessage(message)
            .setNegativeButton("Später", null)
            .setPositiveButton("Bluetooth aktivieren") { _, _ ->
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this, "Bluetooth bitte in den Android-Einstellungen einschalten.", Toast.LENGTH_LONG).show()
                }
            }
            .show()
        return false
    }

    private fun ensurePermissions(): Boolean {
        val missing = permissions().filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            status.text = "Bluetooth-Berechtigung benötigt: Geräte in der Android-Abfrage zulassen"
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
            return false
        }
        return ensureBluetoothEnabled()
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
        cirqaScanEvidence.reset()
        passiveRssiTracker.reset()
        cirqaEvidenceView.text = cirqaScanEvidence.summary()
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
            val failedCallback: ScanCallback = this
            runOnUiThread {
                if (!destroyed) {
                    // Best-effort cleanup; preserve the failure state and message.
                    if (scanning) {
                        try { adapter.bluetoothLeScanner?.stopScan(failedCallback) }
                        catch (_: SecurityException) { /* Permission revoked. */ }
                        catch (_: IllegalStateException) { /* Bluetooth switched off. */ }
                    }
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
            cirqaGattState = CirqaStateMapping.label(CirqaConnectionHistory.State.PERMISSION_DENIED)
            cirqaConnectionHistory.observe(System.currentTimeMillis(), CirqaConnectionHistory.State.PERMISSION_DENIED)
            cirqaCard.text = "CIRQA Smart Band\nBluetooth-Berechtigung erforderlich"
            return
        }
        try {
            val matched = adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
            cirqaBondAddress = matched?.address
            val observedState = if (matched == null) CirqaConnectionHistory.State.NOT_PAIRED else try {
                val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                CirqaStateMapping.fromAndroidState(manager.getConnectionState(matched, BluetoothProfile.GATT))
            } catch (_: SecurityException) {
                CirqaConnectionHistory.State.PERMISSION_DENIED
            } catch (_: IllegalArgumentException) {
                CirqaConnectionHistory.State.UNKNOWN
            }
            cirqaGattState = CirqaStateMapping.label(observedState)
            cirqaConnectionHistory.observe(System.currentTimeMillis(), observedState)
            val message = if (matched != null)
                "CIRQA Smart Band\n✓ Mit Android gekoppelt\n" +
                (if (cirqaScanMatch) "✓ Gleiche BLE-Adresse im Scan erkannt\n" else "BLE-Werbeadresse nicht bestätigt (wechselnde Adressen möglich)\n") +
                "${cirqaGattState}\nGarmin Connect bleibt unverändert\nKeine PulseLibre-Verbindung aufgebaut"
            else "CIRQA Smart Band\nNicht in Android-Kopplungen gefunden\nKeine Verbindung hergestellt"
            cirqaCard.text = message
            cirqaCard.contentDescription = message
        } catch (_: SecurityException) {
            cirqaBondAddress = null
            cirqaGattState = CirqaStateMapping.label(CirqaConnectionHistory.State.PERMISSION_DENIED)
            cirqaConnectionHistory.observe(System.currentTimeMillis(), CirqaConnectionHistory.State.PERMISSION_DENIED)
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
            appendLine("PulseLibre · BLE-Scan-Diagnose (aktive Probes separat)")
            appendLine("Gefundene Werbegeräte: ${diagnosticRows.size}")
            appendLine("Scan-Status: $lastScanDiagnostic")
            appendLine("CIRQA GATT (Android-Systemstatus; verantwortliche App unbekannt): $cirqaGattState")
            appendLine("CIRQA-Statuswechsel (nur bei manuellen Abfragen): ${cirqaConnectionHistory.snapshot().size}")
            cirqaConnectionHistory.snapshot().forEach { entry ->
                appendLine("  ${entry.timestampMillis} · ${entry.state}")
            }
            appendLine("CIRQA-Kopplung: " + if (cirqaBondAddress != null) "vorhanden" else "nicht bestätigt")
            appendLine("CIRQA-Adressabgleich: " + if (cirqaScanMatch) "Treffer (lokal geprüft; Adresse nicht exportiert)" else "Kein bestätigter Treffer; wechselnde BLE-Adressen möglich")
            appendLine(cirqaScanEvidence.summary())
            appendLine()
            appendLine(passiveRssiTracker.summary())
            // Never share raw BLE names, MAC addresses, UUID payloads or bond rows.
            // The detailed device list remains visible locally for diagnosis.
            appendLine("BLE-Geräteeinträge (nur Anzahl): ${diagnosticRows.size}")
            appendLine("Android-Kopplungsliste: " + if (bondedRows.isEmpty()) "nicht abgefragt oder leer" else "lokal abgefragt (Einträge nicht exportiert)")
            appendLine("\nDieser passive Scan baut keine Verbindung auf. Separate, manuell gestartete CIRQA-Probes können aktive Verbindungen, Benachrichtigungsregistrierung und Schreiboperationen verwenden.")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
        }, "BLE-Diagnose teilen"))
    }

    override fun onDestroy() {
        stopDashboardPolling()
        dashboardRefresh = null
        destroyed = true
        cirqaGattProbe?.close()
        cirqaMlProbe?.close()
        handler.removeCallbacksAndMessages(null)
        stopScan()
        super.onDestroy()
    }
    @SuppressLint("MissingPermission")
    private fun showDevice(result: ScanResult) {
        val device = result.device
        val address = device.address
        if (CirqaAddressMatch.matches(cirqaBondAddress, address)) {
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
        passiveRssiTracker.observe(address, result.rssi, services)
        cirqaScanEvidence.observeFeatures(address,
            !((label ?: record?.deviceName).isNullOrBlank()), companyIds.isNotEmpty(), services.isNotEmpty())
        cirqaScanEvidence.observe(address, CirqaAddressMatch.matches(cirqaBondAddress, address),
            BleAdvertisementIdentity.isGarminHint(label ?: record?.deviceName, companyIds, services))
        cirqaEvidenceView.text = cirqaScanEvidence.summary()
        val description = BleAdvertisementIdentity.describe(
            label ?: record?.deviceName, companyIds, services, result.rssi
        )
        val isCirqaMatch = CirqaAddressMatch.matches(cirqaBondAddress, address)
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

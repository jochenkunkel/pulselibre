package de.pulselibre

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

/**
 * User-facing safe laboratory. It deliberately does not expose experimental FileSync,
 * handshake, bootstrap, raw-frame, or motion-command probes.
 */
class CirqaTestLaboratoryActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val preferences by lazy { getSharedPreferences("cirqa_test_laboratory", MODE_PRIVATE) }
    private val records = CirqaTestLaboratory.initialRecords().associateBy { it.test }.toMutableMap()
    private val statusViews = mutableMapOf<CirqaTestLaboratory.TestId, TextView>()
    private lateinit var adapter: BluetoothAdapter
    private lateinit var statusOutput: TextView
    private lateinit var scanOutput: TextView
    private lateinit var discoveryOutput: TextView
    private lateinit var liveOutput: TextView
    private lateinit var fitOutput: TextView
    private lateinit var storedReadingsOutput: TextView
    private lateinit var root: LinearLayout
    private lateinit var legacy5007Button: Button
    private lateinit var legacy5031Button: Button
    private lateinit var legacyHistoryDownloadButton: Button
    private lateinit var legacyHistoryFollowUpButton: Button
    private var historicalSyncProbe: CirqaMlHandshakeProbe? = null
    private var permissionAction: (() -> Unit)? = null
    private var scanCallback: ScanCallback? = null
    private var scanning = false
    private var scanEvents = 0
    private var matchedBondAdvertisement = false
    private var garminHintEvents = 0
    private val rssiSamples = ArrayList<Int>()
    private var gattProbe: CirqaGattProbe? = null
    private var mlProbe: CirqaMlHandshakeProbe? = null
    private var systemEventProbe: CirqaMlHandshakeProbe? = null
    private var rfcommRunner: CirqaRfcommRunner? = null
    private var liveReadingCount = 0
    private val liveKinds = mutableSetOf<String>()
    private var destroyed = false
    private var monitorRunning = false
    private var monitorStartedAt = 0L
    private var connectedMillis = 0L
    private var connectedSince: Long? = null
    private var connectionTransitions = 0
    private var lastConnectionState: CirqaConnectionHistory.State? = null
    private val scanTimeout = Runnable { finishPassiveScan() }
    private val connectionPoll = Runnable { sampleConnectionState() }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun ByteArray.hexBytes(): String =
        joinToString(" ") { "%02X".format(Locale.ROOT, it.toInt() and 0xff) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val action = permissionAction
        permissionAction = null
        if (grants.isNotEmpty() && grants.values.all { it }) action?.invoke()
        else Toast.makeText(this, "Bluetooth-Berechtigung nicht erteilt; kein Test gestartet.", Toast.LENGTH_LONG).show()
    }

    private val fitPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) inspectFit(uri)
    }

    private val reportPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) try {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(CirqaTestLaboratory.report(records.values.toList(), System.currentTimeMillis()))
            } ?: error("No output stream")
            Toast.makeText(this, "Datensparsamer Testbericht gespeichert.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Bericht konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        loadRecords()
        buildScreen()
        refreshStatusRows()
        refreshStoredReadings()
    }

    private fun buildScreen() {
        val palette = AppPalette.of(this)
        fun card() = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(palette.card)
        }
        fun label(text: String, size: Float = 14f, bold: Boolean = false) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(if (bold) palette.ink else palette.muted)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setTextIsSelectable(true)
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text
            minHeight = dp(52)
            isAllCaps = false
            setOnClickListener { action() }
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            setBackgroundColor(palette.background)
        }
        root.addView(label("CIRQA-Testlabor", 24f, true))
        root.addView(label(
            "Sichere Banddiagnostik · passive Beobachtung · Offline-FIT",
            14f
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(16) })

        val warning = card()
        warning.addView(label("Begrenzter lokaler FIT-Abruf freigegeben", 17f, true))
        warning.addView(label(
            "Der freigegebene Einmalpfad lädt nur Directory-Index 0 und höchstens vier lesbare, durch den bestätigten 5031-Status unterstützte FIT-Dateien herunter. " +
                "Kein Archivieren, Flag-Ändern, Löschen, CLOSE_ALL oder FileSync-V2.",
            14f
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addCard(warning)

        val legend = card()
        legend.addView(label("Evidenz richtig einordnen", 16f, true))
        legend.addView(label(
            "Am CIRQA-Gerät bestätigt · Passiv beobachtet · Offline validiert · Unbewiesen\n" +
                "Ergebnis separat: Bestanden / Fehlgeschlagen / Nicht getestet / Nicht unterstützt.\n" +
                "Android-Kopplung, Name oder GATT-Status sind kein kryptographischer Identitätsnachweis.",
            13f
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addCard(legend)

        val statusCard = card()
        statusCard.addView(label("Android- und Verbindungsstatus", 16f, true))
        statusOutput = label("Noch nicht abgefragt.")
        statusCard.addView(statusOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        statusCard.addView(button("Bluetoothstatus einmal abfragen") {
            withBluetoothPermissions(needsScan = false) { captureAndroidStatus() }
        })
        statusCard.addView(button("Verbindungswechsel 30 Sekunden beobachten") {
            withBluetoothPermissions(needsScan = false) { startConnectionMonitor() }
        })
        addRecordView(statusCard, CirqaTestLaboratory.TestId.ANDROID_STATUS)
        addCard(statusCard)

        val scanCard = card()
        scanCard.addView(label("Passiver BLE-Scan", 16f, true))
        scanOutput = label("Noch kein Scan. Es wird nicht verbunden.")
        scanCard.addView(scanOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        scanCard.addView(button("Passiv 15 Sekunden scannen") {
            withBluetoothPermissions(needsScan = true) { startPassiveScan() }
        })
        scanCard.addView(button("Scan stoppen") { stopPassiveScan(userStopped = true) })
        addRecordView(scanCard, CirqaTestLaboratory.TestId.PASSIVE_SCAN)
        addCard(scanCard)

        val discoveryCard = card()
        discoveryCard.addView(label("GATT-Dienst-/Characteristic-Erkennung", 16f, true))
        discoveryOutput = label(
            "Nutzt ausschließlich die vorhandene zeitbegrenzte, lesefreie CirqaGattProbe. " +
                "Sie verbindet sich kurz, liest nur Strukturmetadaten und trennt wieder; keine GATT-Lese-/Schreibbefehle oder Notifications."
        )
        discoveryCard.addView(discoveryOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        discoveryCard.addView(button("Vorhandene Read-only-Dienstsuche starten") {
            withBluetoothPermissions(needsScan = false) { confirmGattDiscovery() }
        })
        addRecordView(discoveryCard, CirqaTestLaboratory.TestId.GATT_DISCOVERY)
        addCard(discoveryCard)

        val liveCard = card()
        liveCard.addView(label("Bereits unterstützte Live-Messwerte", 16f, true))
        liveOutput = label(
            "Nur die vorhandene Puls-/Schritte-Echtzeitprobe; nicht die erweiterten oder experimentellen Probes. " +
                "Empfangene Live-Werte werden wie bisher lokal gespeichert."
        )
        liveCard.addView(liveOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        liveCard.addView(button("Vorhandene Live-Messwertprobe starten") {
            withBluetoothPermissions(needsScan = false) { confirmLiveProbe() }
        })
        addRecordView(liveCard, CirqaTestLaboratory.TestId.LIVE_READINGS)
        addCard(liveCard)

        val storedCard = card()
        storedCard.addView(label("Passiv beobachtete Protokolle und lokale Logs", 16f, true))
        storedReadingsOutput = label("Wird lokal gelesen …")
        storedCard.addView(storedReadingsOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        storedCard.addView(label(
            "PulsLibre speichert derzeit keine passiven CIRQA-Rohframes. Die folgende SQLite-Statistik " +
                "umfasst vorhandene App-Ereignisse (z. B. aus aktiven Live-Proben) und ist kein passiver Mitschnitt.",
            13f
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addRecordView(storedCard, CirqaTestLaboratory.TestId.PASSIVE_PROTOCOL)
        storedCard.addView(button("Lokale Statistik aktualisieren") { refreshStoredReadings() })
        addCard(storedCard)

        val fitCard = card()
        fitCard.addView(label("FIT-Datei lokal prüfen", 16f, true))
        fitOutput = label(
            "Dateiauswahl über Android Storage Access Framework; maximal ${FitFileHeader.MAX_FILE_BYTES / (1024 * 1024)} MiB. " +
                "Keine allgemeine Dateiberechtigung, keine Kopie, kein Import in SQLite."
        )
        fitCard.addView(fitOutput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        fitCard.addView(button("FIT-Datei auswählen und offline analysieren") {
            AlertDialog.Builder(this)
                .setTitle("FIT-Datei lokal prüfen")
                .setMessage(
                    "FIT-Dateien können Gesundheits- und Aktivitätsdaten enthalten. " +
                        "Die ausgewählte Datei wird lokal und begrenzt geprüft, nicht kopiert, hochgeladen oder in PulsLibre importiert. " +
                        "Wähle nur eine Datei, die du prüfen möchtest."
                )
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Datei auswählen") { _, _ ->
                    fitPicker.launch(arrayOf("application/octet-stream", "application/vnd.ant.fit", "*/*"))
                }
                .show()
        })
        addRecordView(fitCard, CirqaTestLaboratory.TestId.FIT_FILE)
        addCard(fitCard)

        val historyCard = card()
        historyCard.addView(label("Historischer Downloadstatus", 16f, true))
        historyCard.addView(label(
            "Vor dem separat freigegebenen, begrenzten Einmaltransfer: unbewiesen. Der Transferbutton und seine festen Grenzen stehen weiter unten.",
            14f
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        addRecordView(historyCard, CirqaTestLaboratory.TestId.HISTORICAL_DOWNLOAD)
        addCard(historyCard)

        val systemEventCard = card()
        systemEventCard.addView(label(
            if (CirqaLegacy5030To5007AttemptGate.RELEASED_FOR_DEVICE_TEST)
                "5030-only · einmalig freigegeben" else "5030-only · Freigabe ausstehend", 18f
        ))
        systemEventCard.addView(label(
            "Vorbereitet: frischer Bootstrap und genau ein HANDSHAKE_COMPLETE-Event. " +
                "Nur äußerer Status, Ergebnislänge und erstes Ergebnisbyte werden festgehalten. " +
                "Keine Erfolgswert-Annahme, kein 5007/5031/5002. " +
                "Separates Einmal-Gate und ausdrückliche Bestätigung vor Verbindung.",
            13f
        ))
        systemEventCard.addView(button("Einmaligen 5030-only-Test bestätigen") {
            confirmLegacy5030OnlyTest()
        }.apply { isEnabled = CirqaLegacy5030To5007AttemptGate.RELEASED_FOR_DEVICE_TEST })
        addCard(systemEventCard)

        val syncCard = card()
        syncCard.addView(label("CIRQA Historische Synchronisation · Test V1", 18f))
        syncCard.addView(label(
            "Eine Testfunktion, drei freigabepflichtige Phasen: A = Handshake (5030+5007), " +
                "B = Dateitypen (5031), C = Verzeichnis (5002). Jede Phase hat ein eigenes " +
                "persistentes Einmal-Gate. Phase A erlaubt nicht automatisch B oder C. " +
                "Strikter Stopp nach jeder Phase, keine Retries, kein Download.",
            13f
        ))
        syncCard.addView(button("Phase A freigeben und ausführen") {
            confirmHistoricalSyncV1(1)
        })
        syncCard.addView(button("Phase B freigeben und ausführen") {
            confirmHistoricalSyncV1(2)
        })
        syncCard.addView(button("Phase C freigeben und ausführen") {
            confirmHistoricalSyncV1(3)
        })
        syncCard.addView(button("Verzeichnis lesen (8192 B, read-only)") {
            confirmDirectoryReadV2()
        })
        syncCard.addView(button("Verzeichnis-Lesestatus") {
            statusOutput.text = "Verzeichnis-Lesen:\n" +
                (preferences.getString("directoryReadV2Summary", null) ?: "Noch kein Lauf.")
        })
        syncCard.addView(button("Kette + Verzeichnis lesen (ein Lauf)") {
            confirmDirectoryAfterChain()
        })
        syncCard.addView(button("RFCOMM-Verzeichnis lesen (read-only)") {
            confirmRfcomm()
        })
        syncCard.addView(button("RFCOMM-Lesestatus") {
            statusOutput.text = "RFCOMM:\n" +
                (preferences.getString(CirqaRfcommAttemptGate.SUMMARY_KEY, null) ?: "Noch kein Lauf.") +
                "\nGate=${preferences.getBoolean(CirqaRfcommAttemptGate.PREFERENCE_KEY, false)}"
        })
        syncCard.addView(button("Sync anfordern (read-only)") {
            confirmSyncRequest()
        })
        syncCard.addView(button("Sync-Status") {
            statusOutput.text = "Sync-Request:\n" +
                (preferences.getString(CirqaSyncAttemptGate.SUMMARY_KEY, null) ?: "Noch kein Lauf.") +
                "\nGate=${preferences.getBoolean(CirqaSyncAttemptGate.PREFERENCE_KEY, false)}"
        })
        syncCard.addView(button("FileAccess-Dateiliste (read-only)") {
            confirmFileAccessList()
        })
        syncCard.addView(button("FileAccess-Status") {
            statusOutput.text = "FileAccess:\n" +
                (preferences.getString(CirqaFileAccessAttemptGate.SUMMARY_KEY, null) ?: "Noch kein Lauf.") +
                "\nGate=${preferences.getBoolean(CirqaFileAccessAttemptGate.PREFERENCE_KEY, false)}"
        })
        syncCard.addView(button("Status der historischen Synchronisation") {
            statusOutput.text = "Historische Synchronisation:\n" +
                "Gates: A=${preferences.getBoolean(CirqaHistoricalSyncV1MultiPhaseAttemptGate.keyForPhase(1), false)}" +
                " B=${preferences.getBoolean(CirqaHistoricalSyncV1MultiPhaseAttemptGate.keyForPhase(2), false)}" +
                " C=${preferences.getBoolean(CirqaHistoricalSyncV1MultiPhaseAttemptGate.keyForPhase(3), false)}\n" +
                (preferences.getString("historicalSyncV1Summary", null) ?: "Noch kein Lauf.")
        })
        addCard(syncCard)

        root.addView(button("Datensparsamen Testbericht speichern") {
            AlertDialog.Builder(this)
                .setTitle("Testbericht lokal speichern")
                .setMessage(
            "Der Standardbericht enthält keine Gerätekennung, Rohpayloads, FIT-Dateinamen oder Messwerte. " +
                        "Der Android-Dateidialog kann auch Cloud-Ziele anbieten; wähle nur einen Speicherort, dem du vertraust."
                )
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speicherort wählen") { _, _ ->
                    reportPicker.launch("pulselibre-cirqa-testbericht.txt")
                }
                .show()
        })
        root.addView(button("Zurück") { finish() })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(palette.background)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(MainNavigationView(this@CirqaTestLaboratoryActivity, 3))
        })
    }

    private fun addCard(card: LinearLayout) {
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun addRecordView(card: LinearLayout, test: CirqaTestLaboratory.TestId) {
        val text = TextView(this).apply {
            textSize = 12f
            setTextColor(AppPalette.of(this@CirqaTestLaboratoryActivity).muted)
            setPadding(0, 12, 0, 0)
            isFocusable = true
        }
        statusViews[test] = text
        card.addView(text)
    }

    private fun loadRecords() {
        for ((test, initial) in records.toMap()) {
            val verdict = runCatching {
                CirqaTestLaboratory.Verdict.valueOf(preferences.getString("${test.name}.verdict", null) ?: "")
            }.getOrDefault(initial.verdict)
            val evidence = runCatching {
                CirqaTestLaboratory.EvidenceLevel.valueOf(preferences.getString("${test.name}.evidence", null) ?: "")
            }.getOrDefault(initial.evidence)
            val checkedAt = preferences.getLong("${test.name}.time", -1L).takeIf { it >= 0 }
            val summary = preferences.getString("${test.name}.summary", null) ?: initial.safeSummary
            records[test] = initial.copy(
                verdict = verdict,
                evidence = evidence,
                checkedAtEpochMillis = checkedAt,
                safeSummary = summary
            )
        }
        // History is a release gate, never an enabled action or a user-editable pass result.
        records[CirqaTestLaboratory.TestId.HISTORICAL_DOWNLOAD] =
            CirqaTestLaboratory.initialRecords().first {
                it.test == CirqaTestLaboratory.TestId.HISTORICAL_DOWNLOAD
            }
        records[CirqaTestLaboratory.TestId.PASSIVE_PROTOCOL] =
            CirqaTestLaboratory.initialRecords().first {
                it.test == CirqaTestLaboratory.TestId.PASSIVE_PROTOCOL
            }
    }

    private fun saveRecord(
        test: CirqaTestLaboratory.TestId,
        verdict: CirqaTestLaboratory.Verdict,
        evidence: CirqaTestLaboratory.EvidenceLevel,
        safeSummary: String
    ) {
        if (test == CirqaTestLaboratory.TestId.HISTORICAL_DOWNLOAD) return
        val now = System.currentTimeMillis()
        val record = CirqaTestLaboratory.Record(test, verdict, evidence, now, safeSummary)
        records[test] = record
        preferences.edit()
            .putString("${test.name}.verdict", verdict.name)
            .putString("${test.name}.evidence", evidence.name)
            .putLong("${test.name}.time", now)
            .putString("${test.name}.summary", safeSummary.take(240))
            .apply()
        refreshRecordView(test)
    }

    private fun refreshStatusRows() {
        CirqaTestLaboratory.TestId.values().forEach(::refreshRecordView)
    }

    private fun refreshRecordView(test: CirqaTestLaboratory.TestId) {
        val record = records[test] ?: return
        val view = statusViews[test] ?: return
        val time = record.checkedAtEpochMillis?.let {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
        } ?: "–"
        view.text = "Status: ${record.verdict.label} · ${record.evidence.label}\nZeit: $time\n${record.safeSummary}"
        view.contentDescription = "${test.label}. ${view.text}"
    }

    private fun requiredPermissions(needsScan: Boolean): Array<String> = if (Build.VERSION.SDK_INT >= 31) {
        if (needsScan) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun withBluetoothPermissions(needsScan: Boolean, action: () -> Unit) {
        val required = requiredPermissions(needsScan)
        val missing = required.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) action()
        else {
            permissionAction = action
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun confirmDirectoryAfterChain() {
        if (!CirqaDirectoryAfterChainGate.RELEASED_FOR_DEVICE_TEST) return
        if (preferences.getBoolean(CirqaDirectoryAfterChainGate.PREFERENCE_KEY, false)) {
            statusOutput.text = "Kette+Verzeichnis ist bereits verbraucht."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Vollkette + Verzeichnis-Body lesen?")
            .setMessage(
                "Ein Lauf: Bootstrap → 5030 → 5007 → 5031 → 5002 (Größe) → 5004-Body " +
                    "CRC-/Offset-validiert lesen, Einträge protokollieren. Danach Stopp vor " +
                    "FIT-Download. Read-only, kein Löschen/Archiv/Flags, kein Retry. " +
                    "Einmal-Gate wird vor dem Verbinden verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    systemEventProbe = probe
                    probe.startDirectoryAfterChainTest(userConfirmed = true) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            val persisted = (preferences.getString("directoryAfterChainSummary", null).orEmpty() + "\n" + message)
                                .trim().takeLast(5000)
                            preferences.edit().putString("directoryAfterChainSummary", persisted).apply()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmSyncRequest() {
        val prefs = getSharedPreferences(CirqaSyncAttemptGate.PREFS_NAME, MODE_PRIVATE)
        if (!CirqaSyncAttemptGate.RELEASED_FOR_DEVICE_TEST ||
            !CirqaSyncAttemptGate.available(prefs)) {
            statusOutput.text = "Sync-Request nicht freigegeben oder Gate bereits verbraucht."
            return
        }
        prefs.edit().remove(CirqaSyncAttemptGate.SUMMARY_KEY).commit()
        AlertDialog.Builder(this)
            .setTitle("Garmin-Sync anfordern?")
            .setMessage(
                "Ein Connect-kompatibler 5043-sync_request nach frischem Bootstrap. " +
                    "Das Gerät kann daraufhin gepufferte Daten liefern und diese " +
                    "geräteseitig als übertragen markieren. Wir senden selbst KEINE " +
                    "Archiv-, Flag-, Lösch- oder Reset-Befehle. Nur exakte Dateien mit gültigem FIT-CRC " +
                    "werden privat gespeichert und unterstützte Messwerte importiert. 60-s-Budget, kein Retry; " +
                    "Einmal-Gate wird vor dem Verbinden verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    systemEventProbe = probe
                    probe.startSyncRequestTest(userConfirmed = true) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            prefs.edit().putString(CirqaSyncAttemptGate.SUMMARY_KEY, message).apply()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmFileAccessList() {
        val prefs = getSharedPreferences(CirqaFileAccessAttemptGate.PREFS_NAME, MODE_PRIVATE)
        if (!CirqaFileAccessAttemptGate.RELEASED_FOR_DEVICE_TEST ||
            !CirqaFileAccessAttemptGate.available(prefs)) {
            statusOutput.text = "FileAccess-Liste nicht freigegeben oder Gate bereits verbraucht."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("FileAccess-Dateiliste lesen?")
            .setMessage(
                "Ein Leselauf über den Protobuf-Kanal: genau eine 5043-Item-List-Anfrage " +
                    "nach frischem Bootstrap, CRC-validierte 5044-Antwort, danach sofortiger " +
                    "Stopp. Rein lesend – kein Archivieren, Löschen, SetFlags, Reset oder " +
                    "FIT-Download. 20-s-Budget, kein Retry. Der Einmal-Gate wird vor dem " +
                    "Verbinden verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    systemEventProbe = probe
                    probe.startFileAccessListTest(userConfirmed = true) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            prefs.edit().putString(CirqaFileAccessAttemptGate.SUMMARY_KEY, message).apply()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmRfcomm() {
        if (!CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST) {
            statusOutput.text = "RFCOMM-Pfad ist nicht freigegeben."
            return
        }
        if (!CirqaRfcommAttemptGate.available(preferences)) {
            statusOutput.text = "RFCOMM-Einmal-Gate ist bereits verbraucht."
            return
        }
        val matching = try {
            adapter.bondedDevices.orEmpty().filter {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
        } catch (_: SecurityException) { emptyList() }
        if (matching.size != 1) {
            Toast.makeText(this,
                if (matching.isEmpty()) "Kein gekoppeltes CIRQA-Gerät gefunden; kein Socket geöffnet."
                else "Mehrere gekoppelte CIRQA-Geräte; Ziel nicht eindeutig.", Toast.LENGTH_LONG).show()
            return
        }
        val address = matching.first().address
        AlertDialog.Builder(this)
            .setTitle("RFCOMM-Verzeichnis lesen?")
            .setMessage(
                "Ein begrenzter, lesender Lauf über den Garmin-SPP-Dienst " +
                    "(${CirqaRfcommSpec.SERVICE_UUID}). Der erwartete, gepaarte CIRQA darf sich " +
                    "verbinden; unbekannt Adressen werden abgelehnt. Genau ein 5002-Request, " +
                    "CRC-/Offset-validierte 5004-Chunks, danach Stopp vor FIT-Download. " +
                    "20 s Annahme, 60 s Transfer, kein Retry, keine Lösch-/Archiv-/Flag-Befehle. " +
                    "Das Einmal-Gate wird vor dem Öffnen des Sockets verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    CirqaRfcommAttemptGate.consume(preferences)
                    val runner = CirqaRfcommRunner()
                    rfcommRunner = runner
                    runner.start(this@CirqaTestLaboratoryActivity, address) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            preferences.edit().putString(
                                CirqaRfcommAttemptGate.SUMMARY_KEY, message
                            ).apply()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmDirectoryReadV2() {
        if (!CirqaDirectoryReadV2AttemptGate.RELEASED_FOR_DEVICE_TEST) return
        if (preferences.getBoolean(CirqaDirectoryReadV2AttemptGate.PREFERENCE_KEY, false)) {
            statusOutput.text = "Verzeichnis-Lesen ist bereits verbraucht."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Verzeichnis read-only lesen?")
            .setMessage(
                "Frischer Bootstrap, dann Directory-Index 0 per 5002 anfordern und die " +
                    "Dateidaten per 5004 CRC-/Offset-validiert lesen. Danach sofortiger Stopp " +
                    "vor jedem FIT-Download. Keine Lösch-, Archiv-, Flag- oder Reset-Befehle. " +
                    "Nur lesend, Einmal-Gate wird vor dem Verbinden verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Lesen und ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    systemEventProbe = probe
                    probe.startDirectoryReadV2Test(userConfirmed = true) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            val persisted = (preferences.getString("directoryReadV2Summary", null).orEmpty() + "\n" + message)
                                .trim().takeLast(4000)
                            preferences.edit().putString("directoryReadV2Summary", persisted).apply()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmHistoricalSyncV1(maxPhase: Int) {
        if (!CirqaHistoricalSyncV1MultiPhaseAttemptGate.RELEASED_FOR_DEVICE_TEST) return
        if (preferences.getBoolean(CirqaHistoricalSyncV1MultiPhaseAttemptGate.keyForPhase(maxPhase), false)) {
            statusOutput.text = "Phase $maxPhase ist bereits verbraucht."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Historische Synchronisation · Phase $maxPhase ausführen?")
            .setMessage(
                "Frischer Bootstrap, danach genau die freigegebenen Schritte bis Phase $maxPhase. " +
                    "Stopp unmittelbar nach der Phase, kein Retry, kein FIT-Download. " +
                    "Recorder und Garmin Connect müssen beendet sein. Das separate Einmal-Gate " +
                    "für Phase $maxPhase wird vor dem Verbinden dauerhaft verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    historicalSyncProbe = probe
                    probe.startHistoricalSyncV1MultiPhaseTest(userConfirmed = true, maxPhase = maxPhase) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            if (message.startsWith("HistoricalSyncV1MultiPhase") || message.startsWith("Historical Sync V1")) {
                                preferences.edit().putString("historicalSyncV1Summary", message.take(900)).apply()
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmLegacy5007Test() {
        val attempts = legacy5007ConsumedAttempts()
        if (attempts >= CirqaLegacy5007AttemptGate.MAX_AUTHORIZED_ATTEMPTS) {
            refreshLegacy5007Button()
            return
        }
        val nextAttempt = attempts + 1
        val attemptOrdinal = when (nextAttempt) {
            1 -> "ersten"
            2 -> "zweiten"
            3 -> "dritten"
            4 -> "vierten"
            else -> "fünften"
        }
        AlertDialog.Builder(this)
            .setTitle("$attemptOrdinal freigegebenen Bootstrap-/5007-Test starten?")
            .setMessage(
                "Dies ist Versuch $nextAttempt von ${CirqaLegacy5007AttemptGate.MAX_AUTHORIZED_ATTEMPTS}. " +
                    "Zuerst wird der Garmin-ML-GFDI-Dienst registriert. Danach wird der 5024/5050-Handshake ausgeführt. " +
                    "Nur wenn 5024-ACK, 5050-Hostantwort und GFDI-ACK für 5050 bestätigt sind, folgt genau ein 5007-Request:\n\n" +
                    "07 00 8F 13 03 C9 1A\n\n" +
                    "Der dynamische ML-Handle und TX-Characteristic werden zur Laufzeit ermittelt. Die 5050-Hostmaske enthält 15 Nullbytes. " +
                    "Nach dem 5007-Write höchstens fünf Sekunden auf eine einzelne CRC-geprüfte GFDI-5000-Antwort warten. " +
                    "Der Filter kann geräteseitige Pending-Sync-Aktivität auslösen; Trennen verhindert unbekannte Bandwirkungen nicht. " +
                    "Bei unvollständigem/negativem Bootstrap wird ohne 5007 passiv abgebrochen. Garmin Connect und Recorder müssen beendet sein. " +
                    "Kein Retry und keine Datei-/Folgekommandos."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Bootstrap; danach max. ein 5007") { _, _ ->
                withBluetoothPermissions(needsScan = false) { runLegacy5007TestOnce() }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runLegacy5007TestOnce() {
        val consumed = legacy5007ConsumedAttempts()
        val nextAttempt = CirqaLegacy5007AttemptGate.nextAttempt(
            preferences.getInt("legacy5007TestAttemptCount", consumed),
            preferences.getBoolean("legacy5007TestAttempted", false)
        ) ?: run {
            refreshLegacy5007Button()
            return
        }
        val matchingDevices = try {
            adapter.bondedDevices.orEmpty().filter { it.name?.contains("CIRQA", ignoreCase = true) == true }
        } catch (_: SecurityException) {
            emptyList()
        }
        if (matchingDevices.size != 1) {
            val message = if (matchingDevices.isEmpty())
                "Kein gekoppeltes CIRQA-Gerät gefunden; kein Request gesendet."
            else "Mehrere gekoppelte CIRQA-Geräte gefunden; Ziel nicht eindeutig, kein Request gesendet."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }
        if (CirqaBleSessionGate.isBusy()) {
            Toast.makeText(this, "CIRQA-Bluetooth ist durch eine laufende Sitzung belegt; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        // Persist the authorized attempt before GATT so process restart cannot retry it.
        if (!preferences.edit()
                .putBoolean("legacy5007TestAttempted", true)
                .putInt("legacy5007TestAttemptCount", nextAttempt)
                .commit()) {
            Toast.makeText(this, "Einmal-Gate konnte nicht dauerhaft gespeichert werden; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        refreshLegacy5007Button()
        statusOutput.text = "Bootstrap-Test läuft; 5007 nur nach bestätigtem 5024/5050-Handshake."
        if (mlProbe == null) mlProbe = CirqaMlHandshakeProbe(this, adapter)
        mlProbe?.startLegacy5007AfterBootstrapTest { message ->
            runOnUiThread {
                statusOutput.text = message
                val supported = message.contains("Status 0 (Connect-Vertrag")
                val notSupported = message.contains("negativer Status")
                val hasVerifiedResponse = message.contains("Antwort: 5000;")
                val verdict = when {
                    supported -> CirqaTestLaboratory.Verdict.PASSED
                    notSupported -> CirqaTestLaboratory.Verdict.NOT_SUPPORTED
                    else -> CirqaTestLaboratory.Verdict.FAILED
                }
                val evidence = if (hasVerifiedResponse)
                    CirqaTestLaboratory.EvidenceLevel.DEVICE_CONFIRMED
                else CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED
                val safeSummary = when {
                    supported -> "Eine CRC-validierte GFDI-5000-Antwort zu 5007 meldete Status 0; nur Kompatibilität dieses Filters beobachtet, kein Historienabruf."
                    notSupported -> "Eine CRC-validierte GFDI-5000-Antwort zu 5007 meldete einen negativen Status; keine Folgeanfrage gesendet."
                    message.contains("5007-Request gesendet: false") -> {
                        val reason = message.lineSequence()
                            .firstOrNull { it.startsWith("Ergebnis: ") }
                            ?.removePrefix("Ergebnis: ")
                            ?.take(160)
                            ?: "Verbindung oder Protokoll vor dem Filter-Write abgebrochen"
                        "Vor dem 5007-Write abgebrochen ($reason); kein GFDI-5007-Write gesendet. " +
                            "Dieser ausdrücklich freigegebene Testlauf ist verbraucht."
                    }
                    message.contains("Fünf-Sekunden") -> "Einmaliger 5007-Request gesendet; innerhalb von fünf Sekunden keine passende Antwort beobachtet."
                    message.contains("nicht gesendet") -> "Test vor dem Request abgebrochen; kein 5007-Request gesendet."
                    else -> "Einmaliger Test abgebrochen; keine Folgeanfrage oder Wiederholung."
                }
                preferences.edit().putString("legacy5007TestSummary", safeSummary).apply()
                saveRecord(
                    CirqaTestLaboratory.TestId.LEGACY_5007_COMPATIBILITY,
                    verdict,
                    evidence,
                    safeSummary
                )
            }
        }
    }

    private fun legacy5007ConsumedAttempts(): Int =
        CirqaLegacy5007AttemptGate.consumedAttempts(
            preferences.getInt("legacy5007TestAttemptCount", 0),
            preferences.getBoolean("legacy5007TestAttempted", false)
        )

    private fun refreshLegacy5007Button() {
        val consumed = legacy5007ConsumedAttempts()
        val remaining = CirqaLegacy5007AttemptGate.MAX_AUTHORIZED_ATTEMPTS - consumed
        legacy5007Button.isEnabled = remaining > 0
        legacy5007Button.text = when {
            remaining <= 0 -> "Freigegebene 5007-Tests verbraucht"
            consumed == 1 -> "Freigegebenen zweiten 5007-Test starten"
            consumed == 2 -> "Freigegebenen dritten 5007-Test starten"
            consumed == 3 -> "Freigegebenen vierten 5007-Test starten"
            consumed == 4 -> "Freigegebenen fünften Bootstrap→5007-Test starten"
            else -> "Einmaligen Bootstrap→5007-Test starten"
        }
        legacy5007Button.contentDescription =
            "Noch $remaining freigegebener Bootstrap-/5007-Testversuch; 5007 nur nach bestätigtem 5024/5050-Handshake."
    }

    private fun confirmLegacy5030OnlyTest() {
        if (!CirqaLegacy5030To5007AttemptGate.RELEASED_FOR_DEVICE_TEST) return
        AlertDialog.Builder(this)
            .setTitle("Einmaliger 5030-only-Test?")
            .setMessage(
                "Recorder und Garmin Connect müssen beendet sein. Genau ein frischer " +
                    "5024/5050-Bootstrap mit unveränderter Nullmaske, danach höchstens ein " +
                    "5030 HANDSHAKE_COMPLETE. Stopp nach einer Antwort, Fehler, Trennung oder " +
                    "5 Sekunden ohne vollständige Antwort/Write; insgesamt maximal 30 Sekunden. " +
                    "Kein Retry, 5007/5031/5002, CLOSE_ALL, Flag-/Archiv-/Löschkommando. " +
                    "Das separate Einmal-Gate wird vor Verbindung dauerhaft verbraucht. " +
                    "Unbekannte Systemevent-Nebenwirkungen sind nicht ausgeschlossen."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Einmal ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    val probe = CirqaMlHandshakeProbe(this, adapter)
                    systemEventProbe = probe
                    probe.startLegacy5030OnlyTest(userConfirmed = true) { message ->
                        runOnUiThread {
                            statusOutput.text = message
                            if (message.startsWith("5030-only: phase=")) {
                                getSharedPreferences("cirqa_5030_only", MODE_PRIVATE).edit()
                                    .putString("summary", message.take(600)).apply()
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmLegacy5031Test() {
        val consumed = preferences.getBoolean("legacy5031TestAttempted", false)
        if (!CirqaLegacy5031AttemptGate.canAttempt(consumed)) {
            refreshLegacy5031Button()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Einmaligen Bootstrap-/5007-/5031-Test starten?")
            .setMessage(
                "Der separate Einmal-Gate wird vor dem Verbindungsaufbau dauerhaft verbraucht. " +
                    "Zuerst bestätigter 5024/5050-Bootstrap, dann höchstens ein 5007. Nur nach CRC-validem Status 0 folgt genau ein leerer 5031-Request. " +
                    "Auf höchstens eine CRC-geprüfte Antwort wird maximal fünf Sekunden gewartet. Bei negativer, fehlender, unerwarteter oder ungültiger Antwort wird abgebrochen. " +
                    "Nach 5031 wird immer vor 5002 gestoppt; kein Retry oder weiteres Kommando. " +
                    "Garmin Connect und Recorder müssen beendet sein. Gerätereaktionen nach Trennung sind unbekannt."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Einmaligen Test ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) { runLegacy5031TestOnce() }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runLegacy5031TestOnce() {
        if (!CirqaLegacy5031AttemptGate.canAttempt(
                preferences.getBoolean("legacy5031TestAttempted", false)
            )) {
            refreshLegacy5031Button()
            return
        }
        val matchingDevices = try {
            adapter.bondedDevices.orEmpty().filter { it.name?.contains("CIRQA", ignoreCase = true) == true }
        } catch (_: SecurityException) { emptyList() }
        if (matchingDevices.size != 1) {
            Toast.makeText(
                this,
                if (matchingDevices.isEmpty()) "Kein gekoppeltes CIRQA-Gerät gefunden; kein Request gesendet."
                else "Mehrere gekoppelte CIRQA-Geräte gefunden; Ziel nicht eindeutig, kein Request gesendet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (CirqaBleSessionGate.isBusy()) {
            Toast.makeText(this, "CIRQA-Bluetooth ist durch eine laufende Sitzung belegt; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        if (!preferences.edit().putBoolean("legacy5031TestAttempted", true).commit()) {
            Toast.makeText(this, "5031-Einmal-Gate konnte nicht dauerhaft gespeichert werden; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        refreshLegacy5031Button()
        statusOutput.text = "5031-Test läuft; Bootstrap und bestätigter 5007 sind Voraussetzungen. Kein 5002."
        if (mlProbe == null) mlProbe = CirqaMlHandshakeProbe(this, adapter)
        mlProbe?.startLegacy5031AfterBootstrapTest { message ->
            runOnUiThread {
                statusOutput.text = message
                val has5031Response = message.contains("5031-Antwort CRC-validiert")
                val passed = message.contains("Status 0; Dateitypen=")
                val negative = message.contains("5031-Antwort CRC-validiert; negativer Status")
                val safeSummary = when {
                    passed -> "5031 wurde nach bestätigtem Bootstrap und 5007 Status 0 beantwortet; Dateitypen klassifiziert. Vor 5002 gestoppt."
                    negative -> "5031-Antwort meldete negativen Status; vor 5002 gestoppt."
                    has5031Response -> "5031-Antwort war unerwartet oder strukturell ungültig; vor 5002 gestoppt."
                    message.contains("5031-Request gesendet: true") -> "Einmaliger 5031-Request gesendet; keine verwertbare passende Antwort. Vor 5002 gestoppt."
                    else -> "Einmaliger Probe vor 5031 abgebrochen; keine Folgeanfrage oder Wiederholung."
                }
                val reportSummary = message.lineSequence()
                    .firstOrNull { it.startsWith("5031-Request gesendet:") }
                    ?.take(700)
                    ?.let { "$safeSummary $it" }
                    ?: safeSummary
                preferences.edit().putString("legacy5031TestSummary", reportSummary).apply()
                statusOutput.text = "$message\nEinmal-Gate verbraucht; 5002 wurde nicht gesendet."
            }
        }
    }

    private fun refreshLegacy5031Button() {
        val consumed = preferences.getBoolean("legacy5031TestAttempted", false)
        legacy5031Button.isEnabled = CirqaLegacy5031AttemptGate.canAttempt(consumed)
        legacy5031Button.text = if (consumed) "Freigegebener 5031-Test verbraucht"
        else "Einmaligen Bootstrap→5007→5031-Test starten"
    }

    private fun confirmLegacyHistoryDownload() {
        if (!CirqaLegacyHistoryDownloadGate.canAttempt(
                preferences.getBoolean("legacyHistoryDownloadAttempted", false)
            )) {
            refreshLegacyHistoryDownloadButton()
            return
        }
        val cachedSummary = preferences.getString("legacy5031TestSummary", null)
        val supportedTypes = CirqaLegacySupportedTypesCache.parse(cachedSummary)
        if (supportedTypes.isEmpty()) {
            statusOutput.text = "Kein erfolgreicher gespeicherter 5031-Befund; kein Gerät kontaktiert."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Begrenzten Directory-/FIT-Transfer einmalig starten?")
            .setMessage(
                "Ein frischer Bootstrap wird ausgeführt. Der geschlossene 5007-Gate wird nicht wiederverwendet: Es werden weder 5007 noch 5031 erneut gesendet. " +
                    "Danach wird Directory-Index 0 gelesen und höchstens vier lesbare FIT-Dateien aus den zuvor bestätigten 5031-Typen angefordert. " +
                    "Unkomprimiert; Directory höchstens 1 MiB, jede FIT-Datei höchstens 4 MiB, gesamte FIT-Daten höchstens 16 MiB, Laufzeit höchstens 5 Minuten. " +
                    "Notwendige 5004-ACKs nur nach CRC-/Offset-Prüfung; jeder Fehler stoppt ohne Retry. " +
                    "Vollständig FIT-validierte Dateien werden in PulsLibres privatem App-Speicher abgelegt, nicht in SQLite. " +
                    "Kein SetFlags/Archivieren/Löschen/CLOSE_ALL. Der Einmal-Gate wird vor Verbindungsaufbau verbraucht; Teilabbrüche werden nicht automatisch fortgesetzt."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Einmaligen Transfer ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) { runLegacyHistoryDownloadOnce(supportedTypes) }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runLegacyHistoryDownloadOnce(supportedTypes: Set<Pair<Int, Int>>) {
        if (!CirqaLegacyHistoryDownloadGate.canAttempt(
                preferences.getBoolean("legacyHistoryDownloadAttempted", false)
            )) {
            refreshLegacyHistoryDownloadButton()
            return
        }
        val matchingDevices = try {
            adapter.bondedDevices.orEmpty().filter { it.name?.contains("CIRQA", ignoreCase = true) == true }
        } catch (_: SecurityException) { emptyList() }
        if (matchingDevices.size != 1) {
            Toast.makeText(
                this,
                if (matchingDevices.isEmpty()) "Kein gekoppeltes CIRQA-Gerät gefunden; kein Request gesendet."
                else "Mehrere gekoppelte CIRQA-Geräte gefunden; Ziel nicht eindeutig, kein Request gesendet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (CirqaBleSessionGate.isBusy()) {
            Toast.makeText(this, "CIRQA-Bluetooth ist belegt; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        if (!preferences.edit().putBoolean("legacyHistoryDownloadAttempted", true).commit()) {
            Toast.makeText(this, "Einmal-Gate konnte nicht dauerhaft gespeichert werden; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        refreshLegacyHistoryDownloadButton()
        refreshLegacyHistoryFollowUpButton()
        statusOutput.text = "Begrenzter Directory-/FIT-Transfer läuft; maximal 5 Minuten, keine Wiederaufnahme."
        if (mlProbe == null) mlProbe = CirqaMlHandshakeProbe(this, adapter)
        mlProbe?.startLegacyDirectoryAndFitsTest(supportedTypes) { message ->
            runOnUiThread {
                statusOutput.text = message
                val summary = message.lineSequence()
                    .filter { it.startsWith("Ergebnis:") || it.startsWith("5002-Indizes") ||
                        it.startsWith("Directory-Einträge=") || it.startsWith("Vollständig validierte") ||
                        it.startsWith("Grenzen:") }
                    .joinToString(" ")
                    .take(900)
                    .ifEmpty { message.take(900) }
                preferences.edit().putString("legacyHistoryDownloadSummary", summary).apply()
            }
        }
    }

    private fun refreshLegacyHistoryDownloadButton() {
        val consumed = preferences.getBoolean("legacyHistoryDownloadAttempted", false)
        legacyHistoryDownloadButton.isEnabled = CirqaLegacyHistoryDownloadGate.canAttempt(consumed)
        legacyHistoryDownloadButton.text = if (consumed)
            "Freigegebener Directory-/FIT-Transfer verbraucht"
        else "Einmaligen Directory-/FIT-Transfer starten"
    }

    private fun confirmLegacyHistoryMetadataFollowUp() {
        val firstAttemptConsumed =
            preferences.getBoolean("legacyHistoryDownloadAttempted", false)
        if (!firstAttemptConsumed ||
            !CirqaLegacyHistoryDownloadGate.canRunMetadataFollowUp(
                preferences.getBoolean("legacyHistoryMetadataFollowUpAttempted", false)
            )) {
            refreshLegacyHistoryFollowUpButton()
            return
        }
        val supportedTypes = CirqaLegacySupportedTypesCache.parse(
            preferences.getString("legacy5031TestSummary", null)
        )
        if (supportedTypes.isEmpty()) {
            statusOutput.text = "Kein erfolgreicher gespeicherter 5031-Befund; kein Gerät kontaktiert."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Einmaligen Metadaten-Nachlauf starten?")
            .setMessage(
                "Ein zweiter und letzter, ausdrücklich freigegebener Lauf liest Directory-Index 0 erneut und protokolliert begrenzt nur Dateiindex, Typ/Subtyp, READ-Flag, Größe und Auswahlgrund. " +
                    "Zeitstempel, Identifierbytes und Rohdaten werden nicht protokolliert. Falls lesbare, unterstützte FIT-Einträge vorhanden sind, dürfen im selben Lauf höchstens vier Dateien (je 4 MiB, gesamt 16 MiB) mit FIT-CRC-Prüfung lokal gespeichert werden. " +
                    "Directory max. 1 MiB, Gesamtlaufzeit max. 5 Minuten; frischer Bootstrap, aber keine Wiederholung von 5007/5031. Kein Retry, Archivieren, Löschen, SetFlags oder CLOSE_ALL. " +
                    "Der gesonderte Follow-up-Gate wird vor dem Verbindungsaufbau verbraucht."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Einmaligen Nachlauf ausführen") { _, _ ->
                withBluetoothPermissions(needsScan = false) {
                    runLegacyHistoryMetadataFollowUpOnce(supportedTypes)
                }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runLegacyHistoryMetadataFollowUpOnce(supportedTypes: Set<Pair<Int, Int>>) {
        if (!preferences.getBoolean("legacyHistoryDownloadAttempted", false) ||
            !CirqaLegacyHistoryDownloadGate.canRunMetadataFollowUp(
                preferences.getBoolean("legacyHistoryMetadataFollowUpAttempted", false)
            )) {
            refreshLegacyHistoryFollowUpButton()
            return
        }
        val matchingDevices = try {
            adapter.bondedDevices.orEmpty().filter {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
        } catch (_: SecurityException) {
            emptyList()
        }
        if (matchingDevices.size != 1) {
            Toast.makeText(
                this,
                if (matchingDevices.isEmpty()) "Kein gekoppeltes CIRQA-Gerät gefunden; kein Request gesendet."
                else "Mehrere gekoppelte CIRQA-Geräte gefunden; Ziel nicht eindeutig, kein Request gesendet.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (CirqaBleSessionGate.isBusy()) {
            Toast.makeText(this, "CIRQA-Bluetooth ist belegt; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        if (!preferences.edit()
                .putBoolean("legacyHistoryMetadataFollowUpAttempted", true)
                .commit()) {
            Toast.makeText(this, "Follow-up-Gate konnte nicht dauerhaft gespeichert werden; kein Request gesendet.", Toast.LENGTH_LONG).show()
            return
        }
        refreshLegacyHistoryFollowUpButton()
        statusOutput.text = "Einmaliger Metadaten-Nachlauf läuft; dieselben Größen-/Zeitlimits gelten."
        if (mlProbe == null) mlProbe = CirqaMlHandshakeProbe(this, adapter)
        mlProbe?.startLegacyDirectoryAndFitsTest(supportedTypes) { message ->
            runOnUiThread {
                statusOutput.text = message
                val persisted = message.lineSequence()
                    .filter {
                        it.startsWith("Ergebnis:") ||
                            it.startsWith("5002-Indizes") ||
                            it.startsWith("Directory-Einträge=") ||
                            it.startsWith("Directory-Eintrag-Metadaten") ||
                            it.startsWith("Vollständig validierte") ||
                            it.startsWith("Grenzen:")
                    }
                    .joinToString(" ")
                    .take(3200)
                    .ifEmpty { message.take(3200) }
                preferences.edit()
                    .putString("legacyHistoryMetadataFollowUpSummary", persisted)
                    .apply()
            }
        }
    }

    private fun refreshLegacyHistoryFollowUpButton() {
        val firstConsumed = preferences.getBoolean("legacyHistoryDownloadAttempted", false)
        val followUpConsumed =
            preferences.getBoolean("legacyHistoryMetadataFollowUpAttempted", false)
        legacyHistoryFollowUpButton.isEnabled =
            firstConsumed && CirqaLegacyHistoryDownloadGate.canRunMetadataFollowUp(followUpConsumed)
        legacyHistoryFollowUpButton.text = when {
            followUpConsumed -> "Metadaten-Nachlauf verbraucht"
            firstConsumed -> "Einmaligen Directory-/FIT-Nachlauf starten"
            else -> "Nachlauf erst nach dem ersten Transfer verfügbar"
        }
    }

    @SuppressLint("MissingPermission")
    private fun findBondedCirqa() = try {
        adapter.bondedDevices.orEmpty().firstOrNull {
            it.name?.contains("CIRQA", ignoreCase = true) == true
        }
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("MissingPermission")
    private fun captureAndroidStatus() {
        val device = findBondedCirqa()
        if (device == null) {
            statusOutput.text = "Kein gekoppeltes Gerät mit CIRQA-Bezeichnung gefunden. Es wurde keine Verbindung aufgebaut."
            saveRecord(
                CirqaTestLaboratory.TestId.ANDROID_STATUS,
                CirqaTestLaboratory.Verdict.PASSED,
                CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED,
                "Abfrage abgeschlossen; kein gekoppeltes CIRQA-Gerät gefunden."
            )
            return
        }
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        val state = runCatching {
            (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
                .getConnectionState(device, BluetoothProfile.GATT)
        }.getOrDefault(BluetoothProfile.STATE_DISCONNECTED)
        val label = when (state) {
            BluetoothProfile.STATE_CONNECTED -> "GATT verbunden"
            BluetoothProfile.STATE_CONNECTING -> "GATT verbindet"
            BluetoothProfile.STATE_DISCONNECTING -> "GATT trennt"
            else -> "GATT nicht verbunden"
        }
        statusOutput.text = "Bluetooth: ${if (enabled) "an" else "aus"} · $label\n" +
            "Android-Systemstatus; die verantwortliche App ist unbekannt. Keine Verbindung gestartet."
        saveRecord(
            CirqaTestLaboratory.TestId.ANDROID_STATUS,
            CirqaTestLaboratory.Verdict.PASSED,
            CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED,
            "Android-Systemstatus abgefragt; Bluetooth ${if (enabled) "an" else "aus"}; $label."
        )
    }

    private fun startConnectionMonitor() {
        if (monitorRunning) {
            Toast.makeText(this, "Verbindungsbeobachtung läuft bereits.", Toast.LENGTH_SHORT).show()
            return
        }
        if (findBondedCirqa() == null) {
            statusOutput.text = "Kein gekoppeltes CIRQA-Gerät für den Android-Statusmonitor gefunden."
            saveRecord(
                CirqaTestLaboratory.TestId.ANDROID_STATUS,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "Statusmonitor nicht gestartet; CIRQA-Kopplung fehlt."
            )
            return
        }
        monitorRunning = true
        monitorStartedAt = System.currentTimeMillis()
        connectedMillis = 0
        connectedSince = null
        connectionTransitions = 0
        lastConnectionState = null
        statusOutput.text = "Beobachte Android-GATT-Status für 30 Sekunden; keine Verbindung wird gestartet."
        sampleConnectionState()
    }

    @SuppressLint("MissingPermission")
    private fun sampleConnectionState() {
        if (!monitorRunning || destroyed) return
        val device = findBondedCirqa()
        val state = if (device == null) CirqaConnectionHistory.State.NOT_PAIRED else try {
            when ((getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
                .getConnectionState(device, BluetoothProfile.GATT)) {
                BluetoothProfile.STATE_CONNECTED -> CirqaConnectionHistory.State.CONNECTED
                BluetoothProfile.STATE_CONNECTING -> CirqaConnectionHistory.State.CONNECTING
                BluetoothProfile.STATE_DISCONNECTING -> CirqaConnectionHistory.State.DISCONNECTING
                else -> CirqaConnectionHistory.State.DISCONNECTED
            }
        } catch (_: SecurityException) {
            CirqaConnectionHistory.State.PERMISSION_DENIED
        } catch (_: IllegalArgumentException) {
            CirqaConnectionHistory.State.UNKNOWN
        }
        val now = System.currentTimeMillis()
        if (state != lastConnectionState) {
            connectionTransitions++
            lastConnectionState = state
        }
        if (state == CirqaConnectionHistory.State.CONNECTED) {
            if (connectedSince == null) connectedSince = now
        } else {
            connectedSince?.let { connectedMillis += now - it }
            connectedSince = null
        }
        if (now - monitorStartedAt >= 30_000L) {
            connectedSince?.let { connectedMillis += now - it }
            connectedSince = null
            monitorRunning = false
            val seconds = connectedMillis / 1_000L
            statusOutput.text = "Beobachtung abgeschlossen: Statuswechsel=$connectionTransitions · " +
                "Android meldete GATT verbunden für ${seconds}s der 30s.\nVerbundene App unbekannt."
            saveRecord(
                CirqaTestLaboratory.TestId.ANDROID_STATUS,
                CirqaTestLaboratory.Verdict.PASSED,
                CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED,
                "30-s-Statusbeobachtung abgeschlossen; Statuswechsel=$connectionTransitions; verbunden=${seconds}s."
            )
            return
        }
        handler.postDelayed(connectionPoll, 1_000L)
    }

    @SuppressLint("MissingPermission")
    private fun startPassiveScan() {
        if (scanning) return
        if (!runCatching { adapter.isEnabled }.getOrDefault(false)) {
            scanOutput.text = "Bluetooth ist aus. Bitte selbst in den Android-Einstellungen aktivieren; kein Test gestartet."
            saveRecord(
                CirqaTestLaboratory.TestId.PASSIVE_SCAN,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "Scan nicht gestartet; Bluetooth ausgeschaltet."
            )
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            scanOutput.text = "Android-BLE-Scanner nicht verfügbar."
            saveRecord(
                CirqaTestLaboratory.TestId.PASSIVE_SCAN,
                CirqaTestLaboratory.Verdict.NOT_SUPPORTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "Android-BLE-Scanner nicht verfügbar."
            )
            return
        }
        scanEvents = 0
        matchedBondAdvertisement = false
        garminHintEvents = 0
        rssiSamples.clear()
        val bondedAddress = findBondedCirqa()?.address
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!scanning || destroyed) return
                scanEvents++
                if (rssiSamples.size < 500) rssiSamples += result.rssi
                if (bondedAddress != null &&
                    CirqaAddressMatch.matches(bondedAddress, result.device.address)) {
                    matchedBondAdvertisement = true
                }
                val record = result.scanRecord
                val companyIds = (0 until (record?.manufacturerSpecificData?.size() ?: 0))
                    .mapNotNull { record?.manufacturerSpecificData?.keyAt(it) }
                val serviceIds = record?.serviceUuids.orEmpty().map { it.uuid.toString() }
                val name = record?.deviceName
                if (BleAdvertisementIdentity.isGarminHint(name, companyIds, serviceIds)) garminHintEvents++
                scanOutput.text = "Passiver Scan läuft · Ereignisse=$scanEvents · " +
                    "Garmin-Hinweise=$garminHintEvents · Adressabgleich=${if (matchedBondAdvertisement) "ja" else "nein"}"
            }

            override fun onScanFailed(errorCode: Int) {
                runOnUiThread {
                    scanning = false
                    scanCallback = null
                    scanOutput.text = "Android-Scan fehlgeschlagen (Code $errorCode); keine Gerätekennung gespeichert."
                    saveRecord(
                        CirqaTestLaboratory.TestId.PASSIVE_SCAN,
                        CirqaTestLaboratory.Verdict.FAILED,
                        CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                        "Android-Scan fehlgeschlagen; keine Gerätekennung gespeichert."
                    )
                }
            }
        }
        try {
            scanner.startScan(callback)
            scanCallback = callback
            scanning = true
            scanOutput.text = "Passiver BLE-Scan gestartet; keine Verbindung oder GATT-Aktion."
            handler.postDelayed(scanTimeout, 15_000L)
        } catch (_: SecurityException) {
            scanOutput.text = "Bluetooth-Berechtigung fehlt; Scan nicht gestartet."
        } catch (_: IllegalStateException) {
            scanOutput.text = "Android-Scanner nicht bereit; Scan nicht gestartet."
        }
    }

    @SuppressLint("MissingPermission")
    private fun finishPassiveScan() {
        if (!scanning) return
        val callback = scanCallback
        try { if (callback != null) adapter.bluetoothLeScanner?.stopScan(callback) }
        catch (_: SecurityException) { }
        catch (_: IllegalStateException) { }
        scanning = false
        scanCallback = null
        val median = rssiSamples.sorted().let { values ->
            if (values.isEmpty()) null else values[values.size / 2]
        }
        val summary = "Scan abgeschlossen; Werbeereignisse=$scanEvents; Garmin-Hinweise=$garminHintEvents; " +
            "Adressabgleich=${if (matchedBondAdvertisement) "ja" else "nein"}; " +
            "RSSI-Messpunkte=${rssiSamples.size}" + (median?.let { "; Median=${it}dBm" } ?: "")
        scanOutput.text = summary + "\nGarmin-/Adresshinweise sind kein Identitätsnachweis."
        val observed = scanEvents > 0
        saveRecord(
            CirqaTestLaboratory.TestId.PASSIVE_SCAN,
            CirqaTestLaboratory.Verdict.PASSED,
            if (observed) CirqaTestLaboratory.EvidenceLevel.PASSIVELY_OBSERVED
            else CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
            summary.replace(Regex("; Median=-?\\d+dBm"), "")
        )
    }

    private fun stopPassiveScan(userStopped: Boolean) {
        if (!scanning) return
        if (userStopped) handler.removeCallbacks(scanTimeout)
        finishPassiveScan()
        if (userStopped) scanOutput.append("\nVom Nutzer vor Ablauf gestoppt.")
    }

    private fun confirmGattDiscovery() {
        AlertDialog.Builder(this)
            .setTitle("Read-only GATT-Dienstsuche")
            .setMessage(
                "Es wird eine temporäre Verbindung zum gekoppelten Gerät mit CIRQA-Bezeichnung aufgebaut. " +
                    "Die vorhandene CirqaGattProbe sucht nur Dienst-/Characteristic-Metadaten, führt keine GATT-Lese-/Schreibbefehle aus, " +
                    "abonniert keine Notifications und trennt nach Abschluss oder spätestens 15 Sekunden. Garmin Connect kann die Verbindung verlieren. Fortfahren?"
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Dienstsuche starten") { _, _ -> runGattDiscovery() }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runGattDiscovery() {
        if (findBondedCirqa() == null) {
            discoveryOutput.text = "Kein gekoppeltes Gerät mit CIRQA-Bezeichnung; keine Verbindung aufgebaut."
            saveRecord(
                CirqaTestLaboratory.TestId.GATT_DISCOVERY,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "Dienstsuche nicht gestartet; CIRQA-Kopplung fehlt."
            )
            return
        }
        discoveryOutput.text = "Read-only GATT-Dienstsuche läuft …"
        if (gattProbe == null) gattProbe = CirqaGattProbe(this, adapter)
        gattProbe?.start { message ->
            runOnUiThread {
                val complete = message.startsWith("GATT-Dienstsuche abgeschlossen")
                discoveryOutput.text = message
                saveRecord(
                    CirqaTestLaboratory.TestId.GATT_DISCOVERY,
                    if (complete) CirqaTestLaboratory.Verdict.PASSED else CirqaTestLaboratory.Verdict.FAILED,
                    if (complete) CirqaTestLaboratory.EvidenceLevel.DEVICE_CONFIRMED
                    else CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                    if (complete) "Read-only Dienst-/Characteristic-Metadaten erfolgreich ermittelt."
                    else "Read-only GATT-Dienstsuche nicht erfolgreich; keine Rohdaten im Testbericht."
                )
            }
        }
    }

    private fun confirmLiveProbe() {
        AlertDialog.Builder(this)
            .setTitle("Vorhandene Live-Messwertprobe")
            .setMessage(
                "Startet ausschließlich die bereits implementierte Puls-/Schritte-Echtzeitprobe. " +
                    "Sie verbindet sich, aktiviert Notifications und sendet die dafür vorhandenen Garmin-Service-Registrierungen. " +
                    "Das kann Garmin Connect unterbrechen. Empfangene echte Werte werden wie bisher lokal gespeichert. " +
                    "Kein historischer Download, keine Dateiliste, kein Archivieren/Löschen/Flagändern und kein 0x8A39. " +
                    "Nur starten, wenn du diesen bestehenden Live-Test jetzt bewusst ausführen willst."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Live-Test starten") { _, _ -> runExistingLiveProbe() }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runExistingLiveProbe() {
        if (findBondedCirqa() == null) {
            liveOutput.text = "Kein gekoppeltes Gerät mit CIRQA-Bezeichnung; keine Live-Probe gestartet."
            saveRecord(
                CirqaTestLaboratory.TestId.LIVE_READINGS,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "Live-Probe nicht gestartet; CIRQA-Kopplung fehlt."
            )
            return
        }
        liveReadingCount = 0
        liveKinds.clear()
        liveOutput.text = "Vorhandene Puls-/Schritte-Probe läuft; höchstens 20 Sekunden."
        if (mlProbe == null) mlProbe = CirqaMlHandshakeProbe(this, adapter)
        mlProbe?.onReading = { service, reading ->
            liveReadingCount++
            liveKinds += when (reading) {
                is GarminRealtime.Reading.HeartRate -> "Puls"
                is GarminRealtime.Reading.Steps -> "Schritte"
                else -> "anderer"
            }
            CirqaLocalLog(this).reading(service, reading)
        }
        mlProbe?.startRealtime { message ->
            runOnUiThread {
                liveOutput.text = message
                if (message.startsWith("CIRQA Echtzeit-Test:")) {
                    val received = liveReadingCount
                    val types = liveKinds.sorted().joinToString(", ").ifEmpty { "keine" }
                    saveRecord(
                        CirqaTestLaboratory.TestId.LIVE_READINGS,
                        if (received > 0) CirqaTestLaboratory.Verdict.PASSED
                        else CirqaTestLaboratory.Verdict.FAILED,
                        if (received > 0) CirqaTestLaboratory.EvidenceLevel.DEVICE_CONFIRMED
                        else CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                        "Empfangene Messpakete=$received; Messwerttypen=$types; lokal gespeichert."
                    )
                    refreshStoredReadings()
                } else if (!message.startsWith("ML-Test: Verbinde")) {
                    saveRecord(
                        CirqaTestLaboratory.TestId.LIVE_READINGS,
                        CirqaTestLaboratory.Verdict.FAILED,
                        CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                        "Vorhandene Live-Probe konnte nicht vollständig gestartet oder abgeschlossen werden."
                    )
                }
            }
        }
    }

    private fun refreshStoredReadings() {
        try {
            val (count, bytes) = CirqaDatabase(this).use { it.eventCount() to it.eventBytes() }
            storedReadingsOutput.text =
                "Lokal gespeicherte Protokollereignisse: $count · Protokollgröße: $bytes Bytes.\n" +
                    "Diese Daten können aus früheren aktiven Live-Proben stammen. Das ist kein passiver Frame-Mitschnitt und kein FIT-Import."
        } catch (_: Exception) {
            storedReadingsOutput.text = "Lokale Statistik momentan nicht lesbar."
        }
    }

    private fun inspectFit(uri: android.net.Uri) {
        fitOutput.text = "FIT-Datei wird lokal geprüft …"
        io.execute {
            val result = try {
                val bytes = contentResolver.openInputStream(uri)?.use {
                    BoundedFitInput.read(it)
                }
                when {
                    bytes == null -> FitCheck.TooLarge
                    FitFileHeader.inspectVerified(bytes) == null -> FitCheck.Invalid(bytes.size)
                    else -> FitHistoricalObservations.decode(bytes)?.let { FitCheck.Valid(bytes.size, it) }
                        ?: FitCheck.Invalid(bytes.size)
                }
            } catch (_: Exception) {
                FitCheck.Invalid(null)
            }
            runOnUiThread {
                if (destroyed) return@runOnUiThread
                when (result) {
                    FitCheck.TooLarge -> {
                        fitOutput.text = "Abgelehnt: FIT-Datei überschreitet 8 MiB. Keine Datei wurde gespeichert."
                        saveRecord(
                            CirqaTestLaboratory.TestId.FIT_FILE,
                            CirqaTestLaboratory.Verdict.FAILED,
                            CirqaTestLaboratory.EvidenceLevel.OFFLINE_VALIDATED,
                            "Datei wegen Größenlimit abgelehnt; keine Kopie gespeichert."
                        )
                    }
                    is FitCheck.Invalid -> {
                        fitOutput.text = "FIT-Datei ungültig, beschädigt oder nicht unterstützte Struktur. " +
                            "Header-/Datei-CRC oder Parserprüfung fehlgeschlagen; keine Daten importiert."
                        saveRecord(
                            CirqaTestLaboratory.TestId.FIT_FILE,
                            CirqaTestLaboratory.Verdict.FAILED,
                            CirqaTestLaboratory.EvidenceLevel.OFFLINE_VALIDATED,
                            "Offline-Prüfung abgelehnt; CRC/Struktur ungültig oder nicht unterstützt."
                        )
                    }
                    is FitCheck.Valid -> renderFitResult(result.bytes, result.result)
                }
            }
        }
    }

    private sealed class FitCheck {
        data object TooLarge : FitCheck()
        data class Invalid(val bytes: Int?) : FitCheck()
        data class Valid(val bytes: Int, val result: FitHistoricalObservations.Result) : FitCheck()
    }

    private fun renderFitResult(bytes: Int, result: FitHistoricalObservations.Result) {
        val counts = FitHistoricalObservations.Metric.values().associateWith { metric ->
            result.observations.count { it.metric == metric }
        }
        val duplicateGroups = result.potentialDuplicates()
        val missingTimestamps = result.observations.count {
            it.quality == FitHistoricalObservations.Quality.MISSING_TIMESTAMP
        }
        val outsideRange = result.observations.count {
            it.quality == FitHistoricalObservations.Quality.OUTSIDE_DISPLAY_RANGE
        }
        val timestamped = result.observations.mapNotNull { it.timestampEpochMillis }
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
        val summary = buildString {
            appendLine("FIT envelope + CRC: gültig · ${bytes} Bytes · offline")
            appendLine("Mess-/Monitoring-Datensätze: ${result.dataRecordCount}")
            appendLine("Unterstützte Felder:")
            for (metric in FitHistoricalObservations.Metric.values()) {
                appendLine("  ${metric.name}: ${counts.getValue(metric)} Beobachtungen (${metric.unit})")
            }
            appendLine("Zeitstempel: ${timestamped.size}/${result.observations.size} vorhanden" +
                (if (timestamped.isEmpty()) "" else " · ${formatter.format(Instant.ofEpochMilli(timestamped.minOrNull()!!))} … ${formatter.format(Instant.ofEpochMilli(timestamped.maxOrNull()!!))}"))
            appendLine("Qualität: ohne Zeitstempel=$missingTimestamps · außerhalb HR-Anzeigebereich=$outsideRange")
            appendLine("Mögliche Duplikatgruppen=${duplicateGroups.size}; keine automatische Zusammenführung.")
            appendLine("Fehlerhafte/unerwartete unterstützte Feldtypen=${result.unsupportedMeasurementFieldCount}")
            appendLine("Kein Datenbankimport; unbekannte Felder wurden nicht geraten.")
            appendLine()
            appendLine("Beobachtungen (maximal 80 angezeigt):")
            result.observations.take(80).forEach { observation ->
                val time = observation.timestampEpochMillis?.let {
                    formatter.format(Instant.ofEpochMilli(it))
                } ?: "Zeitstempel fehlt"
                appendLine("${observation.metric.name} · $time · " +
                    "${String.format(Locale.ROOT, "%.3f", observation.value)} ${observation.unit} · ${observation.quality}")
            }
            if (result.observations.size > 80) {
                appendLine("… ${result.observations.size - 80} weitere Beobachtungen nicht einzeln dargestellt.")
            }
        }
        fitOutput.text = summary
        saveRecord(
            CirqaTestLaboratory.TestId.FIT_FILE,
            CirqaTestLaboratory.Verdict.PASSED,
            CirqaTestLaboratory.EvidenceLevel.OFFLINE_VALIDATED,
            "FIT-CRC/Struktur gültig; Datensätze=${result.dataRecordCount}; " +
                "unterstützte Beobachtungen=${result.observations.size}; mögliche Duplikatgruppen=${duplicateGroups.size}; lokal, kein DB-Import."
        )
    }

    override fun onStop() {
        if (scanning) {
            handler.removeCallbacks(scanTimeout)
            val callback = scanCallback
            try { if (callback != null) adapter.bluetoothLeScanner?.stopScan(callback) }
            catch (_: SecurityException) { }
            catch (_: IllegalStateException) { }
            scanning = false
            scanCallback = null
        }
        if (monitorRunning) {
            monitorRunning = false
            handler.removeCallbacks(connectionPoll)
            saveRecord(
                CirqaTestLaboratory.TestId.ANDROID_STATUS,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "30-s-Statusbeobachtung vor Abschluss beendet; Ergebnis nicht gewertet."
            )
        }
        if (gattProbe != null && discoveryOutput.text.toString().contains("läuft", ignoreCase = true)) {
            saveRecord(
                CirqaTestLaboratory.TestId.GATT_DISCOVERY,
                CirqaTestLaboratory.Verdict.NOT_TESTED,
                CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                "GATT-Dienstsuche beim Verlassen beendet; Ergebnis nicht gewertet."
            )
        }
        gattProbe?.close()
        gattProbe = null
        if (mlProbe != null) {
            mlProbe?.close()
            mlProbe = null
            if (liveOutput.text.toString().contains("läuft", ignoreCase = true)) {
                liveOutput.text = "Live-Probe beim Verlassen des Testlabors beendet."
                saveRecord(
                    CirqaTestLaboratory.TestId.LIVE_READINGS,
                    CirqaTestLaboratory.Verdict.NOT_TESTED,
                    CirqaTestLaboratory.EvidenceLevel.UNPROVEN,
                    "Live-Probe vor Abschluss beendet; Ergebnis nicht gewertet."
                )
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        rfcommRunner?.stop()
        systemEventProbe?.close()
        destroyed = true
        monitorRunning = false
        handler.removeCallbacksAndMessages(null)
        gattProbe?.close()
        mlProbe?.close()
        io.shutdownNow()
        super.onDestroy()
    }
}

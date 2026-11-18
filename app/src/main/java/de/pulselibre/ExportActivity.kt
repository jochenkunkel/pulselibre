package de.pulselibre

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.Executors

/** User-initiated local CSV export. Never transmits measurements over the network. */
class ExportActivity : AppCompatActivity() {
    private var from = LocalDate.now().minusDays(30)
    private var through = LocalDate.now()
    private var fromTime = LocalTime.MIDNIGHT
    private var throughTime = LocalTime.of(23, 59)
    private val zone = ZoneId.systemDefault()
    private lateinit var fromButton: TextView
    private lateinit var throughButton: TextView
    private lateinit var fromTimeButton: TextView
    private lateinit var throughTimeButton: TextView
    private lateinit var feedback: TextView
    private lateinit var databaseFeedback: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val operationHandler = Handler(Looper.getMainLooper())
    private var pendingWindow: ExportWindow? = null

    private val createDatabaseBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            databaseFeedback.text = "Backup abgebrochen."
        } else {
            databaseFeedback.text = "Backup wird erstellt …"
            executor.execute {
                try {
                    val output = contentResolver.openOutputStream(uri)
                        ?: error("Backup-Ziel kann nicht geöffnet werden")
                    val report = output.use { DatabaseBackup.write(this, it) }
                    runOnUiThread {
                        databaseFeedback.text = "Backup gespeichert: ${report.events} Ereignisse, " +
                            "${report.legacyReadings} historische Messwerte, " +
                            "${report.fitSources} historische FIT-Quellen/${report.fitProvenance} Herkunftszeilen; " +
                            "${report.canonicalReadings} kanonische Messwerte, " +
                            "${report.canonicalFitSources} kanonische FIT-Quellen/" +
                            "${report.canonicalFitProvenance} Herkunftszeilen. " +
                            "FIT-Originaldateien sind nicht enthalten."
                    }
                } catch (_: Exception) {
                    runOnUiThread { databaseFeedback.text = "Backup fehlgeschlagen. Bitte Speicherort prüfen." }
                }
            }
        }
    }

    private val restoreDatabaseBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            databaseFeedback.text = "Backup wird geprüft …"
            executor.execute {
                try {
                    val input = contentResolver.openInputStream(uri)
                        ?: error("Backup-Datei kann nicht geöffnet werden")
                    val plan = input.use { DatabaseBackup.prepareRestore(this, it) }
                    runOnUiThread {
                        if (isFinishing || isDestroyed) plan.close() else confirmRestore(plan)
                    }
                } catch (_: Exception) {
                    runOnUiThread { databaseFeedback.text = "Backup ungültig oder nicht lesbar. Datenbank unverändert." }
                }
            }
        }
    }

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val window = pendingWindow
        pendingWindow = null
        if (uri == null || window == null) {
            feedback.text = "Export abgebrochen."
        } else {
            feedback.text = "Export wird gespeichert …"
            executor.execute {
                try {
                    val records = readUnifiedExport(window)
                    val stream = contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Datei kann nicht geöffnet werden")
                    stream.use { writeRawExport(it, records, window) }
                    runOnUiThread {
                        feedback.text = "CSV gespeichert: ${records.size} Messungen. Enthält nur lokal verfügbare Daten."
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        feedback.text = "Export fehlgeschlagen. Bitte Speicherort prüfen und erneut versuchen."
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreSelection(savedInstanceState)
        val palette = AppPalette.of(this)
        val violet = palette.accent
        val ink = palette.ink
        val muted = palette.muted
        fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
        fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
        fun label(value: String, size: Float, color: Int = ink, bold: Boolean = false) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                includeFontPadding = false
                if (bold) setTypeface(null, Typeface.BOLD)
            }
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.background)
            fitsSystemWindows = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(palette.header, palette.headerEnd)
            )
        }
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(label("‹", 32f, Color.WHITE), LinearLayout.LayoutParams(dp(40), dp(48)).apply {
            rightMargin = dp(8)
        })
        headerRow.getChildAt(0).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Zurück"
            setOnClickListener { finish() }
        }
        headerRow.addView(label("Daten", 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        headerRow.addView(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_humerus_settings)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = "PulsLibre Armbruch-Symbol"
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        header.addView(headerRow)
        header.addView(label("Export · Backup · Wiederherstellung · lokal und offline", 12f, Color.WHITE))
        screen.addView(header)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(16))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        screen.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(palette.card)
        }
        root.addView(panel)
        panel.addView(label("Zeitraum", 17f, ink, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        fun dateField(): TextView = label("", 14f, violet, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(48)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(palette.tinted, 12)
            isClickable = true
            isFocusable = true
        }
        fromButton = dateField().apply {
            setOnClickListener { pickDate(from) { from = it; updateLabels() } }
        }
        throughButton = dateField().apply {
            setOnClickListener { pickDate(through) { through = it; updateLabels() } }
        }
        fromTimeButton = dateField().apply {
            setOnClickListener { pickTime(fromTime) { fromTime = it; updateLabels() } }
        }
        throughTimeButton = dateField().apply {
            setOnClickListener { pickTime(throughTime) { throughTime = it; updateLabels() } }
        }
        fun intervalRow(title: String, date: TextView, time: TextView): LinearLayout {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(label(title, 13f, muted, true),
                LinearLayout.LayoutParams(dp(42), -2))
            row.addView(date, LinearLayout.LayoutParams(0, -2, 1.6f).apply {
                rightMargin = dp(8)
            })
            row.addView(time, LinearLayout.LayoutParams(0, -2, 1f))
            return row
        }
        panel.addView(intervalRow("Von", fromButton, fromTimeButton),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        panel.addView(intervalRow("Bis", throughButton, throughTimeButton))
        feedback = label("${zone.id} · Endminute inklusive", 11f, muted)
        panel.addView(feedback, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
            bottomMargin = dp(14)
        })
        val exportButton = label("⬇  CSV exportieren", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(54)
            background = rounded(palette.primary, 14)
            isClickable = true
            isFocusable = true
            contentDescription = "Messwerte als CSV exportieren"
            setOnClickListener {
                try {
                    val start = from.atTime(fromTime).atZone(zone).toInstant().toEpochMilli()
                    val end = through.atTime(throughTime).atZone(zone).toInstant().toEpochMilli() + 60_000L
                    require(end > start) { "Das Ende muss nach dem Beginn liegen" }
                    pendingWindow = ExportWindow(start, end)
                    createDocument.launch("pulselibre_${from}_${fromTime.toString().replace(':', '-')}_bis_${through}_${throughTime.toString().replace(':', '-')}.csv")
                } catch (_: IllegalArgumentException) {
                    feedback.text = "Ungültiger Zeitraum: Das Ende muss nach dem Beginn liegen."
                }
            }
        }
        panel.addView(exportButton)
        panel.addView(label("🔒  Nur auf deinem Gerät · Keine Cloud · Keine Anmeldung", 12f, muted),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        val dataPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(palette.card)
        }
        root.addView(dataPanel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        dataPanel.addView(label("Lokale Datenbank", 17f, ink, true))
        dataPanel.addView(label(
            "Backup enthält CIRQA-Ereignisse, historische Messwerte und Synchronisationsstände. " +
                "Die ZIP-Datei ist unverschlüsselt und enthält sensible Gesundheitsdaten.", 12f, muted
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(12) })
        fun databaseAction(title: String, description: String, background: Int, action: () -> Unit) =
            label(title, 15f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                minHeight = dp(52)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                this.background = rounded(background, 14)
                isClickable = true
                isFocusable = true
                contentDescription = description
                setOnClickListener { action() }
            }
        dataPanel.addView(databaseAction(
            "Backup erstellen", "Lokales Datenbankbackup erstellen", palette.primary
        ) {
            createDatabaseBackup.launch("pulslibre_backup_${LocalDate.now()}.zip")
        })
        dataPanel.addView(databaseAction(
            "Backup wiederherstellen", "Datenbank aus einem Backup wiederherstellen", palette.accent
        ) {
            restoreDatabaseBackup.launch(arrayOf("application/zip", "application/octet-stream"))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        dataPanel.addView(databaseAction(
            "Datenbank zurücksetzen", "Alle lokalen Datenbankeinträge löschen", Color.rgb(170, 52, 65)
        ) { confirmReset() }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        databaseFeedback = label("App-Einstellungen bleiben bei Wiederherstellung und Reset erhalten.", 12f, muted)
        dataPanel.addView(databaseFeedback, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        updateLabels()
        screen.addView(MainNavigationView(this, 4))
        setContentView(screen)
    }

    private fun confirmRestore(plan: PreparedDatabaseRestore) {
        AlertDialog.Builder(this)
            .setTitle("Datenbank ersetzen?")
            .setMessage(
                "Das Backup vom ${plan.createdAt} enthält ${plan.events} Ereignisse, " +
                    "${plan.legacyReadings} historische Messwerte, ${plan.syncCursors} Synchronisationsstände, " +
                    "${plan.fitSources} FIT-Quellen und ${plan.fitProvenance} FIT-Herkunftszeilen. " +
                    "${plan.canonicalReadings} kanonische Messwerte und " +
                    "${plan.canonicalFitSources} kanonische FIT-Quellen/" +
                    "${plan.canonicalFitProvenance} Herkunftszeilen. " +
                    "FIT-Originaldateien sind nicht enthalten.\n\n" +
                    "Alle aktuell gespeicherten Datenbankeinträge werden ersetzt. " +
                    "App-Einstellungen bleiben erhalten. Nur mit einem Backup aus vertrauenswürdiger Quelle fortfahren."
            )
            .setNegativeButton("Abbrechen") { _, _ ->
                plan.close()
                databaseFeedback.text = "Wiederherstellung abgebrochen."
            }
            .setPositiveButton("Ersetzen") { _, _ ->
                stopRecorderThen(onCancel = { plan.close() }) {
                    databaseFeedback.text = "Datenbank wird wiederhergestellt …"
                    executor.execute {
                        try {
                            DatabaseBackup.restore(this, plan); HistoryDataCache.invalidate()
                            runOnUiThread { databaseFeedback.text = "Datenbank aus Backup wiederhergestellt." }
                        } catch (_: Exception) {
                            runOnUiThread { databaseFeedback.text = "Wiederherstellung fehlgeschlagen. Bitte Datenbank prüfen." }
                        } finally { plan.close() }
                    }
                }
            }
            .setOnCancelListener {
                plan.close()
                databaseFeedback.text = "Wiederherstellung abgebrochen."
            }
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Lokale Datenbank zurücksetzen?")
            .setMessage(
                "Alle lokal gespeicherten CIRQA-Ereignisse, historischen Messwerte und Synchronisationsstände werden dauerhaft gelöscht. " +
                    "App-Einstellungen bleiben erhalten. Diese Aktion kann nicht rückgängig gemacht werden."
            )
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Daten löschen") { _, _ ->
                stopRecorderThen {
                    databaseFeedback.text = "Datenbank wird zurückgesetzt …"
                    executor.execute {
                        try {
                            DatabaseBackup.reset(this); HistoryDataCache.invalidate()
                            runOnUiThread { databaseFeedback.text = "Lokale Datenbank wurde geleert." }
                        } catch (_: Exception) {
                            runOnUiThread { databaseFeedback.text = "Reset fehlgeschlagen. Datenbank bitte prüfen." }
                        }
                    }
                }
            }
            .show()
    }

    /** Stop the explicit background recorder before a destructive database operation. */
    private fun stopRecorderThen(onCancel: () -> Unit = {}, action: () -> Unit) {
        val prefs = getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE)
        if (!RecorderStatus.isRunning(prefs)) {
            action()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Aufzeichnung läuft")
            .setMessage("Die laufende lokale CIRQA-Aufzeichnung muss vor dem Datenbankvorgang beendet werden.")
            .setNegativeButton("Abbrechen") { _, _ -> onCancel() }
            .setPositiveButton("Beenden und fortfahren") { _, _ ->
                try {
                    startService(Intent(this, CirqaContinuousRecordingService::class.java)
                        .setAction(CirqaContinuousRecordingService.ACTION_STOP))
                } catch (_: Exception) {
                    databaseFeedback.text = "Aufzeichnung konnte nicht beendet werden; Datenbank blieb unverändert."
                    onCancel()
                    return@setPositiveButton
                }
                val deadline = SystemClock.elapsedRealtime() + 10_000L
                val wait = object : Runnable {
                    override fun run() {
                        if (!RecorderStatus.isRunning(prefs)) action()
                        else if (SystemClock.elapsedRealtime() >= deadline) {
                            databaseFeedback.text = "Aufzeichnung läuft noch; Datenbank blieb unverändert."
                            onCancel()
                        } else operationHandler.postDelayed(this, 100L)
                    }
                }
                operationHandler.post(wait)
            }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("export_from", from.toString())
        outState.putString("export_through", through.toString())
        outState.putString("export_from_time", fromTime.toString())
        outState.putString("export_through_time", throughTime.toString())
        pendingWindow?.let {
            outState.putLong("export_pending_start", it.startInclusiveEpochMillis)
            outState.putLong("export_pending_end", it.endExclusiveEpochMillis)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        operationHandler.removeCallbacksAndMessages(null)
        executor.shutdown()
        super.onDestroy()
    }

    private fun restoreSelection(state: Bundle?) {
        if (state == null) return
        from = state.getString("export_from")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: from
        through = state.getString("export_through")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: through
        fromTime = state.getString("export_from_time")?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: fromTime
        throughTime = state.getString("export_through_time")?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: throughTime
        if (state.containsKey("export_pending_start") && state.containsKey("export_pending_end")) {
            val start = state.getLong("export_pending_start")
            val end = state.getLong("export_pending_end")
            pendingWindow = ExportWindow(start, end).takeIf { end > start }
        }
    }

    private fun updateLabels() {
        fromButton.text = from.toString()
        throughButton.text = through.toString()
        fromTimeButton.text = fromTime.toString()
        throughTimeButton.text = throughTime.toString()
    }

    private fun pickTime(current: LocalTime, onSelected: (LocalTime) -> Unit) {
        TimePickerDialog(this, { _, hour, minute ->
            onSelected(LocalTime.of(hour, minute))
        }, current.hour, current.minute, true).show()
    }

    private fun pickDate(current: LocalDate, onSelected: (LocalDate) -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            onSelected(LocalDate.of(year, month + 1, day))
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    /**
     * CIRQA realtime readings are persisted in CirqaDatabase, not LocalReadingStore.
     * Export each observed numeric field as a separate CSV row. The device does not
     * provide a measurement timestamp here: use received_at_utc for both time columns.
     * No invented device identifier, history or sensor values.
     */
    private fun readUnifiedExport(window: ExportWindow): List<RawExportRecord> {
        val (legacy, normalized) = CirqaDatabase(this).use { db ->
            db.ensureHistoricalStoreCopied()
            db.importLegacyReadings()
            val legacyRows = db.legacyLines().mapNotNull { line ->
                try {
                    val item = JSONObject(line)
                    val time = item.getLong("measured_at_ms")
                    if (time < window.startInclusiveEpochMillis ||
                        time >= window.endExclusiveEpochMillis) null
                    else RawExportRecord(
                        deviceId = item.getString("device_id"),
                        metric = item.getString("metric"),
                        measuredAtEpochMillis = time,
                        value = item.getDouble("value"),
                        unit = item.getString("unit"),
                        sampleId = item.optString("sample_id").ifEmpty { null },
                        receivedAtEpochMillis = if (item.isNull("received_at_ms")) null
                            else item.getLong("received_at_ms")
                    )
                } catch (_: Exception) { null }
            }
            legacyRows to db.historicalObservationWindow(window)
        }
        return (legacy + normalized + readCirqaExport(window)).distinctBy {
            listOf(it.deviceId, it.metric, it.measuredAtEpochMillis.toString(), it.sampleId.orEmpty())
                .joinToString("\u0000")
        }
    }

    private fun readCirqaExport(window: ExportWindow): List<RawExportRecord> {
        val fields = mapOf(
            6 to listOf("bpm" to "bpm", "resting_bpm" to "bpm"),
            7 to listOf("steps" to "steps", "goal" to "steps"),
            8 to listOf("total" to "kcal", "active" to "kcal"),
            10 to listOf("moderate" to "min", "vigorous" to "min"),
            12 to listOf("rr_ms" to "ms"),
            13 to listOf("stress" to "device_scale"),
            19 to listOf("spo2_percent" to "%"),
            20 to listOf("body_battery" to "device_scale"),
            21 to listOf("breaths_per_min" to "breaths/min")
        )
        return CirqaDatabase(this).use { db ->
            db.readingLines().flatMapIndexed { index, line ->
                val item = try { JSONObject(line) } catch (_: Exception) { return@flatMapIndexed emptyList() }
                if (item.optInt("schema") != 1 || item.optString("type") != "reading") {
                    return@flatMapIndexed emptyList()
                }
                val received = try {
                    Instant.parse(item.getString("received_at_utc")).toEpochMilli()
                } catch (_: Exception) { return@flatMapIndexed emptyList() }
                if (received < window.startInclusiveEpochMillis ||
                    received >= window.endExclusiveEpochMillis) return@flatMapIndexed emptyList()
                val service = item.optInt("service", -1)
                fields[service].orEmpty().mapNotNull { (key, unit) ->
                    if (!item.has(key) || item.isNull(key)) return@mapNotNull null
                    val number = item.opt(key) as? Number ?: return@mapNotNull null
                    val value = number.toDouble()
                    if (!value.isFinite()) return@mapNotNull null
                    RawExportRecord(
                        deviceId = "CIRQA",
                        metric = key,
                        measuredAtEpochMillis = received,
                        value = value,
                        unit = unit,
                        sampleId = "cirqa-$index-$key",
                        receivedAtEpochMillis = received
                    )
                }
            }
        }
    }

}

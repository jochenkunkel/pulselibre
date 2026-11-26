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
    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(AppLanguageSettings.wrap(newBase))
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
    /** Bar and step text while a backup is created, checked or restored (null phase = hidden). */
    private lateinit var databaseProgress: android.widget.ProgressBar
    @Volatile private var progressShownKey: String? = null

    /**
     * Worker-thread progress → main thread. Only a changed text is posted, so a busy copy loop cannot flood the UI.
     * The feedback line keeps the step; "do not close the app" stays visible below it.
     */
    private fun progressSink(): BackupProgress = BackupProgress { phase, done, total ->
        val text = BackupProgressText.text(Strings.of(this), phase, done, total)
        if (text == progressShownKey) return@BackupProgress
        progressShownKey = text
        val percent = BackupProgressText.percent(phase, done, total)
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            databaseFeedback.text = text + "\n" + getString(R.string.ex_progress_keep_open)
            databaseProgress.visibility = android.view.View.VISIBLE
            databaseProgress.isIndeterminate = percent == null
            if (percent != null) databaseProgress.progress = percent
        }
    }

    private fun hideProgress() {
        progressShownKey = null
        if (::databaseProgress.isInitialized) databaseProgress.visibility = android.view.View.GONE
    }
    private val executor = Executors.newSingleThreadExecutor()
    private val operationHandler = Handler(Looper.getMainLooper())
    private var pendingWindow: ExportWindow? = null

    private val createDatabaseBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            databaseFeedback.text = getString(R.string.ex_backup_cancelled)
        } else {
            databaseFeedback.text = getString(R.string.ex_backup_running)
            executor.execute {
                try {
                    val output = contentResolver.openOutputStream(uri)
                        ?: error("Backup-Ziel kann nicht geöffnet werden")
                    val report = output.use { DatabaseBackup.write(this, it, progressSink()) }
                    runOnUiThread {
                        hideProgress()
                        databaseFeedback.text = getString(R.string.ex_backup_done, report.events, report.legacyReadings, report.fitSources, report.fitProvenance, report.canonicalReadings, report.canonicalFitSources, report.canonicalFitProvenance, report.canonicalDeviceMetadata, report.rawDeviceMetadata)
                    }
                } catch (e: Exception) {
                    // The picker already created the target file: never leave a 0-byte "backup" behind.
                    val removed = runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) }.getOrDefault(false)
                    val reason = BackupFailure.reason(e)
                    runOnUiThread {
                        hideProgress()
                        databaseFeedback.text = getString(if (removed) R.string.ex_backup_failed_removed else R.string.ex_backup_failed) +
                            "\n" + getString(R.string.ex_backup_reason, reason)
                    }
                } catch (e: OutOfMemoryError) {
                    runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) }
                    runOnUiThread { hideProgress(); databaseFeedback.text = getString(R.string.ex_backup_failed) + "\n" + getString(R.string.ex_backup_reason, "OutOfMemoryError") }
                }
            }
        }
    }

    private val restoreDatabaseBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            databaseFeedback.text = getString(R.string.ex_backup_checking)
            executor.execute {
                try {
                    val input = contentResolver.openInputStream(uri)
                        ?: error("Backup-Datei kann nicht geöffnet werden")
                    val plan = input.use { DatabaseBackup.prepareRestore(this, it, progressSink()) }
                    runOnUiThread {
                        hideProgress()
                        if (isFinishing || isDestroyed) plan.close() else confirmRestore(plan)
                    }
                } catch (e: Exception) {
                    val reason = BackupFailure.reason(e)
                    runOnUiThread { hideProgress(); databaseFeedback.text = getString(R.string.ex_backup_invalid) + "\n" + getString(R.string.ex_backup_reason, reason) }
                }
            }
        }
    }

    /** #328: raw FIT originals as a ZIP, only after a warning and into a location the user picks. */
    private val createFitOriginals = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            databaseFeedback.text = getString(R.string.ex_fit_cancelled)
        } else {
            databaseFeedback.text = getString(R.string.ex_fit_running)
            executor.execute {
                try {
                    val output = contentResolver.openOutputStream(uri) ?: error("Ziel kann nicht geöffnet werden")
                    val result = output.use { FitOriginalsArchive.write(java.io.File(filesDir, "verified_fit"), it) }
                    runOnUiThread {
                        databaseFeedback.text = getString(R.string.ex_fit_done, result.files) +
                            (if (result.skipped > 0) getString(R.string.ex_fit_skipped, result.skipped) else "") +
                            getString(R.string.ex_zip_unencrypted)
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        databaseFeedback.text = getString(R.string.ex_export_failed)
                    }
                }
            }
        }
    }

    /** Stored rows that could not be parsed during the current export read (A6-8); shown to the user, never hidden. */
    private val unreadableRows = java.util.concurrent.atomic.AtomicInteger()

    /** Explicit user-selected export of opaque, verified raw CIRQA originals. */
    private val createVerifiedRawOriginals = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            databaseFeedback.text = getString(R.string.ex_raw_cancelled)
        } else {
            databaseFeedback.text = getString(R.string.ex_raw_running)
            executor.execute {
                try {
                    val output = contentResolver.openOutputStream(uri) ?: error("Ziel kann nicht geöffnet werden")
                    val result = output.use {
                        CirqaVerifiedRawArchive.write(java.io.File(filesDir, "verified_raw"), it)
                    }
                    runOnUiThread {
                        databaseFeedback.text = getString(R.string.ex_raw_done, result.files, result.bytes) +
                            (if (result.skipped > 0) getString(R.string.ex_raw_skipped, result.skipped) else "") +
                            getString(R.string.ex_raw_tail)
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        databaseFeedback.text = getString(R.string.ex_export_failed)
                    }
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
            feedback.text = getString(R.string.ex_csv_cancelled)
        } else {
            feedback.text = getString(R.string.ex_csv_running)
            executor.execute {
                try {
                    val records = readUnifiedExport(window)
                    val stream = contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Datei kann nicht geöffnet werden")
                    stream.use { writeRawExport(it, records, window) }
                    runOnUiThread {
                        val unreadable = unreadableRows.get()
                        feedback.text = getString(R.string.ex_csv_done, records.size) +
                            (if (unreadable > 0) getString(R.string.ex_csv_unreadable, unreadable) else "")
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        feedback.text = getString(R.string.ex_csv_failed)
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
        headerRow.addView(label("‹", 32f, Color.WHITE), LinearLayout.LayoutParams(dp(48), dp(48)).apply { // 48 dp touch target
            rightMargin = dp(0) // the wider target takes the former margin, the title keeps its position
        })
        headerRow.getChildAt(0).apply {
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.ex_back)
            setOnClickListener { finish() }
        }
        headerRow.addView(label(getString(R.string.ex_title), 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        headerRow.addView(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_humerus_settings)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.ex_icon_cd)
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        header.addView(headerRow)
        header.addView(label(getString(R.string.ex_subtitle), 12f, Color.WHITE))
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
        panel.addView(label(getString(R.string.ex_period), 17f, ink, true),
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
        panel.addView(intervalRow(getString(R.string.ex_from), fromButton, fromTimeButton),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        panel.addView(intervalRow(getString(R.string.ex_to), throughButton, throughTimeButton))
        feedback = label(getString(R.string.ex_zone, zone.id), 11f, muted)
        panel.addView(feedback, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
            bottomMargin = dp(14)
        })
        val exportButton = label(getString(R.string.ex_csv_button), 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(54)
            background = rounded(palette.primary, 14)
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.ex_csv_cd)
            setOnClickListener {
                try {
                    val start = from.atTime(fromTime).atZone(zone).toInstant().toEpochMilli()
                    val end = through.atTime(throughTime).atZone(zone).toInstant().toEpochMilli() + 60_000L
                    require(end > start) { "Das Ende muss nach dem Beginn liegen" }
                    pendingWindow = ExportWindow(start, end)
                    createDocument.launch("pulselibre_${from}_${fromTime.toString().replace(':', '-')}_bis_${through}_${throughTime.toString().replace(':', '-')}.csv")
                } catch (_: IllegalArgumentException) {
                    feedback.text = getString(R.string.ex_invalid_period)
                }
            }
        }
        panel.addView(exportButton)
        panel.addView(label(getString(R.string.ex_local_only), 12f, muted).apply {
            setCompoundDrawablesRelative(PlIcon.drawable(this@ExportActivity, PlIcon.STORAGE, 16, muted), null, null, null)
            compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
        },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        val dataPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(palette.card)
        }
        root.addView(dataPanel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        dataPanel.addView(label(getString(R.string.ex_db_title), 17f, ink, true))
        dataPanel.addView(label(
            getString(R.string.ex_db_note), 12f, muted
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
            getString(R.string.ex_btn_backup), getString(R.string.ex_btn_backup_cd), palette.primary
        ) {
            createDatabaseBackup.launch("pulslibre_backup_${LocalDate.now()}.zip")
        })
        dataPanel.addView(databaseAction(
            getString(R.string.ex_btn_fit), getString(R.string.ex_btn_fit_cd), palette.accent
        ) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.ex_fit_confirm_title))
                .setMessage(getString(R.string.ex_fit_confirm_msg))
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setPositiveButton(getString(R.string.ex_export_btn)) { _, _ ->
                    createFitOriginals.launch("pulselibre_fit_originale_${LocalDate.now()}.zip")
                }
                .show()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        dataPanel.addView(databaseAction(
            getString(R.string.ex_btn_raw),
            getString(R.string.ex_btn_raw_cd),
            palette.accent
        ) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.ex_raw_confirm_title))
                .setMessage(
                    getString(R.string.ex_raw_confirm_msg)
                )
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setPositiveButton(getString(R.string.ex_export_btn)) { _, _ ->
                    createVerifiedRawOriginals.launch("pulslibre_rohdateien_${LocalDate.now()}.zip")
                }
                .show()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        dataPanel.addView(databaseAction(
            getString(R.string.ex_btn_restore), getString(R.string.ex_btn_restore_cd), palette.accent
        ) {
            restoreDatabaseBackup.launch(arrayOf("application/zip", "application/octet-stream"))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        dataPanel.addView(databaseAction(
            getString(R.string.ex_btn_reset), getString(R.string.ex_btn_reset_cd), Color.rgb(170, 52, 65)
        ) { confirmReset() }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        databaseFeedback = label(getString(R.string.ex_prefs_kept), 12f, muted)
        databaseProgress = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; visibility = android.view.View.GONE; tag = "database_progress"
            progressTintList = android.content.res.ColorStateList.valueOf(palette.accent)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO // the text below says the same
        }
        dataPanel.addView(databaseProgress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(12) })
        dataPanel.addView(databaseFeedback, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        // Screen readers hear each step as it changes.
        androidx.core.view.ViewCompat.setAccessibilityLiveRegion(databaseFeedback, androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        updateLabels()
        screen.addView(MainNavigationView(this, 4))
        setContentView(screen)
    }

    private fun confirmRestore(plan: PreparedDatabaseRestore) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.ex_restore_title))
            .setMessage(
                getString(R.string.ex_restore_msg, plan.createdAt, plan.events, plan.legacyReadings, plan.syncCursors, plan.fitSources, plan.fitProvenance,
                    plan.canonicalReadings, plan.canonicalFitSources, plan.canonicalFitProvenance, plan.canonicalDeviceMetadata, plan.rawDeviceMetadata,
                    if (plan.containsSettings) getString(R.string.ex_restore_settings_replaced, plan.appSettings)
                    else getString(R.string.ex_restore_settings_kept)) + "\n\n" + getString(R.string.ex_restore_originals_note)
            )
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ ->
                plan.close()
                databaseFeedback.text = getString(R.string.ex_restore_cancelled)
            }
            .setPositiveButton(getString(if (plan.containsSettings) R.string.ex_restore_all else R.string.ex_replace)) { _, _ ->
                stopRecorderThen(onCancel = { plan.close() }) { stopped -> runRestore(plan, includeData = true, restartRecorder = stopped) }
            }
            .apply {
                // Owner request 26.11.2026: a v12 backup can bring back only the configuration, leaving health data alone.
                if (plan.containsSettings) setNeutralButton(getString(R.string.ex_restore_settings_only)) { _, _ ->
                    runRestore(plan, includeData = false) // the databases are not touched: the recorder may keep running
                }
            }
            .setOnCancelListener {
                plan.close()
                databaseFeedback.text = getString(R.string.ex_restore_cancelled)
            }
            .show()
    }

    private fun runRestore(plan: PreparedDatabaseRestore, includeData: Boolean, restartRecorder: Boolean = false) {
        // Replacing the databases while a BLE session (e.g. a history download) still writes would mix both states.
        val gate = Any()
        if (includeData && !CirqaBleSessionGate.acquire(gate)) {
            databaseFeedback.text = getString(R.string.ex_ble_busy)
            plan.close()
            if (restartRecorder) restartRecorder()
            return
        }
        databaseFeedback.text = getString(if (includeData) R.string.ex_restore_running else R.string.ex_progress_settings)
        executor.execute {
            try {
                val settingsRestored = plan.containsSettings
                DatabaseBackup.restore(this, plan, progressSink(), includeData)
                if (includeData) HistoryDataCache.invalidate()
                runOnUiThread {
                    hideProgress()
                    databaseFeedback.text = getString(if (includeData) R.string.ex_restore_done else R.string.ex_restore_settings_done)
                    if (settingsRestored && !isFinishing && !isDestroyed) {
                        // Theme and language of the restored settings take effect now (activity is recreated).
                        AppAppearance.apply(AppAppearance.preference(this))
                        recreate()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { hideProgress(); databaseFeedback.text = getString(R.string.ex_restore_failed) }
            } finally {
                plan.close()
                CirqaBleSessionGate.release(gate)
                // FIT 26.11.: the previous recording state comes back after the restore, also when it failed.
                if (restartRecorder) runOnUiThread { restartRecorder() }
            }
        }
    }

    /** Starts the recording again that [stopRecorderThen] stopped; says so if Android refuses. */
    private fun restartRecorder() {
        val ok = try {
            androidx.core.content.ContextCompat.startForegroundService(this,
                Intent(this, CirqaContinuousRecordingService::class.java).setAction(CirqaContinuousRecordingService.ACTION_START))
            true
        } catch (_: Exception) { false }
        if (!isFinishing && !isDestroyed) databaseFeedback.append("\n" + getString(if (ok) R.string.ex_rec_restarted else R.string.ex_rec_restart_failed))
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.ex_reset_title))
            .setMessage(
                getString(R.string.ex_reset_msg)
            )
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.ex_reset_delete)) { _, _ ->
                stopRecorderThen { stopped ->
                    // FIT 26.11.: a running history download writes originals; deleting them under it would lose bytes.
                    val gate = Any()
                    if (!CirqaBleSessionGate.acquire(gate)) {
                        databaseFeedback.text = getString(R.string.ex_ble_busy)
                        if (stopped) restartRecorder()
                        return@stopRecorderThen
                    }
                    databaseFeedback.text = getString(R.string.ex_reset_running)
                    executor.execute {
                        try {
                            val deleted = DatabaseBackup.reset(this); HistoryDataCache.invalidate(); TrendCache.invalidate()
                            runOnUiThread { databaseFeedback.text = getString(R.string.ex_reset_done, deleted) }
                        } catch (_: Exception) {
                            runOnUiThread { databaseFeedback.text = getString(R.string.ex_reset_failed) }
                        } finally {
                            CirqaBleSessionGate.release(gate)
                            if (stopped) runOnUiThread { restartRecorder() }
                        }
                    }
                }
            }
            .show()
    }

    /** Stop the explicit background recorder before a destructive database operation. */
    /**
     * Stops the recording before the databases are replaced and waits until it has really ended: the running flag is
     * cleared AND the service is gone (its last write finished). [action] learns whether it was stopped here, so the
     * caller can start it again afterwards (FIT 26.11.2026: restore the previous recording state, also on errors).
     */
    private fun stopRecorderThen(onCancel: () -> Unit = {}, action: (stopped: Boolean) -> Unit) {
        val prefs = getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE)
        if (!RecorderStatus.isRunning(prefs) && !AutoStartRecording.isServiceAlive(this)) {
            action(false)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.ex_rec_title))
            .setMessage(getString(R.string.ex_rec_msg))
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ -> onCancel() }
            .setPositiveButton(getString(R.string.ex_rec_stop)) { _, _ ->
                try {
                    startService(Intent(this, CirqaContinuousRecordingService::class.java)
                        .setAction(CirqaContinuousRecordingService.ACTION_STOP))
                } catch (_: Exception) {
                    databaseFeedback.text = getString(R.string.ex_rec_stop_failed)
                    onCancel()
                    return@setPositiveButton
                }
                val deadline = SystemClock.elapsedRealtime() + 10_000L
                val wait = object : Runnable {
                    override fun run() {
                        if (!RecorderStatus.isRunning(prefs) && !AutoStartRecording.isServiceAlive(this@ExportActivity)) action(true)
                        else if (SystemClock.elapsedRealtime() >= deadline) {
                            databaseFeedback.text = getString(R.string.ex_rec_still)
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
        unreadableRows.set(0)
        val (legacy, unified, unreadable) = CirqaDatabase(this).use { db ->
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
                } catch (_: Exception) { unreadableRows.incrementAndGet(); null }
            }
            // Live, imported and band values all come from the one unified table (#514).
            val table = UnifiedMeasurements(db.writableDatabase)
            table.refresh()
            Triple(legacyRows, table.records(window.startInclusiveEpochMillis, window.endExclusiveEpochMillis),
                table.unreadableLiveEvents())
        }
        unreadableRows.addAndGet(unreadable.toInt())
        return (legacy + unified).distinctBy {
            listOf(it.deviceId, it.metric, it.measuredAtEpochMillis.toString(), it.sampleId.orEmpty())
                .joinToString("\u0000")
        }
    }

}

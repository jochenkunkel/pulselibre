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
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(AppLanguageSettings.wrap(newBase))
    private val handler = Handler(Looper.getMainLooper())
    private companion object {
        const val RECORDING_PAUSE_TIMEOUT_MS = 20_000L
        const val RECORDING_SETTLE_MS = 1_500L
    }
    /** One long-lived helper for the 1-second change poll instead of opening/closing the database every second (#114). */
    private val pollDatabase by lazy { CirqaDatabase(applicationContext) }
    private var dashboardRefresh: (() -> Unit)? = null
    private var connectionIndicatorRefresh: (() -> Unit)? = null
    private var cirqaLink = CirqaLinkStatus.Result(CirqaLinkStatus.Kind.UNAVAILABLE, false, "")
    private var historyRefresh: (() -> Unit)? = null
    private var refreshDailyFreshness: (() -> Unit)? = null
    private var refreshBandActivity: (() -> Unit)? = null
    private var refreshPositionTrack: (() -> Unit)? = null
    private var positionTrackTick = 0
    private var startTrackAfterPermission = false
    private var exportTrackId: Long? = null
    private val exportTrack = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/geo+json")
    ) { uri -> uri?.let { writeTrackExport(it) } }
    private var deviceOverviewRef: DeviceOverviewView? = null
    /** Re-reads the profile into the overview card; also needed after saving via the header settings button. */
    private var refreshBodyCardRef: (() -> Unit)? = null
    private var deviceStatsLoadedAt = 0L
    /** One bounded, read-only metadata query per visit to the "Geräte" tab (#142/#159). */
    private var deviceMetadataProbe: DeviceMetadataProbe? = null
    private var lastDeviceMetadataVisitAt = 0L
    private var dashboardVisible = true
    private var historyVisible = false
    private var devicesVisible = false
    private var debugDevicesVisible = false
    private var recorderRefresh: (() -> Unit)? = null
    private var dashboardPolling = false
    private var dashboardLastEventId = -1L
    private var dashboardLastRefreshAt = 0L
    private var historyLastEventId = -1L
    private var historyLastRefreshAt = 0L
    private val dashboardWorker = DashboardWorkerWatch.newDashboardWorker()
    private var historicalDownloadProbe: CirqaMlHandshakeProbe? = null
    private var historicalSyncStatusView: TextView? = null
    private var syncProgressView: HistorySyncProgressView? = null
    private var automaticHistoricalSyncActive = false
    private var manualHistoricalSyncActive = false

    private var pollInFlight = false
    private var readingDashboard = false
    private var pendingDashboard: ((CirqaDashboardReader.Summary) -> Unit)? = null

    private fun readDashboard(includeTotals: Boolean = false, show: (CirqaDashboardReader.Summary) -> Unit) {
        if (dashboardWorker.isShutdown) return
        if (!includeTotals && readingDashboard) {
            pendingDashboard = show
            return
        }
        if (!includeTotals) readingDashboard = true
        dashboardWorker.execute {
            val summary = try { CirqaDashboardReader(applicationContext).read(includeTotals = includeTotals) }
                catch (_: android.database.sqlite.SQLiteException) { null }
            handler.post {
                if (!includeTotals) readingDashboard = false
                if (!isFinishing && !isDestroyed) {
                    if (summary != null) show(summary)
                    if (!includeTotals) {
                        val pending = pendingDashboard
                        pendingDashboard = null
                        if (pending != null) readDashboard(show = pending)
                    }
                }
            }
        }
    }

    private val dashboardPoll = object : Runnable {
        override fun run() {
            if (!dashboardPolling || destroyed) return
            connectionIndicatorRefresh?.invoke()
            recorderRefresh?.invoke()
            if (positionTrackTick++ % 5 == 0) refreshPositionTrack?.invoke()
            if ((dashboardVisible || historyVisible) && !pollInFlight) {
                pollInFlight = true
                dashboardWorker.execute {
                    val latest = try { pollDatabase.latestEventId() }
                        catch (_: android.database.sqlite.SQLiteException) { null }
                    handler.post {
                        pollInFlight = false
                        if (dashboardPolling && !destroyed && latest != null) {
                            val now = android.os.SystemClock.elapsedRealtime()
                            // Live data arrives about every second: rebuilding every second kept the screen busy (battery,
                            // TalkBack focus, uiautomator never idle). New data refreshes after a short minimum interval.
                            if (dashboardVisible && LiveRefreshPolicy.due(latest != dashboardLastEventId,
                                    now - dashboardLastRefreshAt, LiveRefreshPolicy.DASHBOARD_MIN_MS)) {
                                dashboardLastEventId = latest
                                dashboardLastRefreshAt = now
                                dashboardRefresh?.invoke()
                            }
                            if (historyVisible && LiveRefreshPolicy.due(latest != historyLastEventId,
                                    now - historyLastRefreshAt, LiveRefreshPolicy.HISTORY_MIN_MS)) {
                                historyLastEventId = latest
                                historyLastRefreshAt = now
                                historyRefresh?.invoke(); refreshDailyFreshness?.invoke()
                            }
                        }
                    }
                }
            }
            if (dashboardPolling && !destroyed) handler.postDelayed(this, 1000L)
        }
    }
    private fun startDashboardPolling() {
        if (dashboardPolling) return
        dashboardPolling = true
        dashboardLastEventId = -1L
        dashboardLastRefreshAt = 0L
        handler.post(dashboardPoll)
    }
    private fun stopDashboardPolling() {
        dashboardPolling = false
        handler.removeCallbacks(dashboardPoll)
    }
    /**
     * Recover archived FIT originals, backfill verified files, and re-index
     * unknown fields on app entry (#149/#150/#204/#637). Work is bounded,
     * idempotent, off the UI thread, and never communicates with the device.
     */
    private fun scheduleUnknownFitInventoryScan() {
        // Never on the tile/dialog worker (they waited minutes behind this scan on the S10), and at most one per process.
        DashboardWorkerWatch.runMaintenanceOnce {
            try {
                val fitRoot = java.io.File(applicationContext.filesDir, "verified_fit")
                CirqaDatabase(applicationContext).use { database ->
                    database.archiveExistingDownloads()
                    database.recoverArchivedFitSources(fitRoot) {
                        Thread.currentThread().isInterrupted
                    }
                    CirqaFitBackfill(fitRoot, database, auditDb = database.writableDatabase).run {
                        Thread.currentThread().isInterrupted
                    }
                    CirqaUnknownFitInventory.scanAndPersist(
                        root = fitRoot,
                        store = database,
                        validatedAtEpochMillis = System.currentTimeMillis(),
                        cancelled = { Thread.currentThread().isInterrupted }
                    )
                }
            } catch (_: Exception) {
                // Best-effort and repeatable; retry on a later app entry without logging data.
            }
        }
    }
    override fun onResume() {
        super.onResume()
        startDashboardPolling()
        connectionIndicatorRefresh?.invoke()
        if (::adapter.isInitialized && ::status.isInitialized && !adapter.isEnabled) {
            status.text = getString(R.string.main_bt_off)
        }
        if (HistoricalSyncPolicy.AUTOMATIC_SYNC_ENABLED) {
            maybeStartAutomaticHistoricalSync()
        }
    }
    override fun onPause() {
        stopDashboardPolling()
        super.onPause()
    }

    private lateinit var adapter: BluetoothAdapter
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var devices: LinearLayout
    private lateinit var cirqaCard: TextView
    private lateinit var cirqaEvidenceView: TextView
    private val cirqaScanEvidence = CirqaScanEvidence()
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
    private var cirqaBondAddress: String? = null
    private var cirqaScanMatch = false
    private var cirqaGattState = ""
    private val cirqaConnectionHistory = CirqaConnectionHistory()

    /** The layout decides "narrow" once while it is built; see [onConfigurationChanged]. */
    private var builtNarrow = false

    /**
     * A rotation is handled here instead of recreating the activity (manifest `configChanges`, A6-11): a running history download
     * and its status survive. Only when the width crosses the "narrow phone" threshold the compact layout decision is stale; then
     * the activity is rebuilt as before.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if ((newConfig.screenWidthDp < 340) != builtNarrow) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        builtNarrow = resources.configuration.screenWidthDp < 340
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        val palette = AppPalette.of(this)
        val violet = palette.accent
        val ink = palette.ink
        val muted = palette.muted
        val surface = palette.background
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
            background = rounded(palette.card)
            elevation = dp(1).toFloat()
        }
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
            fitsSystemWindows = true
        }
        // Header redesign (flicker fix): the backdrop with the tagline is the first element of the scrolled content and simply
        // scrolls away. Only the thin title bar (logo, title, status and settings buttons) stays pinned as an overlay; its
        // background fades in while the backdrop slides underneath. No view changes its size while scrolling, so nothing can
        // feed back into the scroll range (the old size-switching header flickered).
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Extra bottom space shows the mountain ridges of the backdrop (#124).
            setPadding(dp(20), dp(12), dp(20), dp(46))
            minimumHeight = dp(170)
            background = HeaderBackdropDrawable(palette.dark)
        }
        header.addView(android.view.View(this), LinearLayout.LayoutParams(-1, dp(48)))   // room for the pinned title row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
            minimumHeight = dp(68)
        }
        headerRow.addView(PulseLogoView(this), LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(12) })
        val headerTitle = text("PulsLibre", 30f, Color.WHITE, true).apply {
            // #248: bei 320 dp bricht der Titel sonst mitten im Wort um -> einzeilig, Schrift schrumpft bis 18 sp.
            maxLines = 1
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 18, 30, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            setPadding(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
        }
        headerRow.addView(headerTitle, LinearLayout.LayoutParams(0, -2, 1f))
        // #649: one combined control with three touch areas: band/connection, band battery, settings.
        var wearInputs: WearableStatusReader.Inputs? = null
        val statusButton = WearableStatusControl(this).apply {
            compact = resources.configuration.screenWidthDp < 412
            settingsZone.setOnClickListener {
                PersonalSettings.show(this@MainActivity) {
                    refreshBodyCardRef?.invoke()
                    dashboardLastEventId = -1L
                    dashboardLastRefreshAt = 0L
                }
            }
            statusZone.setOnClickListener {
                val now = System.currentTimeMillis()
                val state = WearableStatus.state(cirqaLink.online, wearInputs?.respiration, now)
                DialogProbe.track(androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.wear_status_title)
                    .setMessage(WearableStatus.statusDetails(Strings.of(this@MainActivity), cirqaLink, state))
                    .setPositiveButton(R.string.action_close, null)
                    .create()).show()
            }
            batteryZone.setOnClickListener {
                val reading = wearInputs?.battery
                val shown = WearableStatus.battery(cirqaLink.online, reading, System.currentTimeMillis())
                DialogProbe.track(androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.wear_battery_title)
                    .setMessage(WearableStatus.batteryDetails(Strings.of(this@MainActivity), reading, shown))
                    .setPositiveButton(R.string.action_close, null)
                    .create()).show()
            }
        }
        var wearInputsAt = 0L
        var wearInputsLoading = false
        fun refreshAvatarConnectionBorder() {
            refreshCirqaCard()
            val link = cirqaLink
            deviceOverviewRef?.let { overviewView ->
                val running = RecorderStatus.isRunning(getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE))
                overviewView.update(link, running)
                val now = System.currentTimeMillis()
                if (overviewView.isShown && now - deviceStatsLoadedAt > 20_000L) {
                    deviceStatsLoadedAt = now
                    Thread {
                        val stats = try { DeviceStatsReader(applicationContext).read(java.time.Instant.now(), java.time.ZoneId.systemDefault()) } catch (_: Exception) { null }
                        // #142/#159: newest provenance-backed values drive the battery row and every extra row.
                        val metadata = try {
                            DeviceInfoRows.withFitSoftwareVersion(pollDatabase.latestLiveDeviceMetadata(),
                                runCatching { pollDatabase.latestFitSoftwareVersion() }.getOrNull())
                        } catch (_: Exception) { emptyList() }
                        val liveBattery = DeviceMetadataFormat.batteryReading(metadata)
                        val battery = liveBattery ?: DeviceBatteryReader.read(applicationContext)
                        runOnUiThread {
                            if (destroyed) return@runOnUiThread
                            if (stats != null) overviewView.showStats(stats)
                            overviewView.showMetadata(metadata)
                            overviewView.showBattery(battery)
                        }
                    }.start()
                }
            }
            // Stored data only (no BLE, no sync), at most every 15 s; the per-second refresh just re-evaluates freshness.
            val now = System.currentTimeMillis()
            if (!wearInputsLoading && now - wearInputsAt > 15_000L) {
                wearInputsLoading = true
                Thread {
                    val inputs = try { CirqaDatabase(applicationContext).use { WearableStatusReader.read(applicationContext, it) } } catch (_: Exception) { null }
                    runOnUiThread {
                        wearInputsLoading = false
                        wearInputsAt = System.currentTimeMillis()
                        if (inputs != null) wearInputs = inputs
                        if (!destroyed) connectionIndicatorRefresh?.invoke()
                    }
                }.start()
            }
            val inputs = wearInputs
            val state = WearableStatus.state(link.online, inputs?.respiration, now)
            val battery = WearableStatus.battery(link.online, inputs?.battery, now)
            val strings = Strings.of(this)
            statusButton.show(state, battery, WearableStatus.statusZoneDescription(strings, link, state),
                WearableStatus.batteryZoneDescription(strings, battery), getString(R.string.wear_zone_settings_cd))
        }
        connectionIndicatorRefresh = { refreshAvatarConnectionBorder() }
        headerRow.addView(statusButton, LinearLayout.LayoutParams(-2, dp(48)))
        // Large fonts / narrow screens: the subtitle uses the full width (no 60 dp indent) and wraps instead of being cut off.
        // #179/#209: reference variant 4 shows only "Deine Daten. Deine Kontrolle."; the former "· Offline" suffix was removed
        // (it was misread as a connection status). The offline-first promise belongs to Help/Privacy, not to the header.
        val taglineWraps = resources.configuration.screenWidthDp < 340 || resources.configuration.fontScale >= 1.3f
        val taglineRow = text(getString(R.string.main_tagline), 13f, Color.parseColor("#E9E1F7")).apply {
            contentDescription = getString(R.string.main_tagline)
        }
        header.addView(taglineRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); marginStart = if (taglineWraps) 0 else dp(60) })
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(header, LinearLayout.LayoutParams(-1, -2))
                addView(root, LinearLayout.LayoutParams(-1, -2))
            })
        }
        val body = android.widget.FrameLayout(this)
        body.addView(scroll, android.widget.FrameLayout.LayoutParams(-1, -1))
        body.addView(headerRow, android.widget.FrameLayout.LayoutParams(-1, -2, android.view.Gravity.TOP))
        screen.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        // Pinned title bar: transparent over the backdrop, opaque page colour once the backdrop slid underneath it.
        fun updateTitleBar(y: Int) {
            val start = (header.height - headerRow.height - dp(40)).coerceAtLeast(0)
            val t = ((y - start).toFloat() / dp(40)).coerceIn(0f, 1f)
            headerRow.setBackgroundColor(Color.argb((255 * t).toInt(), Color.red(palette.background), Color.green(palette.background), Color.blue(palette.background)))
            headerRow.elevation = if (t > 0.5f) dp(2).toFloat() else 0f
            val onPage = t > 0.5f && !palette.dark
            headerTitle.setTextColor(if (onPage) palette.ink else Color.WHITE)
            taglineRow.alpha = (1f - y.toFloat() / dp(36)).coerceIn(0f, 1f)
        }
        scroll.setOnScrollChangeListener { _, _, y, _, _ -> updateTitleBar(y) }
        header.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTitleBar(scroll.scrollY) }

        val overview = mutableListOf<android.view.View>()
        val historyViews = mutableListOf<android.view.View>()
        val deviceViews = mutableListOf<android.view.View>()
        val analysisViews = mutableListOf<android.view.View>()
        fun overviewView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            overview.add(view)
        }
        fun deviceView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            deviceViews.add(view)
            view.visibility = android.view.View.GONE
        }
        fun analysisView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            analysisViews.add(view)
            view.visibility = android.view.View.GONE
        }
        fun historyView(view: android.view.View, margin: Int = 12) {
            root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(margin) })
            historyViews.add(view)
            view.visibility = android.view.View.GONE
        }
        // Compact personal body card: user-entered profile, not measured BLE data.
        // Large fonts / narrow screens: drop the pictogram and use a smaller title so words never break.
        val largeText = resources.configuration.screenWidthDp < 340 || resources.configuration.fontScale >= 1.3f
        val heroText = DesignTokens.heroText(palette.dark)
        val heroSecondary = DesignTokens.heroSecondary(palette.dark)
        val heroAccent = DesignTokens.heroAccent(palette.dark)
        val bodyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(6), dp(16), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, DesignTokens.heroColors(palette.dark)).apply {
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), DesignTokens.heroStroke(palette.dark))
            }
            elevation = dp(2).toFloat()
        }
        val bodyHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        if (!largeText) bodyHeader.addView(android.widget.ImageView(this).apply {
            setImageDrawable(PlIcon.drawable(this@MainActivity, PlIcon.PROFILE, 22, android.graphics.Color.WHITE))
            scaleType = android.widget.ImageView.ScaleType.CENTER
            background = rounded(DesignTokens.BRAND_PURPLE, 11)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) })
        val titleColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleColumn.addView(text(getString(R.string.main_profile_title), if (largeText) 16f else 18f, heroText, true))
        titleColumn.addView(text(getString(R.string.main_profile_tap_edit), 13f, heroSecondary).apply { setPadding(0, dp(2), 0, 0); maxLines = 2 })
        bodyHeader.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        val healthInfo = android.widget.FrameLayout(this).apply {
            addView(PlIcon.view(this@MainActivity, PlIcon.INFO, 22), android.widget.FrameLayout.LayoutParams(dp(22), dp(22), android.view.Gravity.CENTER))
            contentDescription = getString(R.string.main_data_info_cd)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                readDashboard(includeTotals = true) { summary ->
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.main_data_dialog_title)
                    .setMessage(getString(R.string.main_data_dialog_message, summary.samples, summary.bytes / 1024))
                    .setPositiveButton(R.string.action_close, null)
                    .show()
                }
            }
        }
        bodyHeader.addView(healthInfo, LinearLayout.LayoutParams(dp(48), dp(48))) // 48 dp touch target (AccessibilityAuditTest)
        bodyCard.addView(bodyHeader)

        // Narrow screens or large system fonts: avatar centred above the values (never cut off, never wrapped per character).
        val compactProfile = resources.configuration.screenWidthDp < 340 || resources.configuration.fontScale >= 1.3f
        val bodyRow = LinearLayout(this).apply {
            orientation = if (compactProfile) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = if (compactProfile) android.view.Gravity.CENTER_HORIZONTAL else android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
        }
        val bodyFigure = BodyProfileAvatarView(this)
        val avatarSize = if (compactProfile) 72 else 88   // the profile is only background information: keep the card small
        bodyRow.addView(bodyFigure, LinearLayout.LayoutParams(dp(avatarSize), dp(avatarSize)).apply {
            if (compactProfile) bottomMargin = dp(6) else marginEnd = dp(30)
        })
        val bodyValues = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        bodyRow.addView(bodyValues, if (compactProfile) LinearLayout.LayoutParams(-1, -2) else LinearLayout.LayoutParams(0, -2, 1f))
        bodyCard.addView(bodyRow)
        fun refreshBodyCard() {
            val p = getSharedPreferences(SpO2DisplaySettings.STORE, MODE_PRIVATE)
            bodyValues.removeAllViews()
            bodyFigure.contentDescription = getString(R.string.main_body_figure_cd)
            val heightText = p.getString("height_cm", "").orEmpty()
            val weightText = p.getString("weight_kg", "").orEmpty()
            // Age is derived from the birth date for today; a legacy stored age is only a fallback (#117).
            val age = AgeCalculator.currentAge(p.getString("birth_date", ""), p.getString("age_years", ""), java.time.LocalDate.now())
            val bmi = Bmi.assess(weightText.replace(',', '.').toDoubleOrNull(), heightText.replace(',', '.').toDoubleOrNull(), age)
            val bmiFormat = java.text.NumberFormat.getNumberInstance(resources.configuration.locales[0]).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }
            val bmiTone = when (bmi.category?.tone) {
                Bmi.Tone.GREEN -> DesignTokens.MINT
                Bmi.Tone.YELLOW -> android.graphics.Color.rgb(240, 196, 40)
                Bmi.Tone.RED -> android.graphics.Color.rgb(255, 107, 120)
                null -> heroSecondary
            }
            fun add(icon: PlIcon, title: String, value: String, valueColor: Int = heroText, extra: String? = null, tagName: String? = null, description: String? = null, onClick: (() -> Unit)? = null) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                // fixed columns: icon | label | value, identical edges in all four rows
                row.addView(android.widget.ImageView(this).apply {
                    setImageDrawable(PlIcon.drawable(this@MainActivity, icon, 20, heroAccent))
                    scaleType = android.widget.ImageView.ScaleType.CENTER
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(22), dp(22)))
                if (compactProfile) {
                    val stack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER_VERTICAL }
                    stack.addView(text(title, 12f, heroSecondary)); stack.addView(text(value, 15f, valueColor, true))
                    row.addView(stack, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
                } else {
                    row.addView(text(title, 12f, heroSecondary), LinearLayout.LayoutParams(dp(58), -2).apply { marginStart = dp(4) })
                    row.addView(text(value, 15f, valueColor, true), LinearLayout.LayoutParams(0, -2, 1f))
                }
                if (extra != null) bodyValues.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(0) })
                else bodyValues.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(3) })
                if (extra != null) {
                    val dotColor = bmiTone
                    val span = android.text.SpannableString(extra)
                    if (extra.startsWith("●")) span.setSpan(android.text.style.ForegroundColorSpan(dotColor), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    bodyValues.addView(text("", 11f, heroSecondary).apply { this.text = span; contentDescription = extra.removePrefix("● ") },
                        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2); bottomMargin = dp(0); marginStart = dp(if (compactProfile) 28 else 84) })
                }
                if (onClick != null) {
                    row.isClickable = true; row.isFocusable = true; row.setOnClickListener { onClick() }
                    row.minimumHeight = dp(48) // touch target (AccessibilityAuditTest); was 34 dp
                    if (tagName != null) row.tag = tagName
                    if (description != null) row.contentDescription = description
                }
            }
            add(PlIcon.HEIGHT, getString(R.string.main_body_height), if (heightText.isBlank()) "–" else "$heightText cm")
            add(PlIcon.WEIGHT, getString(R.string.main_body_weight), if (weightText.isBlank()) "–" else "$weightText kg", tagName = "body_weight_row",
                description = getString(R.string.main_body_weight_cd, if (weightText.isBlank()) getString(R.string.main_body_weight_unset) else getString(R.string.main_body_weight_kg_words, weightText))) { showQuickWeight { refreshBodyCard() } }
            add(PlIcon.AGE, getString(R.string.main_body_age), age?.let { getString(R.string.main_body_age_years, it) } ?: "–")
            // BMI: value + written category (colour is never the only signal); tap explains reference and limits.
            add(PlIcon.BMI, getString(R.string.main_body_bmi), bmi.value?.let { bmiFormat.format(it) + " kg/m²" } ?: "–", heroText,
                extra = bmi.category?.let { "● " + DomainText.tr(this, it.label) } ?: bmi.note?.let { DomainText.tr(this, it) }) { showBmiInfo(bmi) }
            bodyCard.contentDescription = getString(R.string.main_body_card_cd) +
                (0 until bodyValues.childCount).joinToString(", ") { i ->
                    fun collect(v: android.view.View): String = when (v) {
                        is TextView -> v.text.toString()
                        is android.view.ViewGroup -> (0 until v.childCount).joinToString(" ") { collect(v.getChildAt(it)) }
                        else -> ""
                    }
                    collect(bodyValues.getChildAt(i))
                }
        }
        refreshBodyCard()
        refreshBodyCardRef = ::refreshBodyCard
        bodyCard.isClickable = true
        bodyCard.isFocusable = true
        bodyCard.setOnClickListener {
            PersonalSettings.show(this) {
                refreshBodyCard()
                dashboardLastEventId = -1L
                dashboardLastRefreshAt = 0L
            }
        }
        overviewView(bodyCard, 12)
        // One compact dashboard heading; privacy details remain accessible via the info button.
        overviewView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(android.widget.ImageView(this@MainActivity).apply {
                setImageDrawable(PlIcon.drawable(this@MainActivity, PlIcon.CIRQA_BAND, 20, android.graphics.Color.WHITE))
                scaleType = android.widget.ImageView.ScaleType.CENTER
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(DesignTokens.BRAND_PURPLE) }
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) })
            addView(text(getString(R.string.main_health_heading), 19f, ink, true))
        }, 6)
        val cirqaDashboard = DashboardOverviewView(this).apply {
            openDay = { title -> showDashboardDay(this@MainActivity, title, dashboardWorker) }
        }
        fun updateCirqaDashboard() {
            readDashboard { summary -> cirqaDashboard.render(summary.metrics, summary.activity) }
        }
        updateCirqaDashboard()
        dashboardRefresh = { updateCirqaDashboard() }
        overviewView(cirqaDashboard)
        // #227: compact teaser; the scientific views live in the main tab "Analysen" (no duplicated calculations).
        overviewView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            tag = "analysen_teaser"
            minimumHeight = dp(56)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(palette.card)
            isClickable = true; isFocusable = true
            contentDescription = getString(R.string.main_trend_link_cd)
            addView(PlIcon.view(this@MainActivity, PlIcon.FITNESS, 24, palette.accent), LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) })
            val texts = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(text(getString(R.string.main_trend_link_title), 14f, ink, true).apply { importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO })
            texts.addView(text(getString(R.string.main_trend_link_text), 12f, muted).apply { importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO })
            addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            setOnClickListener { selectMainTab(MainNavigationView.ANALYSEN) }
        }, 12)
        // #263: unified 24-hour view (heart rate + step counter on one axis).
        overviewView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            tag = "day_timeline_teaser"
            minimumHeight = dp(56)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(palette.card)
            isClickable = true; isFocusable = true
            contentDescription = getString(R.string.main_day_link_cd)
            addView(PlIcon.view(this@MainActivity, PlIcon.ACTIVITY_CALENDAR, 24, palette.accent), LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) })
            val texts = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            texts.addView(text(getString(R.string.main_day_link_title), 14f, ink, true).apply { importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO })
            texts.addView(text(getString(R.string.main_day_link_text), 12f, muted).apply { importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO })
            addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            setOnClickListener { showDayTimeline(this@MainActivity, dashboardWorker) }
        }, 12)
        val cirqaHistory = HistoryOverviewView(this) { range, offset -> CirqaHistoryReader(this).read(range, offset = offset) }
        historyRefresh = { cirqaHistory.refresh() }
        cirqaHistory.refresh()
        historyView(cirqaHistory)
        // #227: main tab "Analysen" with the five module groups; hosts the single trend block.
        lateinit var analysenView: AnalysenView
        analysenView = AnalysenView(this, goHistory = { selectMainTab(1) }, reveal = { y -> scroll.post { scroll.smoothScrollTo(0, analysenView.top + y) } })
        analysisView(analysenView, 12)
        val deviceHeading = text(getString(R.string.main_device_heading), 22f, ink, true)
        deviceView(deviceHeading)
        val debugDeviceViews = mutableListOf<android.view.View>()
        fun debugView(view: android.view.View, margin: Int = 12) {
            deviceView(view, margin)
            debugDeviceViews.add(view)
        }
        // #172: no visible test lab tab; a 3 s long press on the CIRQA heading opens it (also as TalkBack custom action).
        // Not a security barrier: the lab stays read-only/safe by default. The former inline debug panel is no longer reachable.
        attachHiddenLongPress(deviceHeading, 3000L, getString(R.string.main_open_testlab)) { DiagnosticsSheet.show(this) }
        debugView(text("Garmin CIRQA · Passive Diagnose ohne Verbindungsaufbau", 13f, muted))
        status = text(getString(R.string.main_status_ready), 14f, muted)
        debugView(status)
        fun actionButton(label: String, primary: Boolean = true, icon: PlIcon? = null, action: () -> Unit) =
            TextView(this).apply {
                text = label
                if (icon != null) {
                    val d = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, icon.res)!!.mutate()
                    androidx.core.graphics.drawable.DrawableCompat.setTint(d, if (primary) Color.WHITE else violet)
                    val size = dp(22); d.setBounds(0, 0, size, size)
                    setCompoundDrawablesRelative(d, null, null, null)
                    compoundDrawablePadding = dp(8)
                    contentDescription = icon.label
                }
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(if (primary) Color.WHITE else violet)
                background = rounded(if (primary) palette.primary else palette.tinted, 14)
                minHeight = dp(52)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        val historicalSyncStatus = text(
            // The last terminal report survives the screen, so an interrupted run stays visible as incomplete.
            getSharedPreferences("historical_sync_energy_policy", MODE_PRIVATE).getString("last_report", null)
                ?.let { SyncReportSummary.display(Strings.of(this), it) }
                ?: getString(R.string.main_hist_status_none), 13f, muted
        ).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(palette.card)
            contentDescription = text.toString()
        }
        historicalSyncStatusView = historicalSyncStatus
        val syncProgress = HistorySyncProgressView(this)
        syncProgressView = syncProgress
        // The report is a technical diagnostic string: show a short preview, expand on tap (#172: no reverse-engineering wall for normal users).
        historicalSyncStatus.maxLines = 4
        historicalSyncStatus.ellipsize = android.text.TextUtils.TruncateAt.END
        historicalSyncStatus.isClickable = true
        historicalSyncStatus.isFocusable = true
        historicalSyncStatus.setOnClickListener {
            val expanded = historicalSyncStatus.maxLines == Int.MAX_VALUE
            historicalSyncStatus.maxLines = if (expanded) 4 else Int.MAX_VALUE
            historicalSyncStatus.ellipsize = if (expanded) android.text.TextUtils.TruncateAt.END else null
        }
        androidx.core.view.ViewCompat.setAccessibilityDelegate(historicalSyncStatus, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: android.view.View, info: androidx.core.view.accessibility.AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK, getString(R.string.main_hist_status_toggle)))
            }
        })
        // Modern device overview FIRST (connection, battery, facts); the technical download block follows (#159, #172).
        val deviceOverview = DeviceOverviewView(this)
        deviceView(deviceOverview, 8)
        deviceOverviewRef = deviceOverview
        // #204 phase B: discreet hint for not-yet-decoded FIT fields (only when the stored inventory has entries).
        val unknownFieldsView = UnknownFieldsView(this)
        deviceView(unknownFieldsView, 0)
        fun refreshUnknownFields() {
            Thread {
                val summary = try {
                    CirqaDatabase(applicationContext).use { UnknownFieldsSummary.summarize(Strings.of(this), it.storedFitUnmappedFields()) }
                } catch (_: Exception) { UnknownFieldsSummary.summarize(Strings.of(this), emptyList()) }
                runOnUiThread {
                    if (destroyed) return@runOnUiThread
                    unknownFieldsView.show(summary)
                    (unknownFieldsView.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = if (summary.isEmpty) 0 else dp(8)
                }
            }.start()
        }
        unknownFieldsView.onStatusChanged = { refreshUnknownFields() }
        deviceView(text(getString(R.string.main_hist_heading), 17f, ink, true), 16)
        deviceView(text(
            getString(R.string.main_hist_info),
            13f, muted
        ), 4)
        deviceView(actionButton(getString(R.string.main_hist_button), true, PlIcon.HISTORY) {
            if (!HistorySyncExclusion.canStartManual(automaticHistoricalSyncActive)) {
                historicalSyncStatus.text = getString(R.string.main_auto_sync_running)
                return@actionButton
            }
            if (!ensureSyncPermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.main_hist_confirm_title)
                .setMessage(
                    getString(R.string.main_hist_confirm_message)
                )
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.main_hist_confirm_ok) { _, _ ->
                    // The dialog may have been open while an automatic run started: never close/replace it.
                    if (!HistorySyncExclusion.canStartManual(automaticHistoricalSyncActive)) {
                        historicalSyncStatus.text = getString(R.string.main_auto_sync_running)
                        return@setPositiveButton
                    }
                    manualHistoricalSyncActive = true
                    historicalSyncStatus.text = getString(R.string.main_hist_running)
                    syncProgress.show(HistorySyncProgress(HistorySyncStage.CONNECTING))
                    val prefs = getSharedPreferences("historical_sync_energy_policy", MODE_PRIVATE)
                    prefs.edit().putLong("last_attempt", System.currentTimeMillis()).commit()
                    // The user's explicit download wins over a running battery/metadata read (it holds the BLE gate).
                    deviceMetadataProbe?.close()
                    deviceMetadataProbe = null
                    runHistoricalDownload(progress = { progress ->
                        if (!destroyed) syncProgress.show(progress)
                    }) { message ->
                        runOnUiThread {
                            manualHistoricalSyncActive = false
                            recordHistoricalSyncResult(prefs, message) // also after the screen closed (abort in onDestroy)
                            if (!destroyed) {
                                historicalSyncStatus.text = SyncReportSummary.display(Strings.of(this@MainActivity), SyncReportStore.forStorage(message))
                                syncProgress.show(HistorySyncProgress.fromReport(message))
                                historyRefresh?.invoke(); refreshDailyFreshness?.invoke()
                            }
                        }
                    }
                }
                .show()
        }, 4)
        deviceView(syncProgress, 4)
        // #637: how current is the band's daily history (stress/SpO2/breathing), independent of the last report.
        val dailyFreshness = text(getString(R.string.daily_fresh_none), 13f, muted).apply {
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        deviceView(dailyFreshness, 4)
        refreshDailyFreshness = {
            Thread {
                val latest = DailyHistoryFreshness.latest(applicationContext)
                runOnUiThread {
                    if (!destroyed) dailyFreshness.text = DailyHistoryFreshness.text(Strings.of(this), latest,
                        System.currentTimeMillis(), java.time.ZoneId.systemDefault(), resources.configuration.locales[0])
                }
            }.start()
        }
        refreshDailyFreshness?.invoke()
        deviceView(historicalSyncStatus, 4)
        // #737: activity state from the last download, with Connect's "end and save" behind a confirmation.
        val bandActivityText = text(getString(R.string.band_activity_running), 14f, ink, true).apply {
            setPadding(dp(16), dp(12), dp(16), dp(4))
            tag = "band_activity_state"
        }
        val endActivityButton = actionButton(getString(R.string.band_activity_end_button), true, PlIcon.HISTORY) {
            if (!HistorySyncExclusion.canStartManual(automaticHistoricalSyncActive) || manualHistoricalSyncActive) {
                historicalSyncStatus.text = getString(R.string.main_auto_sync_running)
                return@actionButton
            }
            if (!ensureSyncPermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.band_activity_end_title)
                .setMessage(R.string.band_activity_end_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.band_activity_end_ok) { _, _ ->
                    if (!HistorySyncExclusion.canStartManual(automaticHistoricalSyncActive)) return@setPositiveButton
                    manualHistoricalSyncActive = true
                    historicalSyncStatus.text = getString(R.string.band_activity_ending)
                    syncProgress.show(HistorySyncProgress(HistorySyncStage.CONNECTING))
                    val prefs = getSharedPreferences("historical_sync_energy_policy", MODE_PRIVATE)
                    prefs.edit().putLong("last_attempt", System.currentTimeMillis()).commit()
                    deviceMetadataProbe?.close()
                    deviceMetadataProbe = null
                    runHistoricalDownload(progress = { progress -> if (!destroyed) syncProgress.show(progress) }, endActivity = true) { message ->
                        runOnUiThread {
                            manualHistoricalSyncActive = false
                            recordHistoricalSyncResult(prefs, message)
                            if (!destroyed) {
                                historicalSyncStatus.text = SyncReportSummary.display(Strings.of(this@MainActivity), SyncReportStore.forStorage(message))
                                syncProgress.show(HistorySyncProgress.fromReport(message))
                                historyRefresh?.invoke(); refreshDailyFreshness?.invoke()
                            }
                        }
                    }
                }
                .show()
        }
        deviceView(bandActivityText, 4)
        deviceView(endActivityButton, 4)
        refreshBandActivity = {
            val active = BandActivityStore.isActive(getSharedPreferences(BandActivityStore.PREFS, MODE_PRIVATE), System.currentTimeMillis())
            bandActivityText.visibility = if (active) android.view.View.VISIBLE else android.view.View.GONE
            endActivityButton.visibility = bandActivityText.visibility
        }
        refreshBandActivity?.invoke()
        cirqaCard = text("CIRQA Smart Band", 15f, ink, true).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(palette.card)
        }
        debugView(cirqaCard)
        cirqaEvidenceView = text(cirqaScanEvidence.summary(), 13f, muted).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(palette.card)
        }
        debugView(cirqaEvidenceView)
        // The foreground service keeps a CIRQA notification connection open and reconnects after disconnects.
        val recorderState = text(getString(R.string.main_rec_off), 15f, muted, true)
        fun setRecorderState(message: String, icon: PlIcon) {
            recorderState.text = message
            recorderState.setCompoundDrawablesRelative(PlIcon.drawable(this, icon, 20, muted), null, null, null)
            recorderState.compoundDrawablePadding = dp(10)
        }
        setRecorderState(getString(R.string.main_rec_off), PlIcon.RECORD_STOP)
        deviceView(recorderState, 8)
        val recorderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val playButton = actionButton(getString(R.string.main_rec_start), true, PlIcon.RECORD_START) { /* Click handler installed below. */ }
        val stopButton = actionButton(getString(R.string.main_rec_stop), false, PlIcon.RECORD_STOP) {
            try {
                startService(Intent(this, CirqaContinuousRecordingService::class.java)
                    .setAction(CirqaContinuousRecordingService.ACTION_STOP))
                setRecorderState(getString(R.string.main_rec_stopping), PlIcon.HOURGLASS)
            } catch (_: Exception) {
                output.text = getString(R.string.main_rec_stop_failed)
            }
        }
        playButton.setOnClickListener {
            if (!ensurePermissions()) return@setOnClickListener
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.main_rec_confirm_title)
                .setMessage(R.string.main_rec_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.main_rec_start) { _, _ ->
                    // Android 13+: without this permission the ongoing notification (with its stop action) stays hidden.
                    if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
                    }
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(
                            this@MainActivity,
                            Intent(this@MainActivity, CirqaContinuousRecordingService::class.java)
                                .setAction(CirqaContinuousRecordingService.ACTION_START)
                        )
                        setRecorderState(getString(R.string.main_rec_starting), PlIcon.HOURGLASS)
                    } catch (_: Exception) {
                        output.text = getString(R.string.main_rec_start_failed)
                    }
                }.show()
        }
        recorderRow.addView(playButton, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        recorderRow.addView(stopButton, LinearLayout.LayoutParams(0, -2, 1f))
        deviceView(recorderRow, 12)
        addPositionTrackSection(::deviceView, ::text, ::actionButton)
        var shownRecorderRunning: Boolean? = null
        recorderRefresh = refresh@{
            val running = RecorderStatus.isRunning(getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE))
            // Polled once a second: touching the views only on a real change keeps the screen quiet for TalkBack and uiautomator.
            if (running == shownRecorderRunning) return@refresh
            shownRecorderRunning = running
            if (running) setRecorderState(getString(R.string.main_rec_on), PlIcon.RECORD_START) else setRecorderState(getString(R.string.main_rec_off), PlIcon.RECORD_STOP)
            recorderState.setTextColor(muted)
            playButton.alpha = if (running) 0.45f else 1f
            stopButton.alpha = if (running) 1f else 0.45f
            playButton.isEnabled = !running
            stopButton.isEnabled = running
        }
        recorderRefresh?.invoke()
        val disabledFileSyncV2 = actionButton("CIRQA: FileSync V2 dauerhaft gesperrt", false) {}
        disabledFileSyncV2.isEnabled = false
        disabledFileSyncV2.isClickable = false
        disabledFileSyncV2.isFocusable = false
        disabledFileSyncV2.alpha = 0.55f
        disabledFileSyncV2.contentDescription =
            "FileSync V2 gesperrt; CIRQA hat die frühere Anfrage abgelehnt. Keine Anfrage wird gesendet."
        debugView(disabledFileSyncV2)
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        debugView(devices)
        output = text("Keine Geräte verbunden.", 13f, muted).apply {
            setTextIsSelectable(true)
        }
        debugView(output)
        val nav = MainNavigationView(this) { selectMainTab(it) }
        selectMainTab = { index ->
            selectedMainTab = index
            nav.select(index)
            dashboardVisible = index == 0
            historyVisible = index == 1
            devicesVisible = index == 2
            if (dashboardVisible) {
                dashboardLastEventId = -1L
                dashboardLastRefreshAt = 0L
            }
            overview.forEach { it.visibility = if (index == 0) android.view.View.VISIBLE else android.view.View.GONE }
            historyViews.forEach { it.visibility = if (index == 1) android.view.View.VISIBLE else android.view.View.GONE }
            analysisViews.forEach { it.visibility = if (index == MainNavigationView.ANALYSEN) android.view.View.VISIBLE else android.view.View.GONE }
            deviceViews.forEach {
                val shown = index == 2 && (debugDevicesVisible || it !in debugDeviceViews)
                if (it is TabAwareVisibility) it.tabShown = shown
                else it.visibility = if (shown) android.view.View.VISIBLE else android.view.View.GONE
            }
            if (index == 1) {
                dashboardLastEventId = -1L
                dashboardLastRefreshAt = 0L
                historyRefresh?.invoke(); refreshDailyFreshness?.invoke()
            }
            if (index == 2) recorderRefresh?.invoke()
            if (index == 2) refreshUnknownFields()
            if (index == 2) maybeProbeDeviceMetadata()
            connectionIndicatorRefresh?.invoke()
            scroll.scrollTo(0, 0)
            updateTitleBar(0)
        }
        screen.addView(nav)
        setContentView(screen)
        scheduleUnknownFitInventoryScan()
        // #208: system bar icons must stay readable in both themes (light bars need dark icons).
        androidx.core.view.WindowCompat.getInsetsController(window, screen).apply {
            isAppearanceLightStatusBars = !palette.dark
            isAppearanceLightNavigationBars = !palette.dark
        }
        selectMainTab(savedInstanceState?.getInt("main_tab") ?: intent.getIntExtra(MainNavigationView.EXTRA_TAB, 0))
        requestBluetoothPermissionOnFirstLaunch()
        if (savedInstanceState == null) autoStartRecordingIfEnabled()
    }

    /** Opt-in from the settings: start the recording service on a fresh launch if everything is already in place. */
    private fun autoStartRecordingIfEnabled() {
        val granted = permissions().all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        // A fresh heartbeat of a killed process must not block the start (#212): require the service to be alive too.
        val running = AutoStartRecording.isAlreadyRunning(
            RecorderStatus.isRunning(getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE)),
            AutoStartRecording.isServiceAlive(this)
        )
        val btOn = try { adapter.isEnabled } catch (_: Exception) { false }
        if (!AutoStartRecording.shouldStart(AutoStartRecording.isEnabled(this), true, granted, btOn, running)) return
        try {
            androidx.core.content.ContextCompat.startForegroundService(this,
                Intent(this, CirqaContinuousRecordingService::class.java).setAction(CirqaContinuousRecordingService.ACTION_START))
        } catch (e: Exception) {
            // Android may refuse (background restrictions); say so instead of failing silently.
            android.widget.Toast.makeText(this, getString(R.string.main_rec_autostart_failed), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Touch-based long press with a custom duration (View.setOnLongClickListener cannot be configured). Cancels on release,
     * on leaving the touch slop and on cancel. Adds a haptic tick when it fires and a TalkBack custom action [actionLabel].
     */
    private fun attachHiddenLongPress(view: android.view.View, durationMs: Long, actionLabel: String, action: () -> Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val fire = Runnable {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            action()
        }
        val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f; var downY = 0f
        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; handler.postDelayed(fire, durationMs) }
                android.view.MotionEvent.ACTION_MOVE ->
                    if (kotlin.math.abs(e.x - downX) > slop || kotlin.math.abs(e.y - downY) > slop) handler.removeCallbacks(fire)
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(fire)
            }
            true
        }
        androidx.core.view.ViewCompat.addAccessibilityAction(view, actionLabel) { _, _ -> action(); true }
    }

    private var selectedMainTab = 0
    private lateinit var selectMainTab: (Int) -> Unit

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("main_tab", selectedMainTab)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra(MainNavigationView.EXTRA_TAB)) {
            selectMainTab(intent.getIntExtra(MainNavigationView.EXTRA_TAB, 0))
        }
    }

    private fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun ensureSyncPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val missing = required.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            status.text = getString(R.string.main_bt_permission_needed)
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 12)
            return false
        }
        return ensureBluetoothEnabled()
    }

    /** Only called by Activity resume/start. Never prompts for permissions or schedules background work. */
    private fun maybeStartAutomaticHistoricalSync() {
        val statusView = historicalSyncStatusView ?: return
        if (!HistorySyncExclusion.canStartAutomatic(manualHistoricalSyncActive) || automaticHistoricalSyncActive || destroyed ||
            ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT
                else Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            !::adapter.isInitialized || !adapter.isEnabled ||
            // a running recording is paused for the download (see pauseRecordingForSync); any other session blocks
            (CirqaBleSessionGate.isBusy() && !AutoStartRecording.isServiceAlive(this))
        ) return
        val prefs = getSharedPreferences("historical_sync_energy_policy", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastAttempt = prefs.getLong("last_attempt", 0L)
        val lastSuccess = prefs.getLong("last_success", 0L)
        if (!HistoricalSyncPolicy.isDue(now, lastAttempt, lastSuccess)) return
        val targetCount = try {
            adapter.bondedDevices.orEmpty().count { it.name?.contains("CIRQA", ignoreCase = true) == true }
        } catch (_: SecurityException) { return }
        if (targetCount != 1) return

        if (!prefs.edit().putLong("last_attempt", now).commit()) return
        automaticHistoricalSyncActive = true
        statusView.text = getString(R.string.main_auto_sync_running)
        syncProgressView?.show(HistorySyncProgress(HistorySyncStage.CONNECTING))
        runHistoricalDownload(progress = { progress ->
            if (!destroyed) syncProgressView?.show(progress)
        }) { message ->
            runOnUiThread {
                automaticHistoricalSyncActive = false
                // Persist success only for a completely drained run; partial/error runs remain retryable
                // after the five-minute attempt cooldown. Recorded even after the screen closed (abort in onDestroy).
                recordHistoricalSyncResult(prefs, message)
                if (destroyed) return@runOnUiThread
                statusView.text = SyncReportSummary.display(Strings.of(this), SyncReportStore.forStorage(message))
                syncProgressView?.show(HistorySyncProgress.fromReport(message))
                historyRefresh?.invoke(); refreshDailyFreshness?.invoke()
            }
        }
    }

    /**
     * One history download; a failure at connection setup before the band accepted the sync (GATT 133, CCCD error) is
     * repeated once after a pause (#637, [HistorySyncRetryPolicy]). [onResult] gets only the final report.
     */
    private fun runHistoricalDownload(
        progress: (HistorySyncProgress) -> Unit,
        attempt: Int = 1,
        endActivity: Boolean = false,
        recordingPaused: Boolean = false,
        onResult: (String) -> Unit
    ) {
        pauseRecordingForSync(recordingPaused) { paused ->
        val probe = CirqaMlHandshakeProbe(this, adapter)
        historicalDownloadProbe?.close()
        historicalDownloadProbe = probe
        probe.startHistoricalDownload(userConfirmed = true, progress = progress, endActivity = endActivity) { message ->
            BandActivityStore.record(getSharedPreferences(BandActivityStore.PREFS, MODE_PRIVATE), message, System.currentTimeMillis())
            runOnUiThread { if (!destroyed) { refreshBandActivity?.invoke(); dashboardRefresh?.invoke() } }
            val delay = HistorySyncRetryPolicy.delayFor(message, attempt)
            // A quick retry (8 s) keeps the recording paused; before a long wait or at the end it runs again.
            val keepPaused = delay != null && delay <= HistorySyncRetryPolicy.DELAY_MS
            if (paused && !keepPaused) runOnUiThread { restartRecordingAfterSync() }
            if (delay != null) {
                // the waiting report is shown meanwhile, so a busy band does not look like a hang
                if (delay > HistorySyncRetryPolicy.DELAY_MS) onResult(message)
                handler.postDelayed({
                    if (destroyed) { onResult(message); if (paused && keepPaused) restartRecordingAfterSync() }
                    else runHistoricalDownload(progress, attempt + 1, recordingPaused = paused && keepPaused, onResult = onResult)
                }, delay)
            } else {
                onResult(message)
            }
        }
        }
    }

    /**
     * The recording service and a history download cannot share the band (one BLE session). If the recording runs, it is
     * stopped, the download runs once the session is free, and the caller starts it again afterwards; [block] learns
     * whether the recording was paused.
     */
    /** #788: opt-in position track of an activity started in PulsLibre (own location service, export as GeoJSON). */
    private fun addPositionTrackSection(
        deviceView: (android.view.View, Int) -> Unit,
        label: (String, Float, Int, Boolean) -> TextView,
        actionButton: (String, Boolean, PlIcon?, () -> Unit) -> android.view.View
    ) {
        val palette = AppPalette.of(this)
        val ink = palette.ink
        val muted = palette.muted
        deviceView(label(getString(R.string.pos_heading), 17f, ink, true), 16)
        val toggle = androidx.appcompat.widget.SwitchCompat(this).apply {
            text = getString(R.string.pos_switch)
            setTextColor(ink)
            isChecked = de.pulselibre.positiontracking.PositionTracking.isEnabled(this@MainActivity)
        }
        deviceView(toggle, 4)
        deviceView(label(getString(R.string.pos_info), 13f, muted, false), 4)
        val state = label("", 13f, ink, true)
        deviceView(state, 4)
        val startStop = actionButton(getString(R.string.pos_start), true, PlIcon.RECORD_START) {
            if (de.pulselibre.positiontracking.PositionTracking.activeSession(this) != null) stopPositionTrack()
            else if (hasLocationPermission()) startPositionTrack()
            else {
                startTrackAfterPermission = true
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 13)
            }
        }
        deviceView(startStop, 4)
        val export = actionButton(getString(R.string.pos_export), false, null) {
            val last = try {
                de.pulselibre.positiontracking.PositionTrackStore(this).use { it.sessions().firstOrNull { s -> s.endMs != null } }
            } catch (_: Exception) { null }
            if (last == null) {
                android.widget.Toast.makeText(this, getString(R.string.pos_export_none), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                exportTrackId = last.id
                val name = "pulslibre_spur_" + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
                    .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(last.startMs)) + ".geojson"
                exportTrack.launch(name)
            }
        }
        deviceView(export, 12)
        refreshPositionTrack = {
            val enabled = de.pulselibre.positiontracking.PositionTracking.isEnabled(this)
            val active = de.pulselibre.positiontracking.PositionTracking.activeSession(this)
            startStop.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
            export.visibility = startStop.visibility
            state.visibility = startStop.visibility
            (startStop as? TextView)?.text = getString(if (active != null) R.string.pos_stop else R.string.pos_start)
            state.text = if (active == null) getString(R.string.pos_state_off) else {
                val n = try { de.pulselibre.positiontracking.PositionTrackStore(this).use { it.pointCount(active) } } catch (_: Exception) { 0 }
                getString(R.string.pos_state_on, n)
            }
        }
        toggle.setOnCheckedChangeListener { _, on ->
            de.pulselibre.positiontracking.PositionTracking.setEnabled(this, on)
            if (!on) stopPositionTrack()
            refreshPositionTrack?.invoke()
        }
        refreshPositionTrack?.invoke()
    }

    private fun hasLocationPermission() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startPositionTrack() {
        try {
            androidx.core.content.ContextCompat.startForegroundService(this,
                Intent(this, de.pulselibre.positiontracking.PositionTrackingService::class.java)
                    .setAction(de.pulselibre.positiontracking.PositionTrackingService.ACTION_START))
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, getString(R.string.pos_start_failed), android.widget.Toast.LENGTH_LONG).show()
        }
        handler.postDelayed({ refreshPositionTrack?.invoke() }, 1000L)
    }

    private fun stopPositionTrack() {
        if (de.pulselibre.positiontracking.PositionTracking.activeSession(this) == null) return
        try {
            startService(Intent(this, de.pulselibre.positiontracking.PositionTrackingService::class.java)
                .setAction(de.pulselibre.positiontracking.PositionTrackingService.ACTION_STOP))
        } catch (_: Exception) {}
        handler.postDelayed({ refreshPositionTrack?.invoke() }, 1000L)
    }

    private fun writeTrackExport(uri: android.net.Uri) {
        val id = exportTrackId ?: return
        Thread {
            val ok = try {
                val json = de.pulselibre.positiontracking.PositionTrackStore(this).use { store ->
                    val session = store.sessions().first { it.id == id }
                    de.pulselibre.positiontracking.PositionGeoJson.export(session, store.points(id))
                }
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
            } catch (_: Exception) { false }
            runOnUiThread {
                android.widget.Toast.makeText(this, getString(if (ok) R.string.pos_export_done else R.string.pos_export_failed), android.widget.Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun pauseRecordingForSync(alreadyPaused: Boolean, block: (paused: Boolean) -> Unit) {
        if (alreadyPaused) { block(true); return }
        if (!AutoStartRecording.isServiceAlive(this)) { block(false); return }
        try {
            startService(Intent(this, CirqaContinuousRecordingService::class.java).setAction(CirqaContinuousRecordingService.ACTION_STOP))
        } catch (_: Exception) { block(false); return }
        historicalSyncStatusView?.text = getString(R.string.main_hist_recording_paused)
        val deadline = android.os.SystemClock.elapsedRealtime() + RECORDING_PAUSE_TIMEOUT_MS
        fun poll() {
            val free = !CirqaBleSessionGate.isBusy() && !AutoStartRecording.isServiceAlive(this)
            if (free || android.os.SystemClock.elapsedRealtime() > deadline) {
                // short pause so the band sees the recording link closed before the sync connects
                handler.postDelayed({ block(true) }, RECORDING_SETTLE_MS)
            } else handler.postDelayed({ poll() }, 500L)
        }
        poll()
    }

    private fun restartRecordingAfterSync() {
        if (AutoStartRecording.isServiceAlive(this)) return
        try {
            androidx.core.content.ContextCompat.startForegroundService(this,
                Intent(this, CirqaContinuousRecordingService::class.java).setAction(CirqaContinuousRecordingService.ACTION_START))
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, getString(R.string.main_rec_autostart_failed), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun recordHistoricalSyncResult(
        preferences: android.content.SharedPreferences,
        report: String
    ) {
        // #637: keep the diagnosis (terminal state, pull results) readable instead of cutting it at 4000 characters.
        preferences.edit().putString("last_report", SyncReportStore.forStorage(report))
            .putLong("last_report_at", System.currentTimeMillis()).apply()
        if (report.contains("runDrained=true") && report.contains("incompleteReason=none")) {
            preferences.edit().putLong("last_success", System.currentTimeMillis()).apply()
        }
    }

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
        val message = getString(R.string.main_bt_off_msg)
        status.text = message
        if (::output.isInitialized) output.text = message
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.main_bt_off_title)
            .setMessage(message)
            .setNegativeButton(R.string.main_later, null)
            .setPositiveButton(R.string.main_bt_enable) { _, _ ->
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.main_bt_enable_toast, Toast.LENGTH_LONG).show()
                }
            }
            .show()
        return false
    }

    /**
     * #142/#159: one bounded, read-only metadata query per visit to the device tab. GATT battery
     * and device information are read first; the Garmin `GDIDeviceStatus` query runs only as a
     * fallback. Every observed value is persisted with its source and timestamp. No device writes
     * beyond the approved status query, no pairing, reset, or CLOSE_ALL.
     */
    private fun maybeProbeDeviceMetadata() {
        val now = System.currentTimeMillis()
        if (now - lastDeviceMetadataVisitAt < 10_000L) return // ignore rapid tab re-taps
        lastDeviceMetadataVisitAt = now
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED) return
        if (!::adapter.isInitialized || !adapter.isEnabled) return
        // Never a second GATT client while a history download or the recorder owns the band (it would disturb that run).
        if (!DeviceMetadataStartPolicy.mayStart(CirqaBleSessionGate.isBusy(),
                automaticHistoricalSyncActive || manualHistoricalSyncActive)) return
        deviceMetadataProbe?.close()
        val probe = DeviceMetadataProbe(applicationContext, adapter)
        deviceMetadataProbe = probe
        probe.start { result ->
            runOnUiThread {
                if (destroyed) return@runOnUiThread
                deviceMetadataProbe = null
                if (result.values.isNotEmpty()) {
                    dashboardWorker.execute {
                        try { pollDatabase.storeLiveDeviceMetadata(result.values) } catch (_: Exception) {}
                        runOnUiThread {
                            if (destroyed) return@runOnUiThread
                            deviceStatsLoadedAt = 0L // force the next refresh to reload from the store
                            connectionIndicatorRefresh?.invoke()
                        }
                    }
                }
                if (::status.isInitialized) status.text = result.summary
            }
        }
    }

    private fun ensurePermissions(): Boolean {
        val missing = permissions().filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            status.text = getString(R.string.main_perm_needed)
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
            return false
        }
        return ensureBluetoothEnabled()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            // #788: coarse-only ("ungefähr") is accepted as well; the track then holds coarse positions.
            val any = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (any && startTrackAfterPermission) startPositionTrack()
            else if (!any) android.widget.Toast.makeText(this, getString(R.string.pos_permission_denied), android.widget.Toast.LENGTH_LONG).show()
            startTrackAfterPermission = false
            refreshPositionTrack?.invoke()
        }
        if (requestCode == 11) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!granted && ::output.isInitialized) {
                output.text = getString(R.string.main_notif_off)
            }
        }
        if (requestCode == 10 && ::status.isInitialized) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            status.text = getString(if (granted) R.string.main_perm_granted else R.string.main_perm_denied)
            connectionIndicatorRefresh?.invoke()
        }
        if (requestCode == 12 && ::status.isInitialized) {
            val granted = grantResults.isNotEmpty() && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }
            status.text = getString(if (granted) R.string.main_sync_perm_granted else R.string.main_sync_perm_denied)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!ensurePermissions()) return
        if (!adapter.isEnabled) { status.text = getString(R.string.main_bt_off_msg); return }
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
        status.text = getString(R.string.main_scan_running)
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
        status.text = getString(R.string.main_scan_finished, found.size)
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
                    status.text = getString(R.string.main_scan_failed, errorCode)
                }
            }
        }
    }

    /** Observes Android's system GATT state for display and diagnostics. */
    @SuppressLint("MissingPermission")
    private fun refreshCirqaCard(): CirqaConnectionHistory.State {
        val permissionGranted = Build.VERSION.SDK_INT < 31 ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        cirqaBondAddress = null
        val state = if (!permissionGranted) {
            CirqaConnectionHistory.State.PERMISSION_DENIED
        } else try {
            val matched = adapter.bondedDevices.orEmpty().firstOrNull {
                it.name?.contains("CIRQA", ignoreCase = true) == true
            }
            cirqaBondAddress = matched?.address
            when {
                matched == null -> CirqaConnectionHistory.State.NOT_PAIRED
                !adapter.isEnabled -> CirqaConnectionHistory.State.DISCONNECTED
                else -> {
                    val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                    CirqaStateMapping.fromAndroidState(manager.getConnectionState(matched, BluetoothProfile.GATT))
                }
            }
        } catch (_: SecurityException) {
            cirqaBondAddress = null
            CirqaConnectionHistory.State.PERMISSION_DENIED
        } catch (_: IllegalArgumentException) {
            CirqaConnectionHistory.State.UNKNOWN
        }
        cirqaGattState = CirqaStateMapping.label(Strings.of(this), state)
        val prefs = getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE)
        val link = CirqaLinkStatus.evaluate(
            Strings.of(this), RecorderStatus.isRunning(prefs), state,
            prefs.getLong(CirqaContinuousRecordingService.KEY_LAST_READING_MS, -1L).takeIf { it >= 0 },
            System.currentTimeMillis()
        )
        cirqaLink = link
        cirqaConnectionHistory.observe(System.currentTimeMillis(), state)
        val message = buildString {
            appendLine("CIRQA Smart Band")
            if (cirqaBondAddress != null) {
                appendLine(getString(R.string.main_card_bonded))
                if (cirqaScanMatch) appendLine(getString(R.string.main_card_scan_match))
            }
            appendLine(cirqaGattState)
            append(link.label)
        }
        if (cirqaCard.text.toString() != message) cirqaCard.text = message
        return state
    }



    /** Weight quick edit (#118): tap on the weight value; − / + change in 0.1 kg steps, cancel changes nothing. */
    private fun showQuickWeight(onSaved: () -> Unit) {
        val p = getSharedPreferences(SpO2DisplaySettings.STORE, MODE_PRIVATE)
        val d = resources.displayMetrics.density
        val palette = AppPalette.of(this)
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            gravity = android.view.Gravity.CENTER
            textSize = 26f
            setText(p.getString("weight_kg", "").orEmpty().replace('.', ','))
            setSelectAllOnFocus(true)
            hint = "kg"
            contentDescription = getString(R.string.main_weight_input_cd)
        }
        fun stepButton(label: String, delta: Double, description: String) = TextView(this).apply {
            text = label; textSize = 26f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            background = GradientDrawable().apply { setColor(DesignTokens.BRAND_PURPLE); cornerRadius = 14 * d }
            contentDescription = description
            isClickable = true; isFocusable = true
            setOnClickListener { input.setText(WeightStepper.format(WeightStepper.step(input.text.toString(), delta), resources.configuration.locales[0])); input.setSelection(input.text.length) }
            // press-and-hold repeats the step
            val repeat = object : Runnable {
                override fun run() { performClick(); postDelayed(this, 120L) }
            }
            setOnLongClickListener { post(repeat); true }
            setOnTouchListener { v, e ->
                if (e.actionMasked == android.view.MotionEvent.ACTION_UP || e.actionMasked == android.view.MotionEvent.ACTION_CANCEL) v.removeCallbacks(repeat)
                false
            }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        row.addView(stepButton("−", -WeightStepper.STEP_KG, getString(R.string.main_weight_minus_cd)).apply { tag = "weight_minus" },
            LinearLayout.LayoutParams((56 * d).toInt(), (56 * d).toInt()))
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = (8 * d).toInt(); marginEnd = (8 * d).toInt() })
        row.addView(stepButton("+", WeightStepper.STEP_KG, getString(R.string.main_weight_plus_cd)).apply { tag = "weight_plus" },
            LinearLayout.LayoutParams((56 * d).toInt(), (56 * d).toInt()))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * d).toInt(), (8 * d).toInt(), (20 * d).toInt(), 0)
            addView(row, LinearLayout.LayoutParams(-1, -2))
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.main_weight_step); textSize = 12f; setTextColor(palette.muted)
                gravity = android.view.Gravity.CENTER; setPadding(0, (8 * d).toInt(), 0, 0)
            })
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.main_weight_title)
            .setView(box)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = WeightStepper.parse(input.text.toString())
                if (value == null || value !in WeightStepper.MIN_KG..WeightStepper.MAX_KG) {
                    Toast.makeText(this, R.string.main_weight_invalid, Toast.LENGTH_LONG).show()
                } else {
                    // stored with one decimal and a dot, like the settings field
                    p.edit().putString("weight_kg", String.format(java.util.Locale.ROOT, "%.1f", value).removeSuffix(".0")).apply()
                    onSaved()
                    dialog.dismiss()
                }
            }
            input.requestFocus()
        }
        DialogProbe.track(dialog).show()
    }

    private fun showBmiInfo(bmi: Bmi.Assessment) {
        val text = buildString {
            append(getString(R.string.bmi_info_body))
            bmi.note?.let { append("\n\n").append(getString(R.string.bmi_info_note, DomainText.tr(this@MainActivity, it))) }
        }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.bmi_info_title).setMessage(text)
            .setPositiveButton(R.string.action_close, null).show()
    }

    override fun onDestroy() {
        dashboardWorker.shutdownNow()
        stopDashboardPolling()
        runCatching { pollDatabase.close() }
        dashboardRefresh = null
        connectionIndicatorRefresh = null
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        stopScan()
        // End a running download through its regular terminal path: report "incomplete", release the partial file,
        // close GATT; recordHistoricalSyncResult still persists the report although this screen is gone.
        historicalDownloadProbe?.abort("Download abgebrochen: Ansicht geschlossen.") // German diagnostic report, like all sync reports
        historicalDownloadProbe = null
        deviceMetadataProbe?.close()
        deviceMetadataProbe = null
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
        val details = (if (isCirqaMatch) getString(R.string.main_row_match) + "\n" else "") +
            "$description\n" + getString(R.string.main_row_service, serviceData)
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
            setTextColor(AppPalette.of(this@MainActivity).ink)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(AppPalette.of(this@MainActivity).card)
                cornerRadius = dp(16).toFloat()
            }
            contentDescription = details
            isClickable = false
        }
        deviceRows[address] = item
        devices.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }
}

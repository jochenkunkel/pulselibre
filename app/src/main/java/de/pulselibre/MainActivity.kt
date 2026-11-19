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
    private val handler = Handler(Looper.getMainLooper())
    /** One long-lived helper for the 1-second change poll instead of opening/closing the database every second (#114). */
    private val pollDatabase by lazy { CirqaDatabase(applicationContext) }
    private var dashboardRefresh: (() -> Unit)? = null
    private var connectionIndicatorRefresh: (() -> Unit)? = null
    private var cirqaLink = CirqaLinkStatus.Result(CirqaLinkStatus.Kind.UNAVAILABLE, false, "")
    private var historyRefresh: (() -> Unit)? = null
    private var deviceOverviewRef: DeviceOverviewView? = null
    /** Re-reads the profile into the overview card; also needed after saving via the header settings button. */
    private var refreshBodyCardRef: (() -> Unit)? = null
    private var deviceStatsLoadedAt = 0L
    private var dashboardVisible = true
    private var historyVisible = false
    private var devicesVisible = false
    private var debugDevicesVisible = false
    private var recorderRefresh: (() -> Unit)? = null
    private var dashboardPolling = false
    private var dashboardLastEventId = -1L
    private var dashboardLastRefreshAt = 0L
    private val dashboardWorker = java.util.concurrent.Executors.newSingleThreadExecutor()

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
            if ((dashboardVisible || historyVisible) && !pollInFlight) {
                pollInFlight = true
                dashboardWorker.execute {
                    val latest = try { pollDatabase.latestEventId() }
                        catch (_: android.database.sqlite.SQLiteException) { null }
                    handler.post {
                        pollInFlight = false
                        if (dashboardPolling && !destroyed && latest != null) {
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (latest != dashboardLastEventId || now - dashboardLastRefreshAt >= 30_000L) {
                                dashboardLastEventId = latest
                                dashboardLastRefreshAt = now
                                if (dashboardVisible) dashboardRefresh?.invoke()
                                if (historyVisible) historyRefresh?.invoke()
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
     * Re-index already verified private FIT files on app entry (#204).
     * Scanning and SQLite writes run off the UI thread and do not communicate
     * with the device. The bounded scan is safe to repeat on later launches.
     */
    private fun scheduleUnknownFitInventoryScan() {
        if (dashboardWorker.isShutdown) return
        dashboardWorker.execute {
            try {
                CirqaDatabase(applicationContext).use { database ->
                    CirqaUnknownFitInventory.scanAndPersist(
                        root = java.io.File(applicationContext.filesDir, "verified_fit"),
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
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Extra bottom space shows the mountain ridges of the backdrop (#124).
            setPadding(dp(20), dp(16), dp(20), dp(46))
            minimumHeight = dp(170)
            background = HeaderBackdropDrawable(palette.dark)
        }
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        headerRow.addView(PulseLogoView(this), LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(12) })
        headerRow.addView(text("PulsLibre", 30f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        // Personal Easter egg: the dedicated humerus SVG-matched vector replaces the gear.
        // Keep a 48dp touch target and an explicit accessibility label.
        val avatarButton = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_humerus_settings)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                PersonalSettings.show(this@MainActivity) {
                    refreshBodyCardRef?.invoke()
                    dashboardLastEventId = -1L
                    dashboardLastRefreshAt = 0L
                }
            }
        }
        // #179 variant 4: a real secondary action, not a decorative icon.
        val linkInfoButton = android.widget.FrameLayout(this).apply {
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            isClickable = true
            isFocusable = true
            contentDescription = "Bluetooth-Verbindungsstatus anzeigen"
            background = rounded(palette.tinted, 24)
            addView(PlIcon.view(this@MainActivity, PlIcon.SIGNAL, 23, violet),
                android.widget.FrameLayout.LayoutParams(dp(23), dp(23), android.view.Gravity.CENTER))
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("CIRQA-Verbindung")
                    .setMessage(CirqaLinkStatus.contentDescription(cirqaLink))
                    .setPositiveButton("Schließen", null)
                    .show()
            }
        }
        var lastHeaderStatus: CirqaLinkStatus.Result? = null
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
                        val battery = DeviceBatteryReader.read(applicationContext)
                        runOnUiThread {
                            if (destroyed) return@runOnUiThread
                            if (stats != null) overviewView.showStats(stats)
                            overviewView.showBattery(battery)
                        }
                    }.start()
                }
            }
            avatarButton.contentDescription = "Einstellungen öffnen"
            linkInfoButton.contentDescription = "CIRQA-Verbindungsstatus: ${CirqaLinkStatus.contentDescription(link)}. Details anzeigen"
            if (link.online == lastHeaderStatus?.online) { lastHeaderStatus = link; return }
            lastHeaderStatus = link
            linkInfoButton.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.tinted)
                setStroke(dp(2), if (link.online) Color.rgb(31, 190, 112) else palette.muted)
            }
        }
        connectionIndicatorRefresh = { refreshAvatarConnectionBorder() }
        headerRow.addView(linkInfoButton, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(6) })
        avatarButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(palette.tinted)
            setStroke(dp(2), palette.accent)
        }
        headerRow.addView(avatarButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(headerRow)
        // Large fonts / narrow screens: the subtitle uses the full width (no 60 dp indent) and wraps instead of being cut off.
        // #179/#209: reference variant 4 shows only "Deine Daten. Deine Kontrolle."; the former "· Offline" suffix was removed
        // (it was misread as a connection status). The offline-first promise belongs to Help/Privacy, not to the header.
        val taglineWraps = resources.configuration.screenWidthDp < 340 || resources.configuration.fontScale >= 1.3f
        val taglineRow = text("Deine Daten. Deine Kontrolle.", 13f, Color.parseColor("#E9E1F7")).apply {
            contentDescription = "Deine Daten. Deine Kontrolle."
        }
        header.addView(taglineRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); marginStart = if (taglineWraps) 0 else dp(60) })
        screen.addView(header)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        screen.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        // #179: one shared header, with a compact sticky presentation after scrolling the overview.
        // Keep actions and subtitle in the same views; only layout and background change.
        var compactHeader = false
        fun setCompactHeader(compact: Boolean) {
            if (compactHeader == compact) return
            compactHeader = compact
            header.minimumHeight = if (compact) 0 else dp(170)
            header.setPadding(dp(20), dp(if (compact) 8 else 16), dp(20), dp(if (compact) 8 else 46))
            header.background = if (compact) rounded(palette.background, 0) else HeaderBackdropDrawable(palette.dark)
            taglineRow.layoutParams = (taglineRow.layoutParams as LinearLayout.LayoutParams).apply {
                topMargin = dp(if (compact) 2 else 8)
            }
            header.requestLayout()
        }
        scroll.setOnScrollChangeListener { _, _, y, _, _ ->
            // All tabs share the header. Hysteresis prevents rapid toggling near the top of the scroll range; collapsing is
            // only allowed when enough content remains to scroll afterwards (otherwise shrinking the header would remove the
            // scroll range, the header would expand again and flicker on short tabs such as Gerät or Daten).
            val overflow = (scroll.getChildAt(0)?.height ?: 0) - scroll.height
            if (!compactHeader && y > dp(40) && overflow > dp(220)) setCompactHeader(true)
            else if (compactHeader && y <= dp(12)) setCompactHeader(false)
        }

        val overview = mutableListOf<android.view.View>()
        val historyViews = mutableListOf<android.view.View>()
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
        titleColumn.addView(text("Mein Körperprofil", if (largeText) 16f else 18f, heroText, true))
        titleColumn.addView(text("Tippen zum Bearbeiten", 13f, heroSecondary).apply { setPadding(0, dp(2), 0, 0); maxLines = 2 })
        bodyHeader.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        val healthInfo = android.widget.FrameLayout(this).apply {
            addView(PlIcon.view(this@MainActivity, PlIcon.INFO, 22), android.widget.FrameLayout.LayoutParams(dp(22), dp(22), android.view.Gravity.CENTER))
            contentDescription = "Informationen zu empfangenen Messungen und lokaler Speicherung"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                readDashboard(includeTotals = true) { summary ->
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
        }
        bodyHeader.addView(healthInfo, LinearLayout.LayoutParams(dp(40), dp(40)))
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
            bodyFigure.contentDescription = "Illustration einer Person mit Basecap und violettem Hoodie. Profilwerte sind daneben aufgeführt."
            val heightText = p.getString("height_cm", "").orEmpty()
            val weightText = p.getString("weight_kg", "").orEmpty()
            // Age is derived from the birth date for today; a legacy stored age is only a fallback (#117).
            val age = AgeCalculator.currentAge(p.getString("birth_date", ""), p.getString("age_years", ""), java.time.LocalDate.now())
            val bmi = Bmi.assess(weightText.replace(',', '.').toDoubleOrNull(), heightText.replace(',', '.').toDoubleOrNull(), age)
            val germanOne = java.text.NumberFormat.getNumberInstance(java.util.Locale.GERMANY).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }
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
                    row.minimumHeight = dp(34)
                    if (tagName != null) row.tag = tagName
                    if (description != null) row.contentDescription = description
                }
            }
            add(PlIcon.HEIGHT, "Größe", if (heightText.isBlank()) "–" else "$heightText cm")
            add(PlIcon.WEIGHT, "Gewicht", if (weightText.isBlank()) "–" else "$weightText kg", tagName = "body_weight_row",
                description = "Gewicht ${if (weightText.isBlank()) "nicht angegeben" else "$weightText Kilogramm"}. Tippen zum Ändern.") { showQuickWeight { refreshBodyCard() } }
            add(PlIcon.AGE, "Alter", age?.let { "$it Jahre" } ?: "–")
            // BMI: value + written category (colour is never the only signal); tap explains reference and limits.
            add(PlIcon.BMI, "BMI", bmi.value?.let { germanOne.format(it) + " kg/m²" } ?: "–", heroText,
                extra = bmi.category?.let { "● ${it.label}" } ?: bmi.note) { showBmiInfo(bmi) }
            bodyCard.contentDescription = "Mein Körperprofil. " +
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
            addView(text("Gesundheit · CIRQA", 19f, ink, true))
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
        val cirqaHistory = HistoryOverviewView(this) { range, offset -> CirqaHistoryReader(this).read(range, offset = offset) }
        historyRefresh = { cirqaHistory.refresh() }
        cirqaHistory.refresh()
        historyView(cirqaHistory)
        val deviceHeading = text("Gerät · CIRQA", 22f, ink, true)
        deviceView(deviceHeading)
        val debugDeviceViews = mutableListOf<android.view.View>()
        fun debugView(view: android.view.View, margin: Int = 12) {
            deviceView(view, margin)
            debugDeviceViews.add(view)
        }
        // #172: no visible test lab tab; a 3 s long press on the CIRQA heading opens it (also as TalkBack custom action).
        // Not a security barrier: the lab stays read-only/safe by default. The former inline debug panel is no longer reachable.
        attachHiddenLongPress(deviceHeading, 3000L, "Testlabor öffnen") { DiagnosticsSheet.show(this) }
        debugView(text("Garmin CIRQA · Passive Diagnose ohne Verbindungsaufbau", 13f, muted))
        status = text("Offline · bereit", 14f, muted)
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
        cirqaCard = text("CIRQA Smart Band", 15f, ink, true).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(palette.card)
        }
        // Modern device overview on top (link hero, facts, reception per source, band metadata placeholders).
        val deviceOverview = DeviceOverviewView(this)
        deviceView(deviceOverview, 8)
        deviceOverviewRef = deviceOverview
        // #204 phase B: discreet hint for not-yet-decoded FIT fields (only when the stored inventory has entries).
        val unknownFieldsView = UnknownFieldsView(this)
        deviceView(unknownFieldsView, 0)
        fun refreshUnknownFields() {
            Thread {
                val summary = try {
                    CirqaDatabase(applicationContext).use { UnknownFieldsSummary.summarize(it.storedFitUnmappedFields()) }
                } catch (_: Exception) { UnknownFieldsSummary.summarize(emptyList()) }
                runOnUiThread {
                    if (destroyed) return@runOnUiThread
                    unknownFieldsView.show(summary)
                    (unknownFieldsView.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = if (summary.isEmpty) 0 else dp(8)
                }
            }.start()
        }
        debugView(cirqaCard)
        cirqaEvidenceView = text(cirqaScanEvidence.summary(), 13f, muted).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(palette.card)
        }
        debugView(cirqaEvidenceView)
        // The foreground service keeps a CIRQA notification connection open and reconnects after disconnects.
        val recorderState = text("Aufzeichnung aus", 15f, muted, true)
        fun setRecorderState(message: String, icon: PlIcon) {
            recorderState.text = message
            recorderState.setCompoundDrawablesRelative(PlIcon.drawable(this, icon, 20, muted), null, null, null)
            recorderState.compoundDrawablePadding = dp(10)
        }
        setRecorderState("Aufzeichnung aus", PlIcon.RECORD_STOP)
        deviceView(recorderState, 8)
        val recorderRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val playButton = actionButton("Starten", true, PlIcon.RECORD_START) { /* Click handler installed below. */ }
        val stopButton = actionButton("Stoppen", false, PlIcon.RECORD_STOP) {
            try {
                startService(Intent(this, CirqaContinuousRecordingService::class.java)
                    .setAction(CirqaContinuousRecordingService.ACTION_STOP))
                setRecorderState("CIRQA-Sammlung wird beendet …", PlIcon.HOURGLASS)
            } catch (_: Exception) {
                output.text = "Stopp konnte nicht angefordert werden."
            }
        }
        playButton.setOnClickListener {
            if (!ensurePermissions()) return@setOnClickListener
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("CIRQA-Sammlung starten")
                .setMessage("PulsLibre verbindet sich aktiv per Bluetooth mit dem CIRQA-Armband und empfängt verfügbare Echtzeitmesswerte über eine laufende Verbindung. Bei Verbindungsabbrüchen versucht die App die Verbindung erneut. Die Messwerte werden lokal auf diesem Gerät gespeichert. Es werden keine historischen FIT-Daten geladen. Android zeigt währenddessen eine dauerhafte Benachrichtigung mit Stopp-Taste (Android 13+: Benachrichtigungen müssen erlaubt sein); das System kann den Dienst dennoch beenden. Garmin Connect kann die Bluetooth-Verbindung beeinflussen. Jetzt starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Starten") { _, _ ->
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
                        setRecorderState("CIRQA-Sammlung wird gestartet …", PlIcon.HOURGLASS)
                    } catch (_: Exception) {
                        output.text = "Start fehlgeschlagen. Bluetooth- und Android-Berechtigungen prüfen."
                    }
                }.show()
        }
        recorderRow.addView(playButton, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        recorderRow.addView(stopButton, LinearLayout.LayoutParams(0, -2, 1f))
        deviceView(recorderRow, 12)
        recorderRefresh = {
            val running = RecorderStatus.isRunning(getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE))
            if (running) setRecorderState("Aufzeichnung läuft", PlIcon.RECORD_START) else setRecorderState("Aufzeichnung aus", PlIcon.RECORD_STOP)
            recorderState.setTextColor(muted)
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
                .setMessage("Garmin Connect vorher beenden. PulsLibre verbindet sich kurz mit dem gekoppelten CIRQA, liest nur die GATT-Dienststruktur und trennt die Verbindung wieder. Keine Schreibbefehle, kein CLOSE_ALL. Fortfahren?")
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
        debugView(actionButton("CIRQA: Garmin-Connect-HCI-Mitschnitt vorbereiten", false) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Referenz-Handshake aufzeichnen")
                .setMessage(
                    "1. Android-Einstellungen > Über das Telefon > Buildnummer siebenmal antippen, falls Entwickleroptionen fehlen.\n\n" +
                    "2. Entwickleroptionen öffnen und \"Bluetooth-HCI-Snoop-Protokoll aktivieren\" einschalten. Die Bezeichnung hängt vom Gerät ab.\n\n" +
                    "3. PulsLibre-Aufzeichnung und BLE-Diagnose beenden. Garmin Connect in Android > Apps wieder aktivieren.\n\n" +
                    "4. Garmin Connect starten und CIRQA genau einmal synchronisieren.\n\n" +
                    "5. HCI-Snoop sofort wieder ausschalten; Garmin Connect bei Bedarf deaktivieren.\n\n" +
                    "6. Über Android-Fehlerbericht oder ADB den Bluetooth-HCI-Snoop-Log sichern und lokal in Wireshark auf die Garmin-GATT-Schreibvorgänge prüfen.\n\n" +
                    "WICHTIG: HCI-Logs können Gerätekennungen, Gesundheitsdaten und andere Bluetooth-Verbindungen enthalten. Vor dem Teilen bereinigen. PulsLibre kann die systemgeschützte HCI-Datei nicht selbst auslesen. Keine Kopplung löschen oder zurücksetzen."
                )
                .setNegativeButton("Schließen", null)
                .setPositiveButton("Entwickleroptionen öffnen") { _, _ ->
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    } catch (_: Exception) {
                        Toast.makeText(this, "Entwickleroptionen bitte manuell in Android-Einstellungen öffnen.", Toast.LENGTH_LONG).show()
                    }
                }
                .show()
        })
        debugView(actionButton("CIRQA: 5050-Konfiguration gezielt testen", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Gezielter 5050-Test")
                .setMessage("Experimenteller einmaliger Bluetooth-Test. Garmin Connect und laufende Aufzeichnung vorher beenden. Nach dem bisherigen 5024-ACK und einer empfangenen 5050-Konfiguration sendet PulsLibre genau ein 5050-ACK und eine 15-Byte-Hostmaske ohne gesetzte Fähigkeiten. Das Armband kann die Antwort ablehnen. Keine Dateioperationen vorgesehen; Auswirkungen der experimentellen GATT-Schreibbefehle auf das Band sind nicht verifiziert. Nur nach gesonderter Freigabe am S25 testen. Automatische Trennung spätestens 20 Sekunden ohne Schreibabschluss, sonst 10 Sekunden nach der 5050-Schreibbestätigung. Jetzt starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("5050-Test starten") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startConfigurationTest { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("5050-Konfigurationstest abgeschlossen")
                                    .setMessage(message)
                                    .setPositiveButton("Schließen", null)
                                    .setNeutralButton("Bericht teilen") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(Intent.createChooser(intent, "CIRQA-5050-Test teilen"))
                                    }.show()
                            }
                        }
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: synthetische SYNC-Aushandlung (experimentell)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("CIRQA · Synthetischer SYNC-Debugtest")
                .setMessage("Nur für das freigegebene Testgerät mit synthetischen Daten. Garmin Connect und laufende Aufzeichnung vorher beenden. PulsLibre sendet einmalig 5050-ACK und eine 15-Byte-Hostmaske mit ausschließlich SYNC-Bit 3. Es werden noch KEINE Dateien heruntergeladen oder verändert. Bei Protokollfehlern wird abgebrochen. Dies ist ein experimenteller Bluetooth-Schreibtest. Fortfahren?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("SYNC-Debugtest starten") { _, _ ->
                    if (cirqaMlProbe == null) cirqaMlProbe = CirqaMlHandshakeProbe(this, adapter)
                    lastHistoryDiagnostic = null
                    cirqaMlProbe?.startSyntheticSyncDebug { message ->
                        runOnUiThread {
                            output.text = message
                            if (message.startsWith("PulsLibre · CIRQA Historien-Diagnose")) {
                                lastHistoryDiagnostic = message
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Synthetischer SYNC-Debugtest abgeschlossen")
                                    .setMessage(message)
                                    .setPositiveButton("Schließen", null)
                                    .setNeutralButton("Bericht teilen") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        }
                                        startActivity(Intent.createChooser(intent, "CIRQA-SYNC-Debugbericht teilen"))
                                    }.show()
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
        val disabledFileSyncV2 = actionButton("CIRQA: FileSync V2 dauerhaft gesperrt", false) {}
        disabledFileSyncV2.isEnabled = false
        disabledFileSyncV2.isClickable = false
        disabledFileSyncV2.isFocusable = false
        disabledFileSyncV2.alpha = 0.55f
        disabledFileSyncV2.contentDescription =
            "FileSync V2 gesperrt; CIRQA hat die frühere Anfrage abgelehnt. Keine Anfrage wird gesendet."
        debugView(disabledFileSyncV2)
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
                .setMessage("Garmin Connect vorher beenden. PulsLibre registriert GFDI, Puls und Schritte ohne CLOSE_ALL, wartet maximal 20 Sekunden auf echte Messwerte und trennt die Verbindung. Empfangene Messwerte werden lokal für die Übersicht gespeichert. Keine Kopplungsänderung. Fortfahren?")
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
                .setMessage("Garmin Connect vorher beenden. PulsLibre versucht zusätzlich SpO₂, Atmung, HRV, Stress, Body Battery, Kalorien und Intensitätsminuten einzeln zu registrieren. Nicht unterstützte Dienste werden gemeldet. Maximal 20 Sekunden, kein CLOSE_ALL. Empfangene Messwerte werden lokal für die Übersicht gespeichert.")
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
                .setMessage("Garmin Connect vorher vollständig beenden. Anders als beim normalen Service-16-Test registriert PulsLibre den Dienst und sendet anschließend das Startkommando 0x01 auf dem bestätigten Handle. Maximal 20 Sekunden; Paketanzahl wird angezeigt, keine Rohbewegungsdaten gespeichert. Beim Beenden wird 0x00 zum Stoppen versucht. Kein CLOSE_ALL, keine Kopplungsänderung. Jetzt testen?")
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
        debugView(actionButton("CIRQA: kontinuierliche Sammlung starten (lokal)", false) {
            if (!ensurePermissions()) return@actionButton
            if (scanning) stopScan()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lokale CIRQA-Sammlung")
                .setMessage("PulsLibre verbindet sich aktiv per Bluetooth mit dem CIRQA-Armband und hält die GATT-Verbindung für verfügbare Echtzeitmesswerte offen. Bei Verbindungsabbrüchen versucht die App die Verbindung erneut. Messwerte und Sitzungsereignisse werden lokal in der App-Datenbank gespeichert. Es werden keine historischen FIT-Daten geladen und keine Daten in eine Cloud übertragen. Android zeigt eine dauerhafte Benachrichtigung mit Stopp-Funktion; das System kann den Dienst dennoch beenden. Garmin Connect kann die Bluetooth-Verbindung beeinflussen. Jetzt starten?")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Starten") { _, _ ->
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(
                            this, Intent(this, CirqaContinuousRecordingService::class.java)
                                .setAction(CirqaContinuousRecordingService.ACTION_START))
                        output.text = "CIRQA-Sammlung angefordert. Laufstatus in Android-Benachrichtigung; Daten bleiben lokal."
                    } catch (_: Exception) {
                        output.text = "CIRQA-Sammlung konnte nicht gestartet werden (Android-Berechtigung oder Hintergrundbeschränkung)."
                    }
                }.show()
        })
        debugView(actionButton("CIRQA: Sammlung stoppen", false) {
            startService(Intent(this, CirqaContinuousRecordingService::class.java)
                .setAction(CirqaContinuousRecordingService.ACTION_STOP))
            output.text = "Stopp der CIRQA-Sammlung angefordert."
        })
        debugView(actionButton("CIRQA: Lokale Dateistatistik", false) {
            val log = CirqaLocalLog(this)
            output.text = "CIRQA JSONL lokal: ${log.sizeBytes()} Bytes, ${log.lines()} Zeilen. Keine automatische Aggregation; Originalmessungen und Versuchsergebnisse bleiben erhalten."
        })
        debugView(actionButton("CIRQA: Messprotokoll exportieren", false) {
            exportCirqaLog.launch("pulselibre-cirqa-overnight.jsonl")
        })
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
            deviceViews.forEach { it.visibility = if (index == 2 && (debugDevicesVisible || it !in debugDeviceViews)) android.view.View.VISIBLE else android.view.View.GONE }
            if (index == 1) {
                dashboardLastEventId = -1L
                dashboardLastRefreshAt = 0L
                historyRefresh?.invoke()
            }
            if (index == 2) recorderRefresh?.invoke()
            if (index == 2) refreshUnknownFields()
            connectionIndicatorRefresh?.invoke()
            setCompactHeader(false)
            scroll.scrollTo(0, 0)
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
            android.widget.Toast.makeText(this, "Automatischer Start der Aufzeichnung nicht möglich – bitte manuell starten.", android.widget.Toast.LENGTH_LONG).show()
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
        if (requestCode == 11) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!granted && ::output.isInitialized) {
                output.text = "Benachrichtigungen sind aus: Die Aufzeichnung läuft, die Benachrichtigung mit Stopp-Taste ist aber unsichtbar. " +
                    "Beenden über „Stoppen“ in der App oder Benachrichtigungen in den Android-Einstellungen erlauben."
            }
        }
        if (requestCode == 10 && ::status.isInitialized) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            status.text = if (granted) "Bluetooth-Berechtigung erteilt · Scan erneut starten"
                else "Bluetooth-Berechtigung abgelehnt · Einstellungen → Apps → PulsLibre → Berechtigungen → Geräte in der Nähe"
            lastScanDiagnostic = status.text.toString()
            connectionIndicatorRefresh?.invoke()
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
        cirqaGattState = CirqaStateMapping.label(state)
        val prefs = getSharedPreferences(CirqaContinuousRecordingService.PREFS, MODE_PRIVATE)
        val link = CirqaLinkStatus.evaluate(
            RecorderStatus.isRunning(prefs), state,
            prefs.getLong(CirqaContinuousRecordingService.KEY_LAST_READING_MS, -1L).takeIf { it >= 0 },
            System.currentTimeMillis()
        )
        cirqaLink = link
        cirqaConnectionHistory.observe(System.currentTimeMillis(), state)
        val message = buildString {
            appendLine("CIRQA Smart Band")
            if (cirqaBondAddress != null) {
                appendLine("Mit Android gekoppelt")
                if (cirqaScanMatch) appendLine("Gleiche BLE-Adresse im Scan erkannt")
            }
            appendLine(cirqaGattState)
            append(link.label)
        }
        if (cirqaCard.text.toString() != message) cirqaCard.text = message
        return state
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
        val report = DiagnosticReport.build(
            advertisingDevices = diagnosticRows.size,
            scanStatus = lastScanDiagnostic,
            gattLabel = cirqaGattState,
            transitions = cirqaConnectionHistory.snapshot(),
            bonded = cirqaBondAddress != null,
            addressMatch = cirqaScanMatch,
            evidenceSummary = cirqaScanEvidence.summary(),
            rssiSummary = passiveRssiTracker.summary(),
            bondListQueried = bondedRows.isNotEmpty()
        )
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
        }, "BLE-Diagnose teilen"))
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
            contentDescription = "Aktuelles Gewicht in Kilogramm"
        }
        fun stepButton(label: String, delta: Double, description: String) = TextView(this).apply {
            text = label; textSize = 26f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            background = GradientDrawable().apply { setColor(DesignTokens.BRAND_PURPLE); cornerRadius = 14 * d }
            contentDescription = description
            isClickable = true; isFocusable = true
            setOnClickListener { input.setText(WeightStepper.format(WeightStepper.step(input.text.toString(), delta))); input.setSelection(input.text.length) }
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
        row.addView(stepButton("−", -WeightStepper.STEP_KG, "Gewicht um 0,1 Kilogramm verringern").apply { tag = "weight_minus" },
            LinearLayout.LayoutParams((56 * d).toInt(), (56 * d).toInt()))
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = (8 * d).toInt(); marginEnd = (8 * d).toInt() })
        row.addView(stepButton("+", WeightStepper.STEP_KG, "Gewicht um 0,1 Kilogramm erhöhen").apply { tag = "weight_plus" },
            LinearLayout.LayoutParams((56 * d).toInt(), (56 * d).toInt()))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * d).toInt(), (8 * d).toInt(), (20 * d).toInt(), 0)
            addView(row, LinearLayout.LayoutParams(-1, -2))
            addView(TextView(this@MainActivity).apply {
                text = "Schritt: 0,1 kg · gedrückt halten für schnelleres Ändern"; textSize = 12f; setTextColor(palette.muted)
                gravity = android.view.Gravity.CENTER; setPadding(0, (8 * d).toInt(), 0, 0)
            })
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Gewicht (kg)")
            .setView(box)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Speichern", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = WeightStepper.parse(input.text.toString())
                if (value == null || value !in WeightStepper.MIN_KG..WeightStepper.MAX_KG) {
                    Toast.makeText(this, "Bitte ein Gewicht zwischen 15 und 400 kg eingeben.", Toast.LENGTH_LONG).show()
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
            append("BMI = Gewicht (kg) / Größe (m)². Er ist ein Screening-Hinweis, keine Diagnose.\n\n")
            append("Einordnung für Erwachsene ab 20 Jahren nach WHO (gleiche Grenzen für Frauen und Männer):\n")
            append("unter 18,5 Untergewicht · 18,5–24,9 Normalgewicht · 25–29,9 Präadipositas · 30–34,9 Adipositas Grad I · 35–39,9 Grad II · ab 40 Grad III.\n\n")
            append("Grenzen: Muskelmasse, Körperbau, Schwangerschaft und Alter beeinflussen die Aussagekraft. ")
            append("Für Menschen unter 20 Jahren gibt es hier keine Bewertung (BMI-für-Alter-Referenzen nicht umgesetzt). ")
            append("Für ältere Erwachsene werden keine Sondergrenzen angewandt.")
            bmi.note?.let { append("\n\nHinweis: ").append(it) }
        }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("BMI · Einordnung").setMessage(text)
            .setPositiveButton("Schließen", null).show()
    }

    override fun onDestroy() {
        dashboardWorker.shutdownNow()
        stopDashboardPolling()
        runCatching { pollDatabase.close() }
        dashboardRefresh = null
        connectionIndicatorRefresh = null
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

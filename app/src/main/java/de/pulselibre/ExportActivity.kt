package de.pulselibre

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private val executor = Executors.newSingleThreadExecutor()
    private var pendingWindow: ExportWindow? = null

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
        val violet = Color.rgb(81, 39, 201)
        val ink = Color.rgb(35, 29, 59)
        val muted = Color.rgb(109, 104, 127)
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
            setBackgroundColor(Color.rgb(248, 247, 252))
            fitsSystemWindows = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(violet, Color.rgb(94, 65, 187))
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
            contentDescription = "Zurück zur Übersicht"
            setOnClickListener { finish() }
        }
        headerRow.addView(label("Datenexport", 22f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        headerRow.addView(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_humerus_settings)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = "PulsLibre Armbruch-Symbol"
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        header.addView(headerRow)
        header.addView(label("CSV · lokal und offline", 12f, Color.WHITE))
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
            background = rounded(Color.WHITE)
        }
        root.addView(panel)
        panel.addView(label("Zeitraum", 17f, ink, true),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        fun dateField(): TextView = label("", 14f, violet, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(48)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(Color.rgb(241, 235, 255), 12)
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
            background = rounded(violet, 14)
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
        updateLabels()
        setContentView(screen)
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
        val legacy = CirqaDatabase(this).use { db ->
            db.importJsonl()
            db.importLegacyReadings()
            db.legacyLines().mapNotNull { line ->
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
        }
        return legacy + readCirqaExport(window)
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
            db.importJsonl()
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

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

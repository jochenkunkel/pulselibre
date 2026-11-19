package de.pulselibre

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Modern device overview: link hero, facts as icon chips, per-source reception, band metadata placeholders (#126). */
internal class DeviceOverviewView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val d = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d + 0.5f).toInt()
    private var link: CirqaLinkStatus.Result? = null
    private var stats: DeviceStats? = null
    private var recorderRunning = false
    private var battery: BatteryReading? = null

    init { orientation = VERTICAL; tag = "device_overview"; render() }

    /** Stored band battery (#159); null keeps the honest "Nicht verfügbar". */
    fun showBattery(reading: BatteryReading?) { if (battery != reading) { battery = reading; render() } }

    fun update(link: CirqaLinkStatus.Result, running: Boolean) {
        val changed = this.link?.kind != link.kind || this.link?.online != link.online || recorderRunning != running
        this.link = link; recorderRunning = running
        if (changed) render() else heroSubtitle?.text = link.label
    }

    fun showStats(stats: DeviceStats) { this.stats = stats; render() }

    private var heroSubtitle: TextView? = null

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        this.text = text; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun card(): LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(20).toFloat(); setStroke(dp(1), palette.track) }
    }

    private fun render() {
        removeAllViews()
        val l = link
        // --- hero ---
        val online = l?.online == true
        val tone = when { online -> Color.rgb(31, 190, 112); recorderRunning -> Color.rgb(228, 112, 30); else -> palette.muted }
        val hero = card().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, DesignTokens.heroColors(palette.dark)).apply {
                cornerRadius = dp(24).toFloat(); setStroke(dp(1), DesignTokens.heroStroke(palette.dark))
            }
        }
        val top = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        // CIRQA is a fitness band, not a watch: the band icon sits in a ring whose colour is the link state.
        top.addView(FrameLayout(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x33000000); setStroke(dp(3), tone) }
            addView(PlIcon.view(context, PlIcon.CIRQA_BAND, 34), FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))
        }, LayoutParams(dp(62), dp(62)).apply { marginEnd = dp(14) })
        val texts = LinearLayout(context).apply { orientation = VERTICAL }
        val heroText = DesignTokens.heroText(palette.dark)
        texts.addView(label("CIRQA", 12f, DesignTokens.heroSecondary(palette.dark), true))
        texts.addView(label(when {
            l == null -> "Status wird gelesen …"
            online -> "Verbunden"
            recorderRunning -> "Wiederverbindung"
            l.kind == CirqaLinkStatus.Kind.UNAVAILABLE -> "Nicht gekoppelt"
            else -> "Nicht verbunden"
        }, 22f, heroText, true))
        heroSubtitle = label(l?.label ?: "", 12f, DesignTokens.heroSecondary(palette.dark)).apply { setPadding(0, dp(2), 0, 0) }
        texts.addView(heroSubtitle)
        top.addView(texts, LayoutParams(0, -2, 1f))
        hero.addView(top)
        hero.contentDescription = "CIRQA ${if (online) "verbunden" else "nicht verbunden"}. ${l?.label.orEmpty()}"
        hero.isFocusable = true
        addView(hero, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        val s = stats
        // --- facts as icon chips (2 per row) ---
        val chips = listOf(
            Triple(PlIcon.SIGNAL, "Empfang · 1 Std.", s?.let { DeviceFormat.count(it.readingsLastHour.toLong()) + " Messwerte" } ?: "…"),
            Triple(PlIcon.STORAGE, "Speicher", s?.let { DeviceFormat.count(it.totalEvents) + " Einträge\n" + DeviceFormat.bytes(it.databaseBytes) } ?: "…"),
            Triple(PlIcon.SESSIONS, "Sitzungen heute", s?.let { "${it.sessionsToday} Starts\n${it.reconnectsToday} Wiederverbindungen" } ?: "…"),
            Triple(PlIcon.CALENDAR, "Aufzeichnung seit", s?.firstReading?.let { DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(it) } ?: "–")
        )
        chips.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            pair.forEachIndexed { i, (icon, title, value) ->
                row.addView(chip(icon, title, value), LayoutParams(0, -1, 1f).apply { if (i == 0) marginEnd = dp(8) })
            }
            addView(row, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        // --- reception per source ---
        val reception = card()
        reception.addView(label("Letzter Empfang je Messgröße", 14f, palette.ink, true))
        val now = Instant.now()
        DeviceSource.entries.forEach { src ->
            val last = s?.lastBySource?.get(src)
            val fresh = ReceptionFreshness.of(last, now)
            val col = when (fresh) {
                ReceptionFreshness.FRESH -> Color.rgb(31, 166, 113); ReceptionFreshness.RECENT -> Color.rgb(207, 166, 19)
                ReceptionFreshness.DELAYED -> Color.rgb(228, 112, 30); else -> palette.muted
            }
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(34) }
            row.addView(PlIcon.view(context, src.icon, 22), LayoutParams(dp(28), dp(22)))
            row.addView(label(src.title, 13f, palette.ink), LayoutParams(0, -2, 1f))
            row.addView(label(if (s == null) "…" else DeviceFormat.age(last, now), 12f, col, true).apply {
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(12).toFloat(); setStroke(dp(1), col) }
            })
            row.contentDescription = "${src.title}: ${if (s == null) "wird gelesen" else DeviceFormat.age(last, now)}"
            reception.addView(row, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        addView(reception, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        // --- band metadata: never infer battery, firmware or band clock from reception statistics ---
        val meta = card()
        meta.addView(label("Bandinformationen", 14f, palette.ink, true))
        meta.addView(label("Gerätewerte werden nur mit bestätigter Quelle und aktuellem Zeitstempel angezeigt.", 11f, palette.muted),
            LayoutParams(-1, -2).apply { topMargin = dp(2) })
        listOf(PlIcon.BATTERY to "Akku", PlIcon.FIRMWARE to "Firmware", PlIcon.BAND_TIME to "Uhrzeit des Bands", PlIcon.IDENTITY to "Modell / Seriennummer").forEach { (icon, name) ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(34) }
            val reading = if (icon == PlIcon.BATTERY) battery else null
            row.addView(PlIcon.view(context, icon, 22).apply { alpha = if (reading != null) 1f else 0.55f }, LayoutParams(dp(28), dp(22)))
            row.addView(label(name, 13f, palette.ink), LayoutParams(0, -2, 1f))
            if (reading != null) {
                val view = DeviceBatteryFormat.describe(reading, System.currentTimeMillis())
                val column = LinearLayout(context).apply { orientation = VERTICAL; gravity = Gravity.END }
                column.addView(label(view.valueText, 14f, palette.ink, true))
                column.addView(DeviceBatteryBar(context, reading.percent,
                    DeviceBatteryFormat.levelColor(reading.percent, palette.dark), palette.track), LayoutParams(dp(120), dp(8)).apply { topMargin = dp(4); gravity = Gravity.END })
                column.addView(label(view.freshness, 11f, if (view.stale) palette.muted else palette.ink), LayoutParams(-2, -2).apply { topMargin = dp(3) })
                row.addView(column)
                row.contentDescription = view.description
                row.tag = "device_battery_row"
            } else {
                row.addView(label("Nicht verfügbar", 12f, palette.muted))
                row.contentDescription = "$name: nicht verfügbar; bisher kein bestätigter Gerätewert"
            }
            meta.addView(row, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        addView(meta, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun chip(icon: PlIcon, title: String, value: String): View = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(16).toFloat(); setStroke(dp(1), palette.track) }
        val head = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(PlIcon.view(context, icon, 20), LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
        head.addView(label(title, 11f, palette.muted, true))
        addView(head)
        addView(label(value, 13f, palette.ink, true), LayoutParams(-1, -2).apply { topMargin = dp(6) })
        contentDescription = "$title: $value"
    }
}

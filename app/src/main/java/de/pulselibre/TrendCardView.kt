package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.Executors

/** 7/28/90-day trends of device resting pulse and daily steps (#41). Not a diagnosis. */
internal class TrendCardView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val content = LinearLayout(context).apply { orientation = VERTICAL }
    private var generation = 0
    private var data: Data? = null
    private val nf = NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 1 }

    /** Everything the card shows, computed off the UI thread. */
    class Data(
        val overview: FitnessOverview.Result,
        val rest: List<TrendAnalysis.Result>,
        val steps: List<TrendAnalysis.Result>,
        val correlation: PairedCorrelation.Result,
        val scatter: List<Pair<Double, Double>>,
        val restDaily: Map<LocalDate, Double> = emptyMap(),
        val stepsDaily: Map<LocalDate, Double> = emptyMap(),
        val today: LocalDate = LocalDate.now(),
        val sensitivity: PairedCorrelation.Sensitivity? = null
    )

    init {
        orientation = VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(16).toFloat(); UnvalidatedFeatures.mark(this, resources.displayMetrics.density, palette.dark) }
        contentDescription = "Trends. Enthält ungeprüften Geräte-Ruhepuls." + UnvalidatedFeatures.ACCESSIBILITY_SUFFIX
        tag = "trend_card"
        addView(content, LayoutParams(-1, -2))
        showMessage("Trends werden berechnet …")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val cached = cachedData
        if (cached != null && System.currentTimeMillis() - cachedAt < CACHE_MS) { data = cached; render(cached); return }
        load()
    }

    override fun onDetachedFromWindow() { generation++; super.onDetachedFromWindow() }

    private fun load() {
        val token = ++generation
        val app = context.applicationContext
        worker.execute {
            val result = try { compute(app) } catch (_: Exception) { null }
            Handler(Looper.getMainLooper()).post {
                if (token != generation) return@post
                if (result == null) { showMessage("Trends konnten nicht berechnet werden."); return@post }
                cachedData = result; cachedAt = System.currentTimeMillis(); data = result
                render(result)
            }
        }
    }

    private fun compute(app: Context): Data {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val first = today.minusDays(180)
        val reader = DailyValueReader(app)
        val rest = reader.read(DashboardMeasurement.RESTING_PULSE, first, today, zone)
        val steps = reader.read(DashboardMeasurement.STEPS, first, today, zone)
        val windows = listOf(7, 28, 90)
        val restTrends = windows.map { TrendAnalysis.analyze(rest, today, it, 1.0) }
        val stepTrends = windows.map { TrendAnalysis.analyze(steps, today, it, 500.0) }
        return Data(FitnessOverview.assess(stepTrends[1], restTrends[1]), restTrends, stepTrends,
            PairedCorrelation.analyze(steps, rest, 1),
            PairedCorrelation.pair(steps, rest, 1).map { it.second to it.third }, rest, steps, today,
            PairedCorrelation.sensitivity(steps, rest, 1))
    }

    private fun showMessage(text: String) {
        content.removeAllViews()
        content.addView(titleRow(null))
        content.addView(TextView(context).apply { this.text = text; textSize = 13f; setTextColor(palette.muted) })
    }

    private fun titleRow(info: (() -> Unit)?): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(PlIcon.view(context, PlIcon.TRENDS, 24), LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
        addView(TextView(context).apply {
            text = "Trends"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink)
        }, LayoutParams(0, -2, 1f))
        if (info != null) addView(FrameLayout(context).apply {
            contentDescription = "Methode und Einschränkungen der Trends"
            isClickable = true; isFocusable = true
            setOnClickListener { info() }
            addView(PlIcon.view(context, PlIcon.INFO, 22), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        }, LayoutParams(dp(48), dp(48)))
    }

    /** Dimension icons identify *what* is assessed; the state is written text only (no status via icon). */
    private fun dimensionIcon(name: String) = when (name) {
        "Fitnessentwicklung" -> PlIcon.FITNESS
        "Regeneration" -> PlIcon.RECOVERY
        else -> PlIcon.CONSISTENCY
    }

    private fun render(d: Data) {
        content.removeAllViews()
        windowListeners.clear()
        content.addView(titleRow { showDetails(d) })
        addWindowSegments()
        addRestingHero(d)
        // 28-day overview: icon + name + state; sentences live in the ⓘ dialog and as accessibility text.
        d.overview.dimensions.forEach { dim ->
            content.addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(32)
                contentDescription = "${dim.name}: ${dim.state.label}. ${dim.sentence}"
                isFocusable = true
                addView(PlIcon.view(context, dimensionIcon(dim.name), 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
                addView(TextView(context).apply { text = dim.name; textSize = 13f; setTextColor(palette.ink)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(0, -2, 1f))
                addView(TextView(context).apply { text = dim.state.label; textSize = 12f; setTextColor(palette.muted)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
            })
        }
        // Trend table: rows = metric, columns = 7 / 28 / 90 days.
        content.addView(tableRow(null, listOf("7 T", "28 T", "90 T")), LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content.addView(tableRow("Ruhepuls", d.rest.map { cell(it) }, PlIcon.HEART).apply {
            tag = "trend_row_rest"; isClickable = true; isFocusable = true; minimumHeight = dp(48)
            setOnClickListener { showDaily(d, "Geräte-Ruhepuls (bpm)", d.restDaily, d.rest, palette.pulse, zeroBased = false) }
        })
        content.addView(tableRow("Schritte", d.steps.map { cell(it) }, PlIcon.STEPS).apply {
            tag = "trend_row_steps"; isClickable = true; isFocusable = true; minimumHeight = dp(48)
            setOnClickListener { showDaily(d, "Schritte pro Tag", d.stepsDaily, d.steps, palette.indicator) }
        })
        // Pre-specified hypothesis, one compact line; tap opens the scatter plot.
        val c = d.correlation
        val corr = if (c.rho == null) "${c.pairs}/${PairedCorrelation.MIN_PAIRS} Tage"
            else "${nf.format(c.rho)} ρ · n=${c.pairs}"
        content.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            isClickable = true; isFocusable = true
            contentDescription = "Zusammenhang Schritte und Ruhepuls am Folgetag: ${c.verdict.label}, ${c.note}. Tippen für das Streudiagramm."
            setOnClickListener { showScatter() }
            addView(PlIcon.view(context, PlIcon.TREND_LINK, 22), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(PlIcon.view(context, PlIcon.STEPS, 18), LayoutParams(dp(18), dp(18)))
                addView(TextView(context).apply { text = " → "; textSize = 13f; setTextColor(palette.ink) })
                addView(PlIcon.view(context, PlIcon.HEART, 18), LayoutParams(dp(18), dp(18)))
                addView(TextView(context).apply { text = "  Folgetag"; textSize = 13f; setTextColor(palette.ink) })
            }, LayoutParams(0, -2, 1f))
            if (c.rho == null) {
                addView(PlIcon.view(context, PlIcon.HOURGLASS, 18), LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(4) })
                addView(TextView(context).apply { text = "${c.pairs}/${PairedCorrelation.MIN_PAIRS} Tage"; textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, dp(8), 0) })
            } else addView(TextView(context).apply { text = corr; textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, dp(8), 0) })
            if (d.scatter.size >= 2) addView(PlIcon.view(context, PlIcon.TRENDS, 22), LayoutParams(dp(22), dp(22)))
        }, LayoutParams(-1, -2).apply { topMargin = dp(4) })
    
        addAtlas(d)
    }

    /** One shared window (7 / 28 / 90 days, default 28) for hero and atlas (#41). */
    private var atlasWindow = 28
    private val windowListeners = mutableListOf<() -> Unit>()

    private fun addWindowSegments() {
        val segments = LinearLayout(context).apply { orientation = HORIZONTAL; tag = "window_segments" }
        listOf(7, 28, 90).forEach { days ->
            segments.addView(TextView(context).apply {
                text = "$days T"; textSize = 12f; gravity = Gravity.CENTER; minimumHeight = dp(48); tag = "atlas_window_$days"
                isClickable = true; isFocusable = true; contentDescription = "Zeitraum $days Tage"
                setOnClickListener {
                    atlasWindow = days
                    for (i in 0 until segments.childCount) (segments.getChildAt(i) as TextView).let { t -> styleSegment(t, t.tag == "atlas_window_$atlasWindow") }
                    windowListeners.forEach { it() }
                }
                styleSegment(this, atlasWindow == days)
            }, LayoutParams(0, -2, 1f))
        }
        content.addView(segments, LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun styleSegment(t: TextView, on: Boolean) {
        t.setTextColor(if (on) palette.accent else palette.muted); t.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Resting-pulse hero (#41 "Pulse Flow"): median, neutral delta, daily points with gaps, previous-period baseline. */
    private fun addRestingHero(d: Data) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "resting_hero"; setPadding(0, dp(8), 0, dp(4)) }
        box.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.HEART, 22, palette.pulse), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(TextView(context).apply { text = "Ruhepuls · Entwicklung"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
        })
        val value = TextView(context).apply { textSize = 28f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink); tag = "resting_hero_value" }
        val delta = TextView(context).apply { textSize = 12f; setTextColor(palette.muted); tag = "resting_hero_delta" }
        val message = TextView(context).apply { textSize = 11f; setTextColor(palette.muted); tag = "resting_hero_message" }
        val chart = RestingPulseChartView(context)
        box.addView(value, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        box.addView(delta)
        box.addView(chart, LayoutParams(-1, -2).apply { topMargin = dp(6) })
        box.addView(message, LayoutParams(-1, -2).apply { topMargin = dp(4) })
        box.isClickable = true; box.isFocusable = true; box.minimumHeight = dp(48)
        box.setOnClickListener { showDaily(d, "Geräte-Ruhepuls (bpm)", d.restDaily, d.rest, palette.pulse, zeroBased = false) }
        fun refresh() {
            val trend = TrendAnalysis.analyze(d.restDaily, d.today, atlasWindow, 1.0)
            val m = RestingPulseHero.build(d.restDaily, d.today, atlasWindow, trend)
            value.text = m.valueText ?: "–"
            delta.text = m.deltaText.orEmpty(); delta.visibility = if (m.deltaText == null) GONE else VISIBLE
            message.text = m.message
            chart.show(m)
            box.contentDescription = m.description() + " Tippen für die Tageswerte."
        }
        windowListeners.add { refresh() }
        refresh()
        content.addView(box)
    }

    /** Activity atlas (#41): calendar of daily step totals for the shared window. */
    private fun addAtlas(d: Data) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; tag = "activity_atlas_box" }
        box.addView(View(context).apply { setBackgroundColor(palette.track) }, LayoutParams(-1, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(10) })
        box.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(PlIcon.view(context, PlIcon.STEPS, 22, palette.indicator), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) })
            addView(TextView(context).apply { text = "Aktivitäts-Atlas · Schritte"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
        })
        val summary = TextView(context).apply { textSize = 11f; setTextColor(palette.muted); setPadding(0, dp(4), 0, dp(6)); tag = "activity_atlas_summary" }
        val atlas = ActivityAtlasView(context) { cell ->
            androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Tagesdetail")
                .setMessage(ActivityAtlas.describe(cell)).setPositiveButton("Schließen", null).show()
        }
        fun refresh() {
            val m = ActivityAtlas.build(d.stepsDaily, d.today, atlasWindow)
            atlas.show(m); summary.text = m.summary()
        }
        box.addView(summary)
        box.addView(atlas, LayoutParams(-1, -2))
        box.addView(TextView(context).apply {
            text = "Abstufung = relative Schrittzahl der erfassten Tage (kein Gesundheitsstatus). Graue Zelle = keine verifizierten Daten, nicht 0 Schritte. " +
                "Umrandet = heute, noch nicht abgeschlossen. Trends beschreiben deine erfassten Daten – keine Fitnessdiagnose."
            textSize = 11f; setTextColor(palette.muted); setPadding(0, dp(8), 0, 0)
        })
        windowListeners.add { refresh() }
        refresh()
        content.addView(box)
    }

    private fun cell(r: TrendAnalysis.Result): String =
        if (r.direction == TrendAnalysis.Direction.UNKNOWN) "–\n${r.current.validDays}/${r.windowDays}"
        else "${compact(r.current.median!!)} ${r.direction.arrow}\n${r.current.validDays}/${r.windowDays}"

    /** Whole numbers from 100 up (7.762 steps), one decimal below (62,5 bpm) – keeps the 70 dp column single-line. */
    private fun compact(v: Double): String =
        if (Math.abs(v) >= 100) NumberFormat.getIntegerInstance(Locale.GERMANY).format(Math.round(v)) else nf.format(v)

    private fun tableRow(label: String?, cells: List<String>, icon: PlIcon? = null): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            if (icon != null) addView(PlIcon.view(context, icon, 20), LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(6) })
            addView(TextView(context).apply { text = label.orEmpty(); textSize = 13f; setTextColor(palette.ink) })
        }, LayoutParams(0, -2, 1.3f))
        cells.forEach { c ->
            addView(TextView(context).apply {
                text = c; textSize = if (label == null) 11f else 12f
                setTextColor(if (label == null || c.startsWith("–")) palette.muted else palette.ink)
                gravity = Gravity.CENTER
                if (label == null) setTypeface(null, Typeface.BOLD)
                contentDescription = if (label == null) c else
                    "$label ${c.replace("\n", ", Tage mit Daten ")}"
            }, LayoutParams(0, -2, 1f))
        }
    }

    /** Drilldown: daily values of the last 90 days with the 7/28-day medians (#41). */
    private fun showDaily(d: Data, title: String, values: Map<LocalDate, Double>, trends: List<TrendAnalysis.Result>, color: Int, zeroBased: Boolean = true) {
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(16), dp(8), dp(16), 0) }
        val present = (1..90).count { values.containsKey(d.today.minusDays(it.toLong())) }
        box.addView(DailyBarsView(context, values, d.today, 90, color, palette.ink, palette.track,
            listOf("Median 7 T" to trends[0].current.median, "Median 28 T" to trends[1].current.median), zeroBased),
            LayoutParams(-1, -2))
        box.addView(TextView(context).apply {
            setTextColor(palette.muted); textSize = 12f; setPadding(0, dp(8), 0, 0)
            text = "$present von 90 Tagen mit Daten · leere Tage = keine Messung (nicht 0). " +
                "Gestrichelt: Median der letzten 7 bzw. 28 vollständigen Tage. Der heutige Tag ist nicht enthalten. Keine Diagnose."
        })
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle(title).setView(box).setPositiveButton("Schließen", null).show()
    }

    /** Full explanation moved out of the card body. */
    private fun showDetails(d: Data) {
        val text = buildString {
            append("Übersicht (28 Tage, ohne Gesamtpunktzahl)")
            d.overview.dimensions.forEach { append("\n• ${it.name}: ${it.state.label} – ${it.sentence}") }
            append("\n\nGrößen je Zeitraum: Median vollständiger Tage, Pfeil = Richtung gegenüber dem gleich langen Zeitraum davor, darunter Tage mit Daten / Zeitraum.")
            listOf("Geräte-Ruhepuls (bpm)" to d.rest, "Schritte pro Tag" to d.steps).forEach { (title, list) ->
                append("\n\n$title")
                list.forEach { r ->
                    append("\n${r.windowDays} Tage: ")
                    if (r.direction == TrendAnalysis.Direction.UNKNOWN) append("nicht bewertbar · ${r.reason}")
                    else append("${nf.format(r.current.median)} ${r.direction.arrow} ${if (r.delta!! > 0) "+" else ""}${nf.format(r.delta)} (davor ${nf.format(r.baseline.median)}) · ${r.current.validDays}/${r.windowDays} Tage")
                }
            }
            val c = d.correlation
            append("\n\nZusammenhang (explorativ, 1 vorab festgelegte Hypothese): Schritte eines Tages ↔ Geräte-Ruhepuls am Folgetag\n")
            if (c.rho == null) append("${c.verdict.label} · ${c.note}")
            else append("${c.verdict.label} · ρ=${nf.format(c.rho)} (95 %: ${nf.format(c.ciLow)} bis ${nf.format(c.ciHigh)}) · n=${c.pairs} Tage. ${c.note}")
            append("\n\nTage ohne Daten werden nicht aufgefüllt; zu wenige Tage ergeben „nicht bewertbar“. Der Ruhepuls ist der vom Gerät gemeldete Wert, seine Messbedingungen sind ungeprüft. Schritte sind Aktivitätsvolumen, kein Maß für Fitness. Keine Diagnose.")
        }
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Trends · Methode")
            .setMessage(text).setPositiveButton("OK", null).show()
    }

    private fun showScatter() {
        val pairs = data?.scatter.orEmpty()
        if (pairs.size < 2) return
        val view = ScatterView(context, pairs, "Schritte (Tag)", "Ruhepuls Folgetag (bpm)", palette.ink, palette.accent, palette.muted)
        val note = TextView(context).apply {
            setTextColor(palette.ink); textSize = 12f
            text = "${pairs.size} Wertepaare · jeder Punkt = ein Tag. Ohne Trendlinie. " +
                "Ein Zusammenhang in den Punkten sagt nichts über Ursache und Wirkung."
            setPadding(0, dp(8), 0, 0)
        }
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(16), dp(12), dp(16), 0); addView(view); addView(note) }
        data?.sensitivity?.let { s ->
            box.addView(TextView(context).apply {
                tag = "scatter_sensitivity"; textSize = 12f; setTextColor(palette.ink); setPadding(0, dp(8), 0, 0)
                text = s.text(nf)
            })
        }
        androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Schritte ↔ Ruhepuls am Folgetag")
            .setView(box).setPositiveButton("Schließen", null).show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        val worker = Executors.newSingleThreadExecutor()
        const val CACHE_MS = 10 * 60_000L
        @Volatile var cachedData: Data? = null
        @Volatile var cachedAt = 0L
    }
}

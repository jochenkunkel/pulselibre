package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
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
        val scatter: List<Pair<Double, Double>>
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
            PairedCorrelation.pair(steps, rest, 1).map { it.second to it.third })
    }

    private fun showMessage(text: String) {
        content.removeAllViews()
        content.addView(titleRow(null))
        content.addView(TextView(context).apply { this.text = text; textSize = 13f; setTextColor(palette.muted) })
    }

    private fun titleRow(info: (() -> Unit)?): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply { text = "📈"; textSize = 18f; setPadding(0, 0, dp(8), 0) })
        addView(TextView(context).apply {
            text = "Trends"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink)
        }, LayoutParams(0, -2, 1f))
        if (info != null) addView(TextView(context).apply {
            text = "ⓘ"; textSize = 20f; setTextColor(palette.accent); gravity = Gravity.CENTER
            minWidth = dp(48); minHeight = dp(48)
            contentDescription = "Methode und Einschränkungen der Trends"
            isClickable = true; isFocusable = true
            setOnClickListener { info() }
        })
    }

    private fun stateIcon(s: FitnessOverview.State) = when (s) {
        FitnessOverview.State.POSITIVE -> "✅"
        FitnessOverview.State.NEUTRAL -> "➖"
        FitnessOverview.State.MIXED -> "⚠️"
        FitnessOverview.State.NOT_ASSESSABLE -> "⏳"
    }

    private fun render(d: Data) {
        content.removeAllViews()
        content.addView(titleRow { showDetails(d) })
        // 28-day overview: icon + name + state; sentences live in the ⓘ dialog and as accessibility text.
        d.overview.dimensions.forEach { dim ->
            content.addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(32)
                contentDescription = "${dim.name}: ${dim.state.label}. ${dim.sentence}"
                isFocusable = true
                addView(TextView(context).apply { text = stateIcon(dim.state); textSize = 15f; setPadding(0, 0, dp(8), 0)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
                addView(TextView(context).apply { text = dim.name; textSize = 13f; setTextColor(palette.ink)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(0, -2, 1f))
                addView(TextView(context).apply { text = dim.state.label; textSize = 12f; setTextColor(palette.muted)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
            })
        }
        // Trend table: rows = metric, columns = 7 / 28 / 90 days.
        content.addView(tableRow(null, listOf("7 T", "28 T", "90 T")), LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content.addView(tableRow("💓 Ruhepuls", d.rest.map { cell(it) }))
        content.addView(tableRow("👣 Schritte", d.steps.map { cell(it) }))
        // Pre-specified hypothesis, one compact line; tap opens the scatter plot.
        val c = d.correlation
        val corr = if (c.rho == null) "⏳ ${c.pairs}/${PairedCorrelation.MIN_PAIRS} Tage"
            else "${nf.format(c.rho)} ρ · n=${c.pairs}"
        content.addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            isClickable = true; isFocusable = true
            contentDescription = "Zusammenhang Schritte und Ruhepuls am Folgetag: ${c.verdict.label}, ${c.note}. Tippen für das Streudiagramm."
            setOnClickListener { showScatter() }
            addView(TextView(context).apply { text = "🔗"; textSize = 15f; setPadding(0, 0, dp(8), 0) })
            addView(TextView(context).apply { text = "👣 → 💓 Folgetag"; textSize = 13f; setTextColor(palette.ink) }, LayoutParams(0, -2, 1f))
            addView(TextView(context).apply { text = corr; textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, dp(8), 0) })
            addView(TextView(context).apply { text = if (d.scatter.size >= 2) "📊" else ""; textSize = 16f })
        }, LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    private fun cell(r: TrendAnalysis.Result): String =
        if (r.direction == TrendAnalysis.Direction.UNKNOWN) "–\n${r.current.validDays}/${r.windowDays}"
        else "${nf.format(r.current.median)} ${r.direction.arrow}\n${r.current.validDays}/${r.windowDays}"

    private fun tableRow(label: String?, cells: List<String>): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply { text = label.orEmpty(); textSize = 13f; setTextColor(palette.ink) }, LayoutParams(0, -2, 1.3f))
        cells.forEach { c ->
            addView(TextView(context).apply {
                text = c; textSize = if (label == null) 11f else 13f
                setTextColor(if (label == null || c.startsWith("–")) palette.muted else palette.ink)
                gravity = Gravity.CENTER
                if (label == null) setTypeface(null, Typeface.BOLD)
                contentDescription = if (label == null) c else
                    "$label ${c.replace("\n", ", Tage mit Daten ")}"
            }, LayoutParams(0, -2, 1f))
        }
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

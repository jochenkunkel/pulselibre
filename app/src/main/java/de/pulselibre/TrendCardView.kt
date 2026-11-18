package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
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
    private val body = TextView(context)
    private var generation = 0

    init {
        orientation = VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(16).toFloat() }
        tag = "trend_card"
        addView(TextView(context).apply {
            text = "Trends · 7, 28 und 90 Tage"
            textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink)
        })
        body.apply { textSize = 13f; setTextColor(palette.ink); setLineSpacing(0f, 1.15f) }
        addView(body, LayoutParams(-1, -2).apply { topMargin = dp(8) })
        body.text = "Trends werden berechnet …"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val now = System.currentTimeMillis()
        if (cachedText != null && now - cachedAt < CACHE_MS) { body.text = cachedText; return }
        load()
    }

    override fun onDetachedFromWindow() { generation++; super.onDetachedFromWindow() }

    private fun load() {
        val token = ++generation
        val app = context.applicationContext
        worker.execute {
            val text = try { compute(app) } catch (_: Exception) { "Trends konnten nicht berechnet werden." }
            Handler(Looper.getMainLooper()).post {
                if (token != generation) return@post
                cachedText = text; cachedAt = System.currentTimeMillis()
                body.text = text
            }
        }
    }

    private fun compute(app: Context): String {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val first = today.minusDays(180)
        val reader = DailyValueReader(app)
        val nf = NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 1 }
        return buildString {
            val rest = reader.read(DashboardMeasurement.RESTING_PULSE, first, today, zone)
            val steps = reader.read(DashboardMeasurement.STEPS, first, today, zone)
            val overview = FitnessOverview.assess(
                TrendAnalysis.analyze(steps, today, 28, 500.0), TrendAnalysis.analyze(rest, today, 28, 1.0))
            append("Übersicht (28 Tage, ohne Gesamtpunktzahl)")
            overview.dimensions.forEach { append("\n• ${it.name}: ${it.state.label} – ${it.sentence}") }
            append("\n\n")
            section("Geräte-Ruhepuls (bpm)", rest, today, 1.0, nf)
            append("\n")
            section("Schritte pro Tag", steps, today, 500.0, nf)
            append("\n\nZusammenhang (explorativ, 1 vorab festgelegte Hypothese)\nSchritte eines Tages ↔ Geräte-Ruhepuls am Folgetag: ")
            val c = PairedCorrelation.analyze(steps, rest, 1)
            if (c.rho == null) append("${c.verdict.label} · ${c.note}")
            else append("${c.verdict.label} · ρ=${nf.format(c.rho)} (95 %: ${nf.format(c.ciLow)} bis ${nf.format(c.ciHigh)}) · n=${c.pairs} Tage. ${c.note}")
            append("\n\nMedian vollständiger Tage gegen den gleich langen Zeitraum davor. Tage ohne Daten werden nicht aufgefüllt; ")
            append("zu wenige Tage ergeben „nicht bewertbar“. Der Ruhepuls ist der vom Gerät gemeldete Wert, seine Messbedingungen sind ungeprüft. ")
            append("Schritte sind Aktivitätsvolumen, kein Maß für Fitness. Keine Diagnose.")
        }
    }

    private fun StringBuilder.section(title: String, values: Map<LocalDate, Double>, today: LocalDate, floor: Double, nf: NumberFormat) {
        append(title)
        for (n in listOf(7, 28, 90)) {
            val r = TrendAnalysis.analyze(values, today, n, floor)
            append("\n$n Tage: ")
            if (r.direction == TrendAnalysis.Direction.UNKNOWN) append("nicht bewertbar · ${r.reason}")
            else {
                val d = r.delta!!
                append("${nf.format(r.current.median)} ${r.direction.arrow} ${if (d > 0) "+" else ""}${nf.format(d)}")
                append(" (davor ${nf.format(r.baseline.median)}) · ${r.current.validDays}/$n Tage")
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        val worker = Executors.newSingleThreadExecutor()
        const val CACHE_MS = 10 * 60_000L
        @Volatile var cachedText: String? = null
        @Volatile var cachedAt = 0L
    }
}

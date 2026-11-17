package de.pulselibre

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Read-only cards for locally received observations. No transport or persistence ownership. */
class DashboardOverviewView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private val ink = palette.ink
    private val muted = palette.muted
    private val violet = palette.accent
    private var lastPulse: HeartRateGaugeModel? = null

    init { orientation = VERTICAL }

    fun render(metrics: List<DashboardMetric>, activity: StepActivityEstimate = StepActivityEstimate.UNKNOWN) {
        removeAllViews()
        val overview = dashboardOverview(metrics)
        addView(label("Deine Messwerte · lokal gespeichert", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addCardPair(overview.vitalSigns) { metric ->
            if (metric.title == "Herzfrequenz") pulseCard(metric, activity)
            else metricCard(metric, palette.oxygen)
        }
        addView(metricCard(overview.sleep, violet),
            LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        heading("Heute auf einen Blick")
        addView(label("Schritte: kumulativer Tageszähler, keine Summe", 12f, muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addCardPair(overview.today) { metric -> metricCard(metric, ink) }
        heading("Weitere Gesundheitswerte")
        overview.details.chunked(2).forEach { pair ->
            addCardPair(pair) { metric -> metricCard(metric, ink) }
        }
    }

    private fun pulseCard(metric: DashboardMetric, activity: StepActivityEstimate): LinearLayout {
        val model = HeartRateGaugeModel.from(metric)
        val previous = lastPulse
        lastPulse = model
        val card = card(metric)
        card.addView(header(metric))
        val gauge = HeartRateGaugeView(context)
        gauge.show(model, activity)
        card.addView(gauge, LayoutParams(-1, -2))
        card.addView(label(activity.caption, 11f, muted),
            LayoutParams(-1, -2).apply { topMargin = dp(2) })
        if (previous?.bpm != null && model.bpm != null && previous.bpm != model.bpm) {
            gauge.post { if (gauge.isAttachedToWindow) gauge.show(model, activity, previous.fraction) }
        }
        return card
    }

    private fun addCardPair(metrics: List<DashboardMetric>, create: (DashboardMetric) -> LinearLayout) {
        val stacked = resources.configuration.fontScale >= 1.3f
        val row = LinearLayout(context).apply { orientation = if (stacked) VERTICAL else HORIZONTAL }
        metrics.forEachIndexed { index, metric ->
            val view = create(metric)
            row.addView(view,
                if (stacked) LayoutParams(-1, -2).apply {
                    if (index != metrics.lastIndex) bottomMargin = dp(8)
                } else LayoutParams(0, -2, 1f).apply {
                    if (index != metrics.lastIndex) marginEnd = dp(8)
                })
        }
        addView(row, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun metricCard(metric: DashboardMetric, accent: Int) = card(metric).apply {
        addView(header(metric))
        addView(label(metric.value, 22f, accent, true),
            LayoutParams(-1, -2).apply { topMargin = dp(6) })
    }

    private fun card(metric: DashboardMetric) = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = GradientDrawable().apply {
            setColor(palette.card)
            cornerRadius = dp(18).toFloat()
        }
        tag = "dashboard_metric_${metric.title}"
        contentDescription = "${metric.title}: ${metric.value}"
        ViewCompat.setScreenReaderFocusable(this, false)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun header(metric: DashboardMetric) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(label(metric.title, 13f, muted, true), LayoutParams(0, -2, 1f))
        addView(ReceptionIndicatorView(context, metric).apply {
            tag = "reception_indicator_${metric.title}"
            minimumWidth = dp(48)
            minimumHeight = dp(48)
        }, LayoutParams(dp(48), dp(48)))
    }

    private fun heading(title: String) {
        val view = label(title, 17f, ink, true)
        ViewCompat.setAccessibilityHeading(view, true)
        addView(view, LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        includeFontPadding = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

/** Small, individually accessible tap target; only the drawn dot changes with reception age. */
@SuppressLint("ViewConstructor")
private class ReceptionIndicatorView(context: Context, private val metric: DashboardMetric) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val palette = AppPalette.of(context)
    private val state = ReceptionFreshness.of(metric.receivedAt, Instant.now())

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Empfangsinformationen zu ${metric.title}: ${state.label()}. Antippen für Details."
        setOnClickListener { showDetails() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = width / 2f
        val y = height / 2f
        val color = when (state) {
            ReceptionFreshness.FRESH -> Color.rgb(31, 166, 113)
            ReceptionFreshness.RECENT -> Color.rgb(207, 166, 19)
            ReceptionFreshness.DELAYED -> Color.rgb(228, 112, 30)
            ReceptionFreshness.OLD -> Color.rgb(205, 57, 65)
            ReceptionFreshness.UNKNOWN -> palette.muted
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = color
        canvas.drawCircle(x, y, 6f * density, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 3.1f * density, paint)
    }

    private fun showDetails() {
        val now = Instant.now()
        val timestamp = metric.receivedAt
        val received = timestamp?.let {
            val local = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss z")
                .withZone(ZoneId.systemDefault()).format(it)
            val seconds = Duration.between(it, now).seconds.coerceAtLeast(0)
            val age = when {
                seconds < 60 -> "vor $seconds Sekunden"
                seconds < 3600 -> "vor ${seconds / 60} Minuten"
                else -> "vor ${seconds / 3600} Stunden"
            }
            "Empfangen: $local ($age)"
        } ?: "Für diesen Wert liegt kein Empfangszeitpunkt vor."
        AlertDialog.Builder(context)
            .setTitle("Empfang · ${metric.title}")
            .setMessage("Wert: ${metric.value}\n$received\nStatus: ${state.label()}\n\n" +
                "Die Farbe zeigt nur das Alter des letzten Empfangs: Grün ≤ 3 min, Gelb ≤ 15 min, " +
                "Orange ≤ 60 min, Rot > 60 min; Grau = kein Zeitstempel. Sie bewertet weder Messqualität noch Gesundheit.\n\n" +
                "${metric.note}\n\nDer Zeitstempel stammt vom Empfang auf dem Telefon; ein Sensor-Messzeitpunkt ist nicht belegt.")
            .setPositiveButton("Schließen", null)
            .show()
    }

    private fun ReceptionFreshness.label() = when (this) {
        ReceptionFreshness.UNKNOWN -> "kein Empfangszeitpunkt"
        ReceptionFreshness.FRESH -> "frisch"
        ReceptionFreshness.RECENT -> "wenige Minuten alt"
        ReceptionFreshness.DELAYED -> "verzögert"
        ReceptionFreshness.OLD -> "lange ohne neuen Empfang"
    }
}

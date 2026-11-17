package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Local-only scatter plots; each mark is a stored observation and no samples are connected. */
class HistoryOverviewView(
    context: Context,
    private val load: (HistoryRange) -> HistorySnapshot
) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private var selectedRange = HistoryRange.DAY

    init { orientation = VERTICAL }

    fun refresh() {
        val snapshot = try { load(selectedRange) } catch (_: Exception) {
            showFailure()
            return
        }
        render(snapshot)
    }

    private fun render(snapshot: HistorySnapshot) {
        removeAllViews()
        addView(label("Verlauf · lokal gespeicherte Messpunkte", 12f, palette.muted),
            LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val rangeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        HistoryRange.entries.forEach { range ->
            val selected = range == selectedRange
            val button = label(range.label, 12f, if (selected) palette.card else palette.accent, true).apply {
                gravity = Gravity.CENTER
                minHeight = dp(48)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                isFocusable = true
                isSelected = selected
                contentDescription = "${range.label}${if (selected) ", ausgewählt" else ""}"
                background = GradientDrawable().apply {
                    setColor(if (selected) palette.primary else palette.tinted)
                    cornerRadius = dp(12).toFloat()
                }
                setOnClickListener {
                    if (selectedRange != range) {
                        selectedRange = range
                        refresh()
                    }
                }
            }
            rangeRow.addView(button, LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        }
        addView(rangeRow, LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        addView(label("Zeit = Empfang auf dem Telefon. Jeder Punkt ist ein gespeicherter Wert; Lücken bleiben leer, es gibt keine Verbindung oder Interpolation.",
            12f, palette.muted), LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        metricCard("Herzfrequenz", "bpm", snapshot.heartRate, snapshot, 240, palette.pulse)
        metricCard("Sauerstoffsättigung · SpO₂", "%", snapshot.oxygen, snapshot, 100, palette.oxygen)
    }

    private fun metricCard(
        title: String,
        unit: String,
        points: List<HistoryPoint>,
        snapshot: HistorySnapshot,
        axisMaximum: Int,
        accent: Int
    ) {
        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(palette.card)
                cornerRadius = dp(18).toFloat()
            }
            tag = "history_series_$title"
        }
        val heading = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label(title, 16f, palette.ink, true), LayoutParams(0, -2, 1f))
        heading.addView(TextView(context).apply {
            text = "ⓘ"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(palette.indicator)
            minWidth = dp(48)
            minHeight = dp(48)
            isClickable = true
            isFocusable = true
            contentDescription = "Quelle und Skala für $title anzeigen"
            setOnClickListener { showMetricInfo(title, unit) }
        }, LayoutParams(dp(48), dp(48)))
        card.addView(heading)
        card.addView(label("${points.size} Messpunkte · Skala 0–$axisMaximum $unit",
            12f, palette.muted), LayoutParams(-1, -2).apply { topMargin = dp(4) })
        if (points.isEmpty()) {
            card.addView(label("Keine Messdaten in diesem Zeitraum", 14f, palette.muted),
                LayoutParams(-1, -2).apply { topMargin = dp(18); bottomMargin = dp(18) })
        } else {
            card.addView(HistoryScatterChartView(context, points, snapshot.start, snapshot.end,
                axisMaximum, accent, snapshot.range), LayoutParams(-1, dp(210)))
        }
        addView(card, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    private fun showFailure() {
        removeAllViews()
        addView(label("Der lokale Verlauf konnte nicht geladen werden.", 14f, palette.muted))
    }

    private fun showMetricInfo(title: String, unit: String) {
        val profile = MetricReferenceProfile.forMetric(DashboardMetric(title, "Keine Messdaten", ""))
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(8))
        }
        fun row(symbol: String, text: String, url: String? = null) {
            content.addView(TextView(context).apply {
                this.text = "$symbol   $text"
                textSize = 14f
                setTextColor(if (url == null) palette.ink else palette.accent)
                setPadding(0, dp(5), 0, dp(5))
                if (url != null) {
                    isFocusable = true
                    isClickable = true
                    setOnClickListener {
                        try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url))) } catch (_: Exception) { }
                    }
                }
            })
        }
        row("▬", "Skala ${profile?.minimum ?: 0}–${profile?.maximum ?: if (unit == "%") 100 else 240} $unit")
        profile?.referenceMinimum?.let { low ->
            row("▰", "$low–${profile.referenceMaximum} $unit · ${profile.referenceLabel}")
        }
        profile?.sourceName?.let { row("↗", "Quelle $it", profile.sourceUrl) }
        row("⚠", profile?.caveat ?: "Keine validierte Norm für Einzelmesswerte.")
        row("◷", "Zeitstempel = Empfang auf dem Telefon.")
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("ⓘ  $title")
            .setView(content)
            .setPositiveButton("Schließen", null)
            .show()
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

private class HistoryScatterChartView(
    context: Context,
    private val points: List<HistoryPoint>,
    private val start: java.time.Instant,
    private val end: java.time.Instant,
    private val axisMaximum: Int,
    private val accent: Int,
    range: HistoryRange
) : View(context) {
    private val palette = AppPalette.of(context)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plot = RectF()
    private val timeFormatter = DateTimeFormatter.ofPattern(if (range == HistoryRange.DAY) "HH:mm" else "dd.MM")
        .withZone(ZoneId.systemDefault())

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "${points.size} tatsächliche Messpunkte. Vertikale Achse 0 bis $axisMaximum. " +
            "Horizontale Achse ${timeFormatter.format(start)} bis ${timeFormatter.format(end)}. " +
            "Messpunkte sind nicht verbunden; Zwischenwerte werden nicht dargestellt."
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val left = 42f * density
        val right = w - 8f * density
        val top = 12f * density
        val bottom = h - 31f * density
        if (right <= left || bottom <= top) return
        plot.set(left, top, right, bottom)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = palette.track
        for (i in 0..4) {
            val fraction = i / 4f
            val y = plot.bottom - fraction * plot.height()
            canvas.drawLine(plot.left, y, plot.right, y, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 10f * density
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = palette.muted
            canvas.drawText((axisMaximum * fraction).toInt().toString(), plot.left - 5f * density,
                y + 3.5f * density, paint)
            paint.style = Paint.Style.STROKE
            paint.color = palette.track
        }
        paint.style = Paint.Style.FILL
        paint.textSize = 10f * density
        paint.color = palette.muted
        val zone = ZoneId.systemDefault()
        canvas.drawText(timeFormatter.format(start.atZone(zone)), plot.left, h - 7f * density, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(timeFormatter.format(start.plusMillis(java.time.Duration.between(start, end).toMillis() / 2).atZone(zone)),
            plot.centerX(), h - 7f * density, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(timeFormatter.format(end.atZone(zone)), plot.right, h - 7f * density, paint)

        val span = java.time.Duration.between(start, end).toMillis().coerceAtLeast(1L)
        paint.color = accent
        points.forEach { point ->
            val xFraction = (java.time.Duration.between(start, point.receivedAt).toMillis().toDouble() / span)
                .toFloat().coerceIn(0f, 1f)
            val yFraction = (point.value.toFloat() / axisMaximum).coerceIn(0f, 1f)
            val x = plot.left + xFraction * plot.width()
            val y = plot.bottom - yFraction * plot.height()
            canvas.drawCircle(x, y, 3.2f * density, paint)
        }
    }
}

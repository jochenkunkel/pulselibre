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

/** Local-only summaries of stored observations; no missing windows are filled or connected. */
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
        val resolution = HistoryResolution.forRange(snapshot.range)
        addView(label("Zeit = Empfang auf dem Telefon. Werte werden je Zeitfenster als Median und beobachtete Spanne zusammengefasst; leere Fenster bleiben leer.",
            12f, palette.muted), LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        metricCard("Herzfrequenz", "bpm", snapshot.heartRate, snapshot, 240, palette.pulse, resolution)
        metricCard("Sauerstoffsättigung · SpO₂", "%", snapshot.oxygen, snapshot, 100, palette.oxygen, resolution)
    }

    private fun metricCard(
        title: String,
        unit: String,
        points: List<HistoryPoint>,
        snapshot: HistorySnapshot,
        axisMaximum: Int,
        accent: Int,
        resolution: HistoryResolution
    ) {
        val buckets = aggregateHistoryPoints(points, snapshot.start, snapshot.end, resolution)
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
        heading.addView(DashboardMetricIconView(context, title, accent), LayoutParams(dp(20), dp(20)).apply {
            marginEnd = dp(7)
        })
        heading.addView(label(title, 15f, palette.ink, true).apply { maxLines = 2 }, LayoutParams(0, -2, 1f))
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
        card.addView(label("${points.size} Werte · ${buckets.size} belegte Fenster · ${resolution.label} · Skala 0–$axisMaximum $unit",
            12f, palette.muted), LayoutParams(-1, -2).apply { topMargin = dp(4) })
        if (points.isEmpty()) {
            card.addView(label("Keine Messdaten in diesem Zeitraum", 14f, palette.muted),
                LayoutParams(-1, -2).apply { topMargin = dp(18); bottomMargin = dp(18) })
        } else {
            card.addView(HistoryScatterChartView(context, buckets, points.size, snapshot.start, snapshot.end,
                axisMaximum, accent, snapshot.range, resolution), LayoutParams(-1, dp(210)))
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
    private val buckets: List<HistoryBucket>,
    private val storedPointCount: Int,
    private val start: java.time.Instant,
    private val end: java.time.Instant,
    private val axisMaximum: Int,
    private val accent: Int,
    range: HistoryRange,
    private val resolution: HistoryResolution
) : View(context) {
    private val palette = AppPalette.of(context)
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plot = RectF()
    private val timeFormatter = DateTimeFormatter.ofPattern(if (range == HistoryRange.DAY) "HH:mm" else "dd.MM")
        .withZone(ZoneId.systemDefault())

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "$storedPointCount gespeicherte Messwerte in ${buckets.size} belegten Zeitfenstern, Auflösung ${resolution.label}. " +
            "Markierung ist der Median; Strich zeigt beobachtetes Minimum bis Maximum. Vertikale Achse 0 bis $axisMaximum. " +
            "Horizontale Achse ${timeFormatter.format(start)} bis ${timeFormatter.format(end)}. " +
            "Leere Fenster bleiben leer. Es gibt keine Verbindung oder Interpolation zwischen Fenstern."
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
        val zone = ZoneId.systemDefault()
        val span = java.time.Duration.between(start, end).toMillis().coerceAtLeast(1L)
        val bucketMillis = resolution.duration.toMillis()
        val tickCount = ((span + bucketMillis - 1L) / bucketMillis).toInt().coerceAtLeast(1)
        paint.style = Paint.Style.STROKE
        for (index in 0..tickCount) {
            val fraction = index.toFloat() / tickCount
            val x = plot.left + fraction * plot.width()
            val major = index % resolution.majorTickEvery == 0 || index == tickCount
            if (index > 0) {
                paint.color = palette.track
                paint.alpha = if (major) 150 else 65
                paint.strokeWidth = if (major) 1f * density else 0.7f * density
                canvas.drawLine(x, plot.top, x, plot.bottom, paint)
            }
            paint.alpha = 255
            paint.strokeWidth = 1f * density
            canvas.drawLine(x, plot.bottom, x, plot.bottom + (if (major) 4f else 2.5f) * density, paint)
            if (major) {
                val time = start.plusMillis(span * index / tickCount).atZone(zone)
                paint.style = Paint.Style.FILL
                paint.textSize = 10f * density
                paint.color = palette.muted
                paint.textAlign = when (index) {
                    0 -> Paint.Align.LEFT
                    tickCount -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(timeFormatter.format(time), x, h - 7f * density, paint)
                paint.style = Paint.Style.STROKE
            }
        }

        buckets.forEach { bucket ->
            val center = bucket.start.plusMillis(java.time.Duration.between(bucket.start, bucket.end).toMillis() / 2L)
            val xFraction = (java.time.Duration.between(start, center).toMillis().toDouble() / span)
                .toFloat().coerceIn(0f, 1f)
            val x = plot.left + xFraction * plot.width()
            val minY = plot.bottom - (bucket.minimum.toFloat() / axisMaximum).coerceIn(0f, 1f) * plot.height()
            val medianY = plot.bottom - (bucket.median.toFloat() / axisMaximum).coerceIn(0f, 1f) * plot.height()
            val maxY = plot.bottom - (bucket.maximum.toFloat() / axisMaximum).coerceIn(0f, 1f) * plot.height()
            paint.color = accent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.1f * density
            canvas.drawLine(x, maxY, x, minY, paint)
            canvas.drawLine(x - 1.5f * density, maxY, x + 1.5f * density, maxY, paint)
            canvas.drawLine(x - 1.5f * density, minY, x + 1.5f * density, minY, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, medianY, 2f * density, paint)
        }
    }
}

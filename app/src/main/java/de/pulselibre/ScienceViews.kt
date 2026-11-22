package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Time series with explicit quality: valid values as line/min-max strokes, LIMITED/UNVERIFIED as hollow circles, gaps hatched
 * and never bridged, INVALID/STATUS only as marks in a strip under the plot (never as values). Large series are drawn per pixel
 * column (min/max), so 43k points stay cheap.
 */
class ScienceSeriesView(context: Context, private val palette: AppPalette) : View(context) {
    private var series: SeriesData? = null
    private var assessment: SeriesAssessment? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val timeFormat = DateTimeFormatter.ofPattern("d.M. HH:mm").withZone(ZoneId.systemDefault())

    fun show(data: SeriesData) {
        series = data
        assessment = SeriesAnalysis.assess(data)
        contentDescription = ScienceText.describe(data, assessment!!)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        requestLayout(); invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (200 * density).toInt())

    override fun onDraw(canvas: Canvas) {
        val data = series ?: return
        val a = assessment ?: return
        val left = 40 * density; val right = width - 8 * density; val top = 8 * density
        val stripH = 14 * density
        val bottom = height - 22 * density - stripH
        val plot = RectF(left, top, right, bottom)
        val span = (data.rangeEndMs - data.rangeStartMs).coerceAtLeast(1L)
        fun x(t: Long) = plot.left + (t - data.rangeStartMs).toFloat() / span * plot.width()
        val shown = data.points.filter { it.timeMs != null && it.timeMs in data.rangeStartMs..data.rangeEndMs }
        val values = shown.filter { it.value != null && it.quality !in NON_VALUE }.map { it.value!! }
        val lo = values.minOrNull() ?: 0.0
        val hi = values.maxOrNull() ?: 1.0
        val pad = ((hi - lo) * 0.1).coerceAtLeast(1.0)
        val vMin = lo - pad; val vMax = hi + pad
        fun y(v: Double) = plot.bottom - ((v - vMin) / (vMax - vMin)).toFloat() * plot.height()
        // grid + value labels
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density; paint.color = palette.track
        paint.textSize = 10 * density; paint.typeface = Typeface.DEFAULT
        for (i in 0..3) {
            val gy = plot.top + plot.height() * i / 3f
            canvas.drawLine(plot.left, gy, plot.right, gy, paint)
            paint.style = Paint.Style.FILL; paint.color = palette.muted; paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(Math.round(vMax - (vMax - vMin) * i / 3.0).toString(), plot.left - 4 * density, gy + 3.5f * density, paint)
            paint.style = Paint.Style.STROKE; paint.color = palette.track
        }
        // gaps: hatched, labelled by the legend, never bridged
        paint.style = Paint.Style.FILL; paint.color = withAlpha(palette.muted, 40)
        a.gaps.forEach { g -> canvas.drawRect(x(g.startMs), plot.top, x(g.endMs), plot.bottom, paint) }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density; paint.color = withAlpha(palette.muted, 90)
        a.gaps.forEach { g ->
            canvas.save()
            canvas.clipRect(x(g.startMs), plot.top, x(g.endMs), plot.bottom)
            var hx = x(g.startMs) - plot.height()
            while (hx < x(g.endMs)) { canvas.drawLine(hx, plot.bottom, hx + plot.height(), plot.top, paint); hx += 8 * density }
            canvas.restore()
        }
        // drawn values per pixel column
        val cols = plot.width().toInt().coerceAtLeast(1)
        val minV = DoubleArray(cols) { Double.NaN }; val maxV = DoubleArray(cols) { Double.NaN }; val sum = DoubleArray(cols); val cnt = IntArray(cols)
        shown.filter { it.quality == SeriesQuality.VALID && it.value != null }.forEach { p ->
            val c = (((p.timeMs!! - data.rangeStartMs).toDouble() / span) * (cols - 1)).toInt().coerceIn(0, cols - 1)
            minV[c] = if (minV[c].isNaN()) p.value!! else minOf(minV[c], p.value!!)
            maxV[c] = if (maxV[c].isNaN()) p.value!! else maxOf(maxV[c], p.value!!)
            sum[c] += p.value!!; cnt[c]++
        }
        fun inGap(col: Int): Boolean { val t = data.rangeStartMs + span * col / cols.toDouble(); return a.gaps.any { t >= it.startMs && t <= it.endMs } }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.6f * density; paint.color = palette.pulse; paint.strokeJoin = Paint.Join.ROUND
        val path = Path(); var open = false
        for (c in 0 until cols) {
            if (cnt[c] == 0 || inGap(c)) { open = false; continue }
            val px = plot.left + c
            if (maxV[c] > minV[c]) canvas.drawLine(px, y(maxV[c]), px, y(minV[c]), paint)
            val py = y(sum[c] / cnt[c])
            if (!open) { path.moveTo(px, py); open = true } else path.lineTo(px, py)
        }
        canvas.drawPath(path, paint)
        // limited / unverified: hollow circles
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * density; paint.color = palette.oxygen
        shown.filter { (it.quality == SeriesQuality.LIMITED || it.quality == SeriesQuality.UNVERIFIED) && it.value != null }.forEach {
            canvas.drawCircle(x(it.timeMs!!), y(it.value!!), 3 * density, paint)
        }
        // invalid / status marks below the plot: x in warning colour, never a value
        val stripY = plot.bottom + stripH / 2 + 4 * density
        paint.color = Color.parseColor(if (palette.dark) "#FFB74D" else "#B26A00"); paint.strokeWidth = 1.4f * density
        shown.filter { it.quality == SeriesQuality.INVALID || it.quality == SeriesQuality.STATUS }.forEach {
            val cx = x(it.timeMs!!); val r = 3 * density
            canvas.drawLine(cx - r, stripY - r, cx + r, stripY + r, paint); canvas.drawLine(cx - r, stripY + r, cx + r, stripY - r, paint)
        }
        // time axis
        paint.style = Paint.Style.FILL; paint.color = palette.muted; paint.textSize = 10 * density; paint.textAlign = Paint.Align.CENTER
        for (i in 0..3) {
            val t = data.rangeStartMs + span * i / 3
            paint.textAlign = when (i) { 0 -> Paint.Align.LEFT; 3 -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            canvas.drawText(timeFormat.format(Instant.ofEpochMilli(t)), x(t), height - 4 * density, paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    private companion object { val NON_VALUE = setOf(SeriesQuality.INVALID, SeriesQuality.STATUS) }
}

/** Sleep intervals as a bar: one colour per phase, gaps hatched ("no data", not awake), interruptions marked. */
class SleepTimelineView(context: Context, private val palette: AppPalette) : View(context) {
    private var intervals: List<SleepInterval> = emptyList()
    private var assessment: SleepAssessment? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun show(data: List<SleepInterval>) {
        intervals = data
        assessment = SleepAnalysis.assess(data)
        contentDescription = ScienceText.describeSleep(assessment!!)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        requestLayout(); invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (96 * density).toInt())

    private fun phaseColor(p: SleepPhase) = when (p) {
        SleepPhase.AWAKE -> Color.parseColor(if (palette.dark) "#FFB74D" else "#B26A00")
        SleepPhase.LIGHT -> palette.oxygen
        SleepPhase.DEEP -> palette.primary
        SleepPhase.REM -> palette.indicator
        SleepPhase.UNKNOWN -> palette.muted
    }

    override fun onDraw(canvas: Canvas) {
        val a = assessment ?: return
        val start = a.spanStartMs ?: return
        val end = a.spanEndMs ?: return
        val span = (end - start).coerceAtLeast(1L)
        val left = 8 * density; val right = width - 8 * density
        fun x(t: Long) = left + (t - start).toFloat() / span * (right - left)
        val top = 8 * density; val bar = 40 * density
        paint.style = Paint.Style.FILL
        a.gaps.forEach { g -> paint.color = withAlpha(palette.muted, 50); canvas.drawRect(x(g.startMs), top, x(g.endMs), top + bar, paint) }
        intervals.filter { it.endMs > it.startMs }.forEach { i ->
            val h = if (i.phase == SleepPhase.AWAKE) bar * 0.45f else bar
            paint.color = phaseColor(i.phase)
            canvas.drawRect(x(i.startMs), top + (bar - h), x(i.endMs), top + bar, paint)
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density; paint.color = withAlpha(palette.muted, 120)
        a.gaps.forEach { g -> canvas.drawRect(x(g.startMs), top, x(g.endMs), top + bar, paint) }
        paint.style = Paint.Style.FILL; paint.color = palette.muted; paint.textSize = 10 * density
        paint.textAlign = Paint.Align.LEFT; canvas.drawText(timeFormat.format(Instant.ofEpochMilli(start)), left, top + bar + 14 * density, paint)
        paint.textAlign = Paint.Align.RIGHT; canvas.drawText(timeFormat.format(Instant.ofEpochMilli(end)), right, top + bar + 14 * density, paint)
        // legend
        var lx = left; val ly = top + bar + 32 * density
        listOf(SleepPhase.AWAKE to "wach", SleepPhase.LIGHT to "leicht", SleepPhase.DEEP to "tief", SleepPhase.REM to "REM").forEach { (p, label) ->
            paint.color = phaseColor(p); canvas.drawRect(lx, ly - 8 * density, lx + 8 * density, ly, paint)
            paint.color = palette.muted; paint.textAlign = Paint.Align.LEFT; canvas.drawText(label, lx + 11 * density, ly, paint)
            lx += (paint.measureText(label) + 26 * density)
        }
    }

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

/** Plain-language summaries for captions and TalkBack; they state facts (counts, gaps), never interpretations. */
object ScienceText {
    private val HM = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    private fun minutes(ms: Long) = ms / 60_000L

    fun describe(data: SeriesData, a: SeriesAssessment): String {
        val head = "${data.metricKey} (${data.unit}), Quelle ${data.origin.name.lowercase()}"
        return when (a.state) {
            SeriesState.EMPTY -> "$head: keine Daten im gewählten Zeitraum."
            SeriesState.NO_VALID_VALUES -> "$head: ${a.total} Einträge, aber keine gültigen Messwerte (${a.invalid} ungültig, ${a.status} Gerätestatus)."
            else -> buildString {
                append("$head: ${a.valid} gültige Werte")
                if (a.mean != null) append(", Mittel ${"%.1f".format(a.mean)}, Streuung ${"%.1f".format(a.standardDeviation ?: 0.0)}")
                if (a.limited > 0) append(", ${a.limited} mit eingeschränkter Qualität")
                if (a.unverified > 0) append(", ${a.unverified} ungeprüft")
                if (a.invalid > 0) append(", ${a.invalid} ungültig (nicht gezeichnet)")
                if (a.status > 0) append(", ${a.status} Gerätestatus (nicht gezeichnet)")
                if (a.unknownTime > 0) append(", ${a.unknownTime} ohne Zeitstempel")
                append(". Abdeckung ${Math.round(a.coverage * 100)} %")
                if (a.gaps.isNotEmpty()) append(", ${a.gaps.size} Lücken (nicht aufgefüllt)")
                append('.')
            }
        }
    }

    fun describeSleep(a: SleepAssessment): String {
        val start = a.spanStartMs ?: return "Keine Schlafintervalle."
        val end = a.spanEndMs ?: return "Keine Schlafintervalle."
        val phases = a.phaseMs.entries.joinToString(", ") { "${it.key.name.lowercase()} ${minutes(it.value)} min" }
        return "Schlaf ${HM.format(Instant.ofEpochMilli(start))} bis ${HM.format(Instant.ofEpochMilli(end))}: $phases; " +
            "${a.interruptions.size} Unterbrechungen, ${a.gaps.size} Datenlücken (kein Messwert, nicht gleich wach)" +
            (if (a.overlaps > 0) ", ${a.overlaps} überlappende Intervalle (Datenfehler)" else "") + "."
    }
}

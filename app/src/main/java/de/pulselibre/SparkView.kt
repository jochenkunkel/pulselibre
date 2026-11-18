package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** 24-hour sparkline of a tile: hourly values, gaps stay gaps, a "now" marker on the current hour. Decorative (the text carries the data). */
internal class SparkView(context: Context, private val color: Int, private val track: Int) : View(context) {
    private val d = resources.displayMetrics.density
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()
    private var values: List<Double?> = emptyList()
    private var nowHour = -1

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun show(hourly: List<Double?>, currentHour: Int) { values = hourly; nowHour = currentHour; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val present = values.filterNotNull()
        val w = width.toFloat(); val h = height.toFloat()
        grid.color = track; grid.strokeWidth = d
        canvas.drawLine(0f, h - d, w, h - d, grid)
        if (present.isEmpty() || values.size < 2) return
        val lo = present.min(); val hi = present.max()
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        val padTop = 3 * d; val padBottom = 4 * d
        fun x(i: Int) = i * (w / (values.size - 1))
        fun y(v: Double) = (h - padBottom - ((v - lo) / span).toFloat() * (h - padTop - padBottom))
        line.color = color; line.strokeWidth = 2f * d
        fill.color = color; fill.alpha = 38
        var segment: MutableList<Int> = mutableListOf()
        fun flush() {
            if (segment.size >= 2) {
                path.reset(); path.moveTo(x(segment[0]), y(values[segment[0]]!!))
                for (k in 1 until segment.size) path.lineTo(x(segment[k]), y(values[segment[k]]!!))
                canvas.drawPath(path, line)
                path.lineTo(x(segment.last()), h - padBottom); path.lineTo(x(segment[0]), h - padBottom); path.close()
                canvas.drawPath(path, fill)
            } else if (segment.size == 1) { line.style = Paint.Style.FILL; canvas.drawCircle(x(segment[0]), y(values[segment[0]]!!), 2.2f * d, line); line.style = Paint.Style.STROKE }
            segment = mutableListOf()
        }
        values.forEachIndexed { i, v -> if (v == null) flush() else segment.add(i) }
        flush()
        if (nowHour in values.indices) { // marker of the current hour
            grid.color = color; grid.alpha = 120
            canvas.drawLine(x(nowHour), padTop, x(nowHour), h - padBottom, grid)
            grid.alpha = 255
        }
    }
}

package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import java.text.NumberFormat
import java.util.Locale

/** Plain scatter plot of real paired observations; deliberately no fitted line (#45). */
internal class ScatterView(
    context: Context,
    private val points: List<Pair<Double, Double>>,
    private val xLabel: String,
    private val yLabel: String,
    private val ink: Int,
    private val dot: Int,
    private val grid: Int
) : View(context) {
    private val d = resources.displayMetrics.density
    private val nf = NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 0 }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        contentDescription = "Streudiagramm $xLabel gegen $yLabel mit ${points.size} Wertepaaren. " +
            "Wertebereich ${xLabel} ${nf.format(points.minOf { it.first })} bis ${nf.format(points.maxOf { it.first })}, " +
            "${yLabel} ${nf.format(points.minOf { it.second })} bis ${nf.format(points.maxOf { it.second })}."
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        setMeasuredDimension(width, (260 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (points.isEmpty()) return
        val left = 48 * d; val right = width - 12 * d; val top = 12 * d; val bottom = height - 40 * d
        val xmin = points.minOf { it.first }; val xmax = points.maxOf { it.first }.let { if (it == xmin) it + 1 else it }
        val ymin = points.minOf { it.second }; val ymax = points.maxOf { it.second }.let { if (it == ymin) it + 1 else it }
        paint.color = grid; paint.strokeWidth = 1 * d
        canvas.drawLine(left, bottom, right, bottom, paint); canvas.drawLine(left, top, left, bottom, paint)
        paint.color = ink; paint.textSize = 11 * d
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(nf.format(ymax), left - 4 * d, top + 10 * d, paint)
        canvas.drawText(nf.format(ymin), left - 4 * d, bottom, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(nf.format(xmin), left, bottom + 14 * d, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(nf.format(xmax), right, bottom + 14 * d, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(xLabel, (left + right) / 2, height - 6 * d, paint)
        canvas.save(); canvas.rotate(-90f, 12 * d, (top + bottom) / 2)
        canvas.drawText(yLabel, 12 * d, (top + bottom) / 2, paint); canvas.restore()
        paint.color = dot; paint.alpha = 170
        for ((x, y) in points) {
            canvas.drawCircle(left + ((x - xmin) / (xmax - xmin)).toFloat() * (right - left),
                bottom - ((y - ymin) / (ymax - ymin)).toFloat() * (bottom - top), 3.5f * d, paint)
        }
        paint.alpha = 255
    }
}

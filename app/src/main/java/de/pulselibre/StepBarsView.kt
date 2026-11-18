package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * Decorative background of the step tile: one faint vertical bar per 15-minute interval, time left to right.
 * No axes, numbers or legend; unobserved intervals draw nothing. The tile itself carries the accessible summary.
 */
internal class StepBarsView(context: Context, private val color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 40 } // ≈ 16 %
    private val rect = RectF()
    private var series: StepIntervals.Series? = null

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun show(value: StepIntervals.Series?) { series = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val s = series ?: return
        val n = s.values.size
        val max = s.max
        if (n == 0 || max <= 0.0 || width <= 0 || height <= 0) return
        val slot = width.toFloat() / n
        val bar = slot * 0.72f
        val radius = bar / 2f
        val maxHeight = height * 0.55f
        val base = height - height * 0.30f      // bars grow from a baseline above the goal bar
        s.values.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed   // gap: no data, draw nothing
            val h = if (v <= 0.0) bar else maxOf(bar, (v / max).toFloat() * maxHeight)   // zero activity = small dot
            val left = i * slot + (slot - bar) / 2f
            rect.set(left, base - h, left + bar, base)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }
}

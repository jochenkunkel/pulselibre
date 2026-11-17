package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Small theme-tinted vector symbols for legends, information rows, and navigation. */
internal class DashboardInfoIconView(
    context: Context,
    private val kind: Kind,
    private val tint: Int
) : View(context) {
    enum class Kind { TIME, RANGE, WINDOW, SOURCE, SCALE, LIMIT, HOME, HISTORY, DEVICES, LAB, DATA }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tint
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = minOf(width, height).toFloat()
        if (side <= 0f) return
        canvas.save()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / 24f, side / 24f)
        when (kind) {
            Kind.TIME -> clock(canvas)
            Kind.RANGE -> range(canvas)
            Kind.WINDOW -> window(canvas)
            Kind.SOURCE -> source(canvas)
            Kind.SCALE -> scale(canvas)
            Kind.LIMIT -> limit(canvas)
            Kind.HOME -> home(canvas)
            Kind.HISTORY -> history(canvas)
            Kind.DEVICES -> devices(canvas)
            Kind.LAB -> lab(canvas)
            Kind.DATA -> data(canvas)
        }
        canvas.restore()
    }

    private fun clock(canvas: Canvas) {
        canvas.drawCircle(12f, 12f, 9f, paint)
        canvas.drawLine(12f, 6f, 12f, 12f, paint)
        canvas.drawLine(12f, 12f, 16.5f, 14.5f, paint)
    }

    private fun range(canvas: Canvas) {
        canvas.drawLine(4f, 12f, 20f, 12f, paint)
        canvas.drawLine(4f, 8f, 4f, 16f, paint)
        canvas.drawLine(20f, 8f, 20f, 16f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(12f, 12f, 2.2f, paint)
    }

    private fun window(canvas: Canvas) {
        rect.set(4f, 4f, 20f, 20f)
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        canvas.drawLine(7f, 17f, 17f, 7f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(7f, 17f, 1.4f, paint)
        canvas.drawCircle(17f, 7f, 1.4f, paint)
    }

    private fun source(canvas: Canvas) {
        rect.set(3.5f, 10f, 14f, 20.5f)
        canvas.drawRoundRect(rect, 1.5f, 1.5f, paint)
        canvas.drawLine(10f, 14f, 20f, 4f, paint)
        canvas.drawLine(14f, 4f, 20f, 4f, paint)
        canvas.drawLine(20f, 4f, 20f, 10f, paint)
    }

    private fun scale(canvas: Canvas) {
        canvas.drawLine(3f, 8f, 21f, 8f, paint)
        canvas.drawLine(3f, 8f, 3f, 18f, paint)
        canvas.drawLine(21f, 8f, 21f, 18f, paint)
        for (x in 7..17 step 5) canvas.drawLine(x.toFloat(), 8f, x.toFloat(), 13f, paint)
        canvas.drawLine(3f, 18f, 21f, 18f, paint)
    }

    private fun limit(canvas: Canvas) {
        canvas.drawPath(Path().apply {
            moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close()
        }, paint)
        canvas.drawLine(12f, 9f, 12f, 14f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(12f, 17f, 1f, paint)
    }

    private fun home(canvas: Canvas) {
        canvas.drawPath(Path().apply {
            moveTo(3f, 11f); lineTo(12f, 4f); lineTo(21f, 11f)
            lineTo(19f, 11f); lineTo(19f, 20f); lineTo(5f, 20f); lineTo(5f, 11f); close()
        }, paint)
        canvas.drawRect(10f, 14f, 14f, 20f, paint)
    }

    private fun history(canvas: Canvas) {
        canvas.drawArc(4f, 4f, 20f, 20f, 45f, 285f, false, paint)
        canvas.drawPath(Path().apply {
            moveTo(4f, 4f); lineTo(4f, 10f); lineTo(10f, 10f)
            moveTo(7f, 15f); lineTo(10f, 12f); lineTo(13f, 14f); lineTo(17f, 9f)
        }, paint)
    }

    private fun devices(canvas: Canvas) {
        rect.set(3f, 5f, 13f, 19f)
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        canvas.drawLine(6f, 8f, 10f, 8f, paint)
        canvas.drawCircle(8f, 16.5f, 0.8f, paint)
        rect.set(15f, 8f, 21f, 16f)
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        canvas.drawLine(16.5f, 18.5f, 19.5f, 18.5f, paint)
    }

    private fun lab(canvas: Canvas) {
        canvas.drawLine(9f, 3f, 15f, 3f, paint)
        canvas.drawLine(10f, 3f, 10f, 10f, paint)
        canvas.drawLine(14f, 3f, 14f, 10f, paint)
        canvas.drawPath(Path().apply {
            moveTo(10f, 9f); lineTo(4f, 19f); quadTo(4f, 21f, 7f, 21f)
            lineTo(17f, 21f); quadTo(20f, 21f, 20f, 19f); lineTo(14f, 9f)
        }, paint)
        canvas.drawLine(7f, 16f, 17f, 16f, paint)
    }

    private fun data(canvas: Canvas) {
        canvas.drawOval(4f, 4f, 20f, 10f, paint)
        canvas.drawLine(4f, 7f, 4f, 18f, paint)
        canvas.drawLine(20f, 7f, 20f, 18f, paint)
        canvas.drawArc(4f, 12f, 20f, 18f, 0f, 180f, false, paint)
        canvas.drawArc(4f, 15f, 20f, 21f, 0f, 180f, false, paint)
    }
}

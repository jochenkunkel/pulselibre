package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Small decorative, theme-colored glyphs that distinguish dashboard measurements. */
class DashboardMetricIconView(
    context: Context,
    title: String,
    color: Int
) : View(context) {
    private enum class Kind { PULSE, OXYGEN, SLEEP, STEPS, GOAL, CALORIES, ACTIVITY, RR, STRESS, BATTERY, BREATH, GENERIC }

    private val kind = when {
        title.contains("Herzfrequenz", true) || title.contains("Ruhepuls", true) -> Kind.PULSE
        title.contains("SpO₂", true) || title.contains("Sauerstoff", true) -> Kind.OXYGEN
        title.contains("Schlaf", true) -> Kind.SLEEP
        title.contains("Schrittziel", true) || title.contains("Tagesziel", true) -> Kind.GOAL
        title.contains("Schritte", true) -> Kind.STEPS
        title.contains("Kalorien", true) || title.contains("Energie", true) -> Kind.CALORIES
        title.contains("Intensitätsminuten", true) || title.contains("Aktivität", true) -> Kind.ACTIVITY
        title.contains("RR-Intervall", true) || title.contains("HRV", true) -> Kind.RR
        title.contains("Stress", true) -> Kind.STRESS
        title.contains("Body Battery", true) -> Kind.BATTERY
        title.contains("Atem", true) -> Kind.BREATH
        else -> Kind.GENERIC
    }
    private val accentColor = color
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = accentColor
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
            Kind.PULSE -> pulse(canvas)
            Kind.OXYGEN -> oxygen(canvas)
            Kind.SLEEP -> sleep(canvas)
            Kind.STEPS -> steps(canvas)
            Kind.GOAL -> goal(canvas)
            Kind.CALORIES -> calories(canvas)
            Kind.ACTIVITY -> activity(canvas)
            Kind.RR -> rr(canvas)
            Kind.STRESS -> stress(canvas)
            Kind.BATTERY -> battery(canvas)
            Kind.BREATH -> breath(canvas)
            Kind.GENERIC -> generic(canvas)
        }
        canvas.restore()
    }

    private fun pulse(canvas: Canvas) {
        val path = Path().apply {
            moveTo(2f, 12f); lineTo(7f, 12f); lineTo(9.5f, 7f); lineTo(13f, 17f)
            lineTo(16f, 9f); lineTo(18f, 12f); lineTo(22f, 12f)
        }
        canvas.drawPath(path, paint)
    }

    private fun oxygen(canvas: Canvas) {
        canvas.drawCircle(12f, 12f, 9f, paint)
        drawGlyph(canvas, "%", 12f, 16f, 10f)
    }

    private fun sleep(canvas: Canvas) {
        canvas.drawCircle(11f, 12f, 8.5f, paint)
        paint.style = Paint.Style.FILL
        paint.color = AppPalette.of(context).card
        canvas.drawCircle(15.5f, 8.5f, 7f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = accentColor
        canvas.drawPath(Path().apply {
            moveTo(17.5f, 16f); lineTo(18.4f, 18f); lineTo(20.5f, 18.2f)
            lineTo(19f, 19.6f); lineTo(19.4f, 21.5f); lineTo(17.5f, 20.5f)
            lineTo(15.8f, 21.5f); lineTo(16.1f, 19.6f); lineTo(14.5f, 18.2f)
            lineTo(16.6f, 18f); close()
        }, paint)
    }

    private fun steps(canvas: Canvas) {
        canvas.save(); canvas.rotate(-25f, 8f, 15f)
        canvas.drawOval(5f, 10f, 10f, 19f, paint)
        canvas.drawCircle(5.3f, 8.5f, 1.4f, paint)
        canvas.drawCircle(7.8f, 7.7f, 1.35f, paint)
        canvas.drawCircle(10.1f, 8.2f, 1.15f, paint)
        canvas.restore()
        canvas.save(); canvas.rotate(25f, 17f, 9f)
        canvas.drawOval(14f, 5f, 19f, 14f, paint)
        canvas.drawCircle(14.2f, 3.5f, 1.35f, paint)
        canvas.drawCircle(16.6f, 2.8f, 1.3f, paint)
        canvas.drawCircle(18.9f, 3.5f, 1.1f, paint)
        canvas.restore()
    }

    private fun goal(canvas: Canvas) {
        canvas.drawCircle(12f, 12f, 9f, paint)
        canvas.drawCircle(12f, 12f, 5.5f, paint)
        canvas.drawCircle(12f, 12f, 1.7f, paint)
        canvas.drawLine(12f, 12f, 20f, 4f, paint)
    }

    private fun calories(canvas: Canvas) {
        val path = Path().apply {
            moveTo(12f, 2.5f); cubicTo(14f, 7f, 20.5f, 9.5f, 19f, 16f)
            cubicTo(18f, 20f, 15f, 22f, 11.5f, 21.5f); cubicTo(6f, 21f, 3.5f, 17f, 5f, 12f)
            cubicTo(6f, 8.5f, 9f, 7f, 9f, 4f); cubicTo(11f, 6f, 12f, 8f, 11.5f, 11f)
            cubicTo(14f, 9f, 14f, 6f, 12f, 2.5f); close()
        }
        canvas.drawPath(path, paint)
    }

    private fun activity(canvas: Canvas) {
        canvas.drawCircle(12f, 13f, 8.5f, paint)
        canvas.drawLine(9f, 2.5f, 15f, 2.5f, paint)
        canvas.drawLine(12f, 2.5f, 12f, 4.5f, paint)
        canvas.drawLine(12f, 13f, 12f, 8f, paint)
        canvas.drawLine(12f, 13f, 16f, 15f, paint)
    }

    private fun rr(canvas: Canvas) {
        canvas.drawPath(Path().apply {
            moveTo(2f, 12f); lineTo(6f, 12f); lineTo(8.5f, 8f); lineTo(11f, 16f)
            lineTo(14f, 6f); lineTo(16.5f, 12f); lineTo(22f, 12f)
        }, paint)
        canvas.drawCircle(4f, 5f, 1.1f, paint)
        canvas.drawCircle(20f, 19f, 1.1f, paint)
    }

    private fun stress(canvas: Canvas) {
        canvas.drawLine(3f, 6f, 21f, 6f, paint)
        canvas.drawLine(3f, 12f, 21f, 12f, paint)
        canvas.drawLine(3f, 18f, 21f, 18f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(9f, 6f, 2.2f, paint)
        canvas.drawCircle(15f, 12f, 2.2f, paint)
        canvas.drawCircle(7f, 18f, 2.2f, paint)
    }

    private fun battery(canvas: Canvas) {
        rect.set(3f, 6f, 20f, 18f)
        canvas.drawRoundRect(rect, 2f, 2f, paint)
        canvas.drawRect(20f, 10f, 22f, 14f, paint)
        paint.style = Paint.Style.FILL
        rect.set(5.5f, 8.5f, 13.5f, 15.5f)
        canvas.drawRoundRect(rect, 1f, 1f, paint)
    }

    private fun breath(canvas: Canvas) {
        canvas.drawLine(12f, 3f, 12f, 10f, paint)
        canvas.drawPath(Path().apply {
            moveTo(11f, 8f); cubicTo(8f, 7f, 4f, 9f, 3.5f, 14f)
            cubicTo(3f, 19f, 6f, 21f, 9f, 19f); cubicTo(11f, 17f, 11f, 13f, 12f, 11f)
            moveTo(13f, 8f); cubicTo(16f, 7f, 20f, 9f, 20.5f, 14f)
            cubicTo(21f, 19f, 18f, 21f, 15f, 19f); cubicTo(13f, 17f, 13f, 13f, 12f, 11f)
        }, paint)
    }

    private fun generic(canvas: Canvas) {
        canvas.drawCircle(6f, 17f, 2f, paint)
        canvas.drawCircle(12f, 11f, 2f, paint)
        canvas.drawCircle(18f, 6f, 2f, paint)
        canvas.drawLine(6f, 17f, 12f, 11f, paint)
        canvas.drawLine(12f, 11f, 18f, 6f, paint)
    }

    private fun drawGlyph(canvas: Canvas, glyph: String, x: Float, y: Float, size: Float) {
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        val baseline = y - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(glyph, x, baseline, paint)
        paint.style = Paint.Style.STROKE
    }
}

package de.pulselibre

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Native scalable instrument, drawn from one observed pulse; never generates measurements. */
class HeartRateGaugeView(context: Context) : View(context) {
    private val colors = AppPalette.of(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var reading = HeartRateGaugeModel(null)
    private var mode = PulseContext.UNKNOWN
    private var shownFraction = 0f
    private var animator: ValueAnimator? = null

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    fun show(model: HeartRateGaugeModel, context: PulseContext, from: Float? = null) {
        animator?.cancel()
        if (from != null) shownFraction = from
        reading = model
        mode = context
        contentDescription = "Herzfrequenz: ${model.bpm?.let { "$it bpm" } ?: "Keine Messdaten"}. " +
            "${context.label}. ${if (context == PulseContext.UNKNOWN) "Kein Aktivitätsmodus erkannt." else "Modus manuell gewählt."} ${model.scaleNote}"
        val target = model.fraction ?: 0f
        if (isAttachedToWindow && model.bpm != null && ValueAnimator.areAnimatorsEnabled()) {
            animator = ValueAnimator.ofFloat(shownFraction, target).apply {
                duration = 400
                addUpdateListener { shownFraction = it.animatedValue as Float; invalidate() }
                start()
            }
        } else { shownFraction = target; invalidate() }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize((w * 0.98f).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2
        val cy = h * 0.45f
        val radius = w * 0.39f
        val arc = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        fun point(angle: Float, r: Float): Pair<Float, Float> {
            val radians = Math.toRadians(angle.toDouble())
            return cx + cos(radians).toFloat() * r to cy + sin(radians).toFloat() * r
        }
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = w * 0.038f
        paint.color = colors.track
        canvas.drawArc(arc, 135f, 270f, false, paint)
        // Teal is a measured-value progress arc, NOT a normal range or prescribed target.
        if (reading.bpm != null) {
            paint.color = colors.indicator
            canvas.drawArc(arc, 135f, shownFraction * 270f, false, paint)
        }
        for (i in 0..70) {
            val angle = 135f + i / 70f * 270f
            val major = i % 10 == 0
            val (x1, y1) = point(angle, radius - w * (if (major) 0.035f else 0.015f))
            val (x2, y2) = point(angle, radius + w * 0.012f)
            paint.color = if (major) colors.ink else colors.muted
            paint.strokeWidth = if (major) w * 0.005f else w * 0.002f
            canvas.drawLine(x1, y1, x2, y2, paint)
            if (major) {
                val (tx, ty) = point(angle, radius + w * 0.065f)
                text(canvas, (40 + i * 2).toString(), tx, ty, w * 0.038f, colors.muted)
            }
        }
        val pivotY = h * 0.84f
        if (reading.bpm != null) {
            val (tx, ty) = point(135f + shownFraction * 270f, radius - w * 0.015f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * 0.009f
            paint.color = colors.ink
            canvas.drawLine(cx, pivotY, tx, ty, paint)
            paint.style = Paint.Style.FILL
            paint.color = colors.accent
            canvas.drawCircle(cx, pivotY, w * 0.024f, paint)
        }
        // Keep central labels legible when the needle points directly behind them.
        paint.style = Paint.Style.FILL
        paint.color = colors.card
        canvas.drawRoundRect(RectF(w * 0.24f, h * 0.25f, w * 0.76f, h * 0.67f), w * 0.05f, w * 0.05f, paint)
        drawModeIcon(canvas, cx, h * 0.29f, w * 0.02f)
        text(canvas, mode.label, cx, h * 0.40f, w * 0.043f, colors.muted, true)
        text(canvas, reading.bpm?.toString() ?: "—", cx, h * 0.54f, w * 0.18f, colors.ink, true)
        text(canvas, if (reading.bpm == null) "Keine Messdaten" else "bpm", cx, h * 0.64f, w * 0.045f, colors.muted)
    }

    private fun drawModeIcon(canvas: Canvas, x: Float, y: Float, unit: Float) {
        if (mode == PulseContext.UNKNOWN) {
            text(canvas, "?", x, y, unit * 3, colors.accent, true)
            return
        }
        paint.color = colors.accent
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * 0.3f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawCircle(x, y - unit * 1.8f, unit * 0.65f, paint)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            canvas.drawLine(x + x1 * unit, y + y1 * unit, x + x2 * unit, y + y2 * unit, paint)
        if (mode == PulseContext.REST) {
            line(0f, -0.8f, 0f, 1f)
            line(0f, 1f, 1.8f, 1f)
            line(1.8f, 1f, 2.2f, 2.4f)
            line(-0.9f, 0f, -0.9f, 1.8f)
            line(-0.9f, 1.8f, 1.2f, 1.8f)
            line(-0.9f, 1.8f, -0.9f, 2.5f)
        } else {
            val lean = if (mode == PulseContext.SPORT) 0.6f else 0f
            line(lean, -0.8f, -0.4f, 1f)
            line(-0.1f, 0f, -1.3f, 0.5f)
            line(-0.1f, 0f, 1.2f, 0.5f)
            line(-0.4f, 1f, -1.5f, 2.5f)
            line(-0.4f, 1f, 1.2f, 2.2f)
        }
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x, y - (paint.ascent() + paint.descent()) / 2, paint)
    }
}

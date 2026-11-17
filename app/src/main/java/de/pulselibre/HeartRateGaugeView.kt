package de.pulselibre

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Compact instrument face, drawn from an observed pulse and an explicitly estimated step context. */
class HeartRateGaugeView(context: Context) : View(context) {
    private val colors = AppPalette.of(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arc = RectF()
    private var reading = HeartRateGaugeModel(null)
    private var mode = PulseContext.UNKNOWN
    private var shownFraction = 0f
    private var animator: ValueAnimator? = null

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    fun show(model: HeartRateGaugeModel, activity: StepActivityEstimate, from: Float? = null) {
        animator?.cancel()
        if (from != null) shownFraction = from
        reading = model
        mode = activity.context
        contentDescription = "Herzfrequenz: ${model.bpm?.let { "$it bpm" } ?: "Keine Messdaten"}. " +
            "${activity.context.label}. ${activity.explanation}. " +
            "Modus heuristisch aus Schrittzähler abgeleitet, kein Nachweis von Sitzen oder Sport. " +
            "Anzeigeskala 40 bis 180 bpm, keine medizinischen Zonen. ${model.scaleNote}"
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
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize((w * 1.12f).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2
        val cy = h * 0.29f
        val radius = w * 0.31f
        arc.set(cx - radius, cy - radius, cx + radius, cy + radius)
        fun point(angle: Float, r: Float): Pair<Float, Float> {
            val radians = Math.toRadians(angle.toDouble())
            return cx + cos(radians).toFloat() * r to cy + sin(radians).toFloat() * r
        }

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = w * 0.045f
        paint.color = colors.track
        paint.shader = null
        drawArcRotated(canvas, arc, 0f, HeartRateGaugeModel.ARC_SWEEP_DEGREES,
            cx, cy, HeartRateGaugeModel.ARC_START_DEGREES, paint)

        // Continuous green→yellow→orange→red display gradient; colors are not medical bands.
        val ramp = intArrayOf(0xFF26A879.toInt(), 0xFFE4C441.toInt(), 0xFFE78A31.toInt(), 0xFFD84B52.toInt())
        val segments = 120
        for (i in 0 until segments) {
            paint.color = rampColor(ramp, i.toFloat() / (segments - 1))
            drawArcRotated(canvas, arc,
                i * HeartRateGaugeModel.ARC_SWEEP_DEGREES / segments,
                (HeartRateGaugeModel.ARC_SWEEP_DEGREES + 0.8f) / segments,
                cx, cy, HeartRateGaugeModel.ARC_START_DEGREES, paint)
        }

        for (i in 0..70) {
            val angle = HeartRateGaugeModel.ARC_START_DEGREES +
                i / 70f * HeartRateGaugeModel.ARC_SWEEP_DEGREES
            val major = i % 10 == 0
            val (x1, y1) = point(angle, radius - w * (if (major) 0.045f else 0.020f))
            val (x2, y2) = point(angle, radius + w * 0.015f)
            paint.color = if (major) colors.ink else colors.muted
            paint.strokeWidth = if (major) w * 0.006f else w * 0.0025f
            canvas.drawLine(x1, y1, x2, y2, paint)
            if (major) {
                val (tx, ty) = point(angle, radius + w * 0.085f)
                drawText(canvas, (40 + i * 2).toString(), tx, ty, w * 0.043f, colors.muted)
            }
        }

        // The pivot sits on the chord through both arc endpoints, like a classic semicircular dial.
        val pivotY = cy + HeartRateGaugeModel.pivotOffsetFromCircleCenter(radius)
        if (reading.bpm != null) {
            val (tipX, tipY) = point(HeartRateGaugeModel.needleAngle(shownFraction), radius - w * 0.025f)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = w * 0.012f
            paint.color = colors.ink
            canvas.drawLine(cx, pivotY, tipX, tipY, paint)
            paint.style = Paint.Style.FILL
            paint.color = colors.accent
            canvas.drawCircle(cx, pivotY, w * 0.027f, paint)
        }
        drawText(canvas, mode.label, cx, h * 0.63f, w * 0.062f, colors.muted, bold = true)
        drawText(canvas, reading.bpm?.toString() ?: "—", cx, h * 0.79f, w * 0.19f, colors.ink, bold = true)
        drawText(canvas, if (reading.bpm == null) "Keine Messdaten" else "bpm",
            cx, h * 0.9f, w * 0.052f, colors.muted)
    }

    private fun drawArcRotated(canvas: Canvas, bounds: RectF, start: Float, sweep: Float,
                               cx: Float, cy: Float, rotation: Float, targetPaint: Paint) {
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawArc(bounds, start, sweep, false, targetPaint)
        canvas.restore()
    }

    private fun rampColor(stops: IntArray, fraction: Float): Int {
        val position = fraction.coerceIn(0f, 1f) * (stops.size - 1)
        val index = position.toInt().coerceAtMost(stops.size - 2)
        val part = position - index
        fun channel(shift: Int) = (((stops[index] shr shift) and 0xFF) * (1f - part) +
            ((stops[index + 1] shr shift) and 0xFF) * part).toInt()
        return android.graphics.Color.rgb(channel(16), channel(8), channel(0))
    }

    private fun drawText(canvas: Canvas, value: String, x: Float, y: Float, size: Float,
                         color: Int, bold: Boolean = false) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.typeface = android.graphics.Typeface.create("sans-serif",
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        val baseline = y - (paint.ascent() + paint.descent()) / 2
        canvas.drawText(value, x, baseline, paint)
    }
}

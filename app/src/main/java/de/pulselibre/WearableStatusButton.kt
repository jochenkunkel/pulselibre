package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Combined band status, band battery and settings button of the header (#649), drawn after the approved designs
 * `design/assets/wearable-settings-battery-{worn,not-worn,offline,unknown}.svg` (viewBox 186 × 48). Example numbers of
 * the SVGs are not used: the percentage comes from stored device data or reads "–".
 * State is never shown by colour alone: every state has its own glyph (check, "!", crossed band, "?").
 */
internal class WearableStatusButton(context: Context) : View(context) {
    var state: WearState = WearState.UNKNOWN
        private set
    var batteryPercent: Int? = null
        private set
    /** Narrow screens drop the band outline so the title keeps its room; status, battery and gear stay. */
    var compact: Boolean = false
        set(value) { field = value; requestLayout(); invalidate() }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /** Sized in design units: the canvas is scaled to the view height (48 units). */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 15f }
    private val rect = RectF()
    private val path = Path()

    init {
        isClickable = true
        isFocusable = true
        tag = TAG
        minimumHeight = (48 * density).toInt()
    }

    fun show(state: WearState, batteryPercent: Int?, description: String) {
        val changed = state != this.state || batteryPercent != this.batteryPercent
        this.state = state
        this.batteryPercent = batteryPercent
        if (contentDescription?.toString() != description) contentDescription = description
        if (changed) invalidate() // polled every second: draw only on a real change
    }

    /** Drawing origin of the design (SVG x units) after the optional band outline. */
    private val shift get() = if (compact) 26f else 0f
    private val designWidth get() = 186f - shift

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((designWidth * density).toInt(), (48 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val s = height / 48f
        canvas.save()
        canvas.scale(s, s)
        val colors = COLORS.getValue(state)
        paint.style = Paint.Style.FILL
        paint.color = colors.background
        rect.set(0f, 0f, designWidth, 48f); canvas.drawRoundRect(rect, 24f, 24f, paint)
        canvas.translate(-shift, 0f)
        stroke(INK, 2.6f)
        if (!compact) {
            rect.set(13f, 9f, 30f, 39f); canvas.drawRoundRect(rect, 7f, 7f, paint)
            stroke(INK, 1.5f); canvas.drawLine(18f, 13f, 25f, 13f, paint); canvas.drawLine(18f, 35f, 25f, 35f, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = colors.status
        canvas.drawCircle(40f, 24f, 11f, paint)
        drawGlyph(canvas)
        // Battery outline, fill proportional to the real percentage, or a dash without a value.
        stroke(INK, 2f)
        rect.set(61f, 17f, 82f, 31f); canvas.drawRoundRect(rect, 2f, 2f, paint)
        stroke(INK, 3f); canvas.drawLine(83f, 21f, 83f, 27f, paint)
        val percent = batteryPercent
        if (percent != null) {
            paint.style = Paint.Style.FILL
            paint.color = if (WearableStatus.isLow(percent)) LOW else BATTERY
            rect.set(64f, 20f, 64f + 15f * percent / 100f, 28f); canvas.drawRoundRect(rect, 1f, 1f, paint)
        } else {
            stroke(MUTED, 2f); canvas.drawLine(66f, 24f, 78f, 24f, paint)
        }
        textPaint.color = INK
        canvas.drawText(percent?.let { "$it%" } ?: "–", 90f, 30f, textPaint)
        stroke(DIVIDER, 1.5f); canvas.drawLine(137f, 12f, 137f, 36f, paint)
        drawGear(canvas, 160f, 24f)
        canvas.restore()
    }

    private fun drawGlyph(canvas: Canvas) {
        stroke(android.graphics.Color.WHITE, 2.8f)
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        path.reset()
        when (state) {
            WearState.WORN -> { path.moveTo(35f, 22f); path.rLineTo(4f, 4f); path.rLineTo(7f, -8f); canvas.drawPath(path, paint) }
            WearState.NOT_WORN -> {
                canvas.drawLine(40f, 18f, 40f, 26f, paint)
                paint.style = Paint.Style.FILL; canvas.drawCircle(40f, 30f, 1.5f, paint)
            }
            WearState.UNKNOWN -> {
                path.moveTo(37f, 21f); path.arcTo(RectF(36.6f, 16.4f, 43.8f, 23.6f), 180f, 260f); path.lineTo(40f, 27f)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL; canvas.drawCircle(40f, 30.5f, 1.5f, paint)
            }
            WearState.OFFLINE -> {
                stroke(LOW, 2.6f); canvas.drawLine(28f, 35f, 49f, 14f, paint) // crosses band and status: "no link"
            }
        }
        paint.strokeCap = Paint.Cap.BUTT; paint.strokeJoin = Paint.Join.MITER
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float) {
        stroke(INK, 2.2f)
        canvas.drawCircle(cx, cy, 6.5f, paint)
        canvas.drawCircle(cx, cy, 3f, paint)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = Math.cos(a).toFloat(); val sn = Math.sin(a).toFloat()
            canvas.drawLine(cx + 6.5f * c, cy + 6.5f * sn, cx + 9.5f * c, cy + 9.5f * sn, paint)
        }
    }

    private fun stroke(color: Int, width: Float) {
        paint.style = Paint.Style.STROKE; paint.color = color; paint.strokeWidth = width
    }

    private data class Colors(val background: Int, val status: Int)

    companion object {
        const val TAG = "wearable_status_button"
        private val INK = 0xFF31165B.toInt()
        private val BATTERY = 0xFF58309A.toInt()
        private val LOW = 0xFFD33D46.toInt()
        private val MUTED = 0xFF8990A0.toInt()
        private val DIVIDER = 0xFFA899C1.toInt()
        private val COLORS = mapOf(
            WearState.WORN to Colors(0xFFE3F7E8.toInt(), 0xFF259A37.toInt()),
            WearState.NOT_WORN to Colors(0xFFFFF2D4.toInt(), 0xFFE69B00.toInt()),
            WearState.OFFLINE to Colors(0xFFECEBF2.toInt(), 0xFF8990A0.toInt()),
            WearState.UNKNOWN to Colors(0xFFEEE3FF.toInt(), 0xFF7450A8.toInt())
        )
    }
}

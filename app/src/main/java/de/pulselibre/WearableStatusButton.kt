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
    /** The shown value is only the last known one (offline or older than 12 h): drawn grey. */
    var batteryStale: Boolean = false
        private set
    /**
     * Phones narrower than 412 dp (e.g. S10, 360 dp): no band outline and tighter spacing (147 instead of 186 units),
     * otherwise the header title was cut ("PulsLib"). Status, battery, value and gear stay.
     */
    var compact: Boolean = false
        set(value) { field = value; requestLayout(); invalidate() }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /** Sized in design units: the canvas is scaled to the view height (48 units). */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 15f }
    private val rect = RectF()
    private val path = Path()

    init {
        // Drawing only: the three touch areas of [WearableStatusControl] carry clicks and TalkBack labels.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        minimumHeight = (48 * density).toInt()
    }

    fun show(state: WearState, battery: WearableStatus.BatteryShown?) {
        val percent = battery?.percent
        val stale = battery != null && !battery.current
        val changed = state != this.state || percent != batteryPercent || stale != batteryStale
        this.state = state
        batteryPercent = percent
        batteryStale = stale
        if (changed) invalidate() // polled every second: draw only on a real change
    }

    /** Touch-area widths in design units (status | battery | settings) for the current variant. */
    internal fun zoneUnits(): Triple<Float, Float, Float> =
        if (compact) Triple(48f, 51f, 48f) else Triple(56f, 81f, 49f)

    // Horizontal layout in design units; the full variant follows the SVG exactly.
    private val statusDx get() = if (compact) -20f else 0f
    private val batteryDx get() = if (compact) -23f else 0f
    private val dividerX get() = if (compact) 108f else 137f
    private val gearX get() = if (compact) 127f else 160f
    private val designWidth get() = if (compact) 147f else 186f

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
        stroke(INK, 2.6f)
        if (!compact) {
            rect.set(13f, 9f, 30f, 39f); canvas.drawRoundRect(rect, 7f, 7f, paint)
            stroke(INK, 1.5f); canvas.drawLine(18f, 13f, 25f, 13f, paint); canvas.drawLine(18f, 35f, 25f, 35f, paint)
        }
        canvas.save(); canvas.translate(statusDx, 0f)
        paint.style = Paint.Style.FILL; paint.color = colors.status
        canvas.drawCircle(40f, 24f, 11f, paint)
        drawGlyph(canvas)
        canvas.restore()
        canvas.save(); canvas.translate(batteryDx, 0f)
        // Battery outline, fill proportional to the real percentage, or a dash without a value.
        stroke(INK, 2f)
        rect.set(61f, 17f, 82f, 31f); canvas.drawRoundRect(rect, 2f, 2f, paint)
        stroke(INK, 3f); canvas.drawLine(83f, 21f, 83f, 27f, paint)
        val percent = batteryPercent
        if (percent != null) {
            paint.style = Paint.Style.FILL
            paint.color = if (batteryStale) MUTED else if (WearableStatus.isLow(percent)) LOW else BATTERY
            rect.set(64f, 20f, 64f + 15f * percent / 100f, 28f); canvas.drawRoundRect(rect, 1f, 1f, paint)
        } else {
            stroke(MUTED, 2f); canvas.drawLine(66f, 24f, 78f, 24f, paint)
        }
        textPaint.color = if (batteryStale) MUTED else INK
        canvas.drawText(percent?.let { "$it%" } ?: "–", 90f, 30f, textPaint)
        canvas.restore()
        stroke(DIVIDER, 1.5f); canvas.drawLine(dividerX, 12f, dividerX, 36f, paint)
        drawGear(canvas, gearX, 24f)
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

/**
 * The header control (#649): the drawn pill plus three invisible touch areas on top of it – band/connection, band
 * battery, settings – each at least 48 dp and with its own TalkBack label.
 */
internal class WearableStatusControl(context: Context) : android.widget.FrameLayout(context) {
    val drawing = WearableStatusButton(context)
    val statusZone = zone(TAG_STATUS)
    val batteryZone = zone(TAG_BATTERY)
    val settingsZone = zone(TAG_SETTINGS)
    private val zones = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
    private val density = resources.displayMetrics.density

    var compact: Boolean
        get() = drawing.compact
        set(value) { drawing.compact = value; layoutZones() }

    init {
        tag = WearableStatusButton.TAG
        addView(drawing, LayoutParams(-2, -1))
        listOf(statusZone, batteryZone, settingsZone).forEach { zones.addView(it) }
        addView(zones, LayoutParams(-1, -1))
        layoutZones()
    }

    private fun zone(name: String) = View(context).apply {
        tag = name
        isClickable = true
        isFocusable = true
        minimumHeight = (48 * resources.displayMetrics.density).toInt()
        background = android.util.TypedValue().let { tv ->
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
            androidx.core.content.ContextCompat.getDrawable(context, tv.resourceId)
        }
    }

    private fun layoutZones() {
        val (a, b, c) = drawing.zoneUnits()
        listOf(statusZone to a, batteryZone to b, settingsZone to c).forEach { (v, units) ->
            v.layoutParams = android.widget.LinearLayout.LayoutParams((units * density).toInt(), -1)
        }
    }

    fun show(state: WearState, battery: WearableStatus.BatteryShown?, statusDescription: String, batteryDescription: String,
             settingsDescription: String) {
        drawing.show(state, battery)
        if (statusZone.contentDescription?.toString() != statusDescription) statusZone.contentDescription = statusDescription
        if (batteryZone.contentDescription?.toString() != batteryDescription) batteryZone.contentDescription = batteryDescription
        if (settingsZone.contentDescription?.toString() != settingsDescription) settingsZone.contentDescription = settingsDescription
    }

    companion object {
        const val TAG_STATUS = "wearable_zone_status"
        const val TAG_BATTERY = "wearable_zone_battery"
        const val TAG_SETTINGS = "wearable_zone_settings"
    }
}

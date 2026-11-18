package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Small app mark: violet rounded square with a white pulse line (decorative). */
internal class PulseLogoView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DesignTokens.BRAND_PURPLE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()
    private val path = Path()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, w * 0.26f, w * 0.26f, fill)
        line.strokeWidth = w * 0.075f
        path.reset()
        path.moveTo(w * 0.16f, h * 0.54f); path.lineTo(w * 0.34f, h * 0.54f); path.lineTo(w * 0.42f, h * 0.30f)
        path.lineTo(w * 0.54f, h * 0.76f); path.lineTo(w * 0.62f, h * 0.54f); path.lineTo(w * 0.84f, h * 0.54f)
        canvas.drawPath(path, line)
    }
}

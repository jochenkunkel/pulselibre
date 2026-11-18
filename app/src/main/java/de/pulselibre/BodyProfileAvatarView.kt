package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * Round design illustration for the body profile card (#124): a friendly, neutral figure in a violet hoodie
 * in front of a mountain sunset, drawn locally as vector paths (no image, no network, no photo).
 * It is an illustration, not a statement about the appearance or sex of the user.
 */
internal class BodyProfileAvatarView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFFD9C6FF.toInt() }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = DesignTokens.MINT
    }
    private val oval = RectF()
    private val figure = Path()   // reused scratch path, sized in 100x100 space

    init {
        contentDescription = "Illustration einer Person im violetten Kapuzenpullover"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val ringW = size * 0.03f
        val cx = width / 2f; val cy = height / 2f
        val r = size / 2f - ringW
        canvas.save()
        clip.reset(); clip.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(clip)
        canvas.translate(cx - r, cy - r)
        val s = 2f * r / 100f
        canvas.scale(s, s)
        // sunset sky
        paint.shader = LinearGradient(0f, 0f, 0f, 70f, intArrayOf(0xFF3A1B66.toInt(), 0xFFB45A9A.toInt(), 0xFFF3A58A.toInt()),
            floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        paint.shader = null
        // sun
        paint.color = 0xFFFFD2A1.toInt(); canvas.drawCircle(70f, 52f, 9f, paint)
        // mountains
        paint.color = 0xFF6A3B9B.toInt()
        figure.reset(); figure.moveTo(0f, 100f); figure.lineTo(0f, 62f); figure.lineTo(20f, 50f); figure.lineTo(38f, 63f)
        figure.lineTo(58f, 46f); figure.lineTo(80f, 62f); figure.lineTo(100f, 52f); figure.lineTo(100f, 100f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFF4A2A78.toInt()
        figure.reset(); figure.moveTo(0f, 100f); figure.lineTo(0f, 76f); figure.lineTo(26f, 64f); figure.lineTo(50f, 78f)
        figure.lineTo(76f, 66f); figure.lineTo(100f, 76f); figure.lineTo(100f, 100f); figure.close()
        canvas.drawPath(figure, paint)
        // hoodie shoulders
        paint.shader = LinearGradient(0f, 66f, 0f, 100f, 0xFF7B4FD8.toInt(), 0xFF4A2A9E.toInt(), Shader.TileMode.CLAMP)
        figure.reset(); figure.moveTo(14f, 100f); figure.quadTo(16f, 72f, 40f, 68f); figure.lineTo(60f, 68f)
        figure.quadTo(84f, 72f, 86f, 100f); figure.close()
        canvas.drawPath(figure, paint)
        paint.shader = null
        // hood rim and neck
        paint.color = 0xFF5A33B8.toInt(); oval.set(32f, 58f, 68f, 78f); canvas.drawOval(oval, paint)
        paint.shader = LinearGradient(0f, 34f, 0f, 70f, 0xFFF4C9A5.toInt(), 0xFFD79A78.toInt(), Shader.TileMode.CLAMP)
        oval.set(36f, 30f, 64f, 68f); canvas.drawOval(oval, paint)   // head
        paint.shader = null
        // hair cap (neutral short style)
        paint.color = 0xFF3B2A4E.toInt()
        figure.reset(); figure.moveTo(35f, 46f); figure.quadTo(33f, 26f, 50f, 25f); figure.quadTo(67f, 26f, 65f, 46f)
        figure.quadTo(58f, 36f, 50f, 36f); figure.quadTo(42f, 36f, 35f, 46f); figure.close()
        canvas.drawPath(figure, paint)
        // friendly face: eyes and a soft smile
        paint.color = 0xFF3A2A3A.toInt()
        canvas.drawCircle(44.5f, 49f, 1.5f, paint); canvas.drawCircle(55.5f, 49f, 1.5f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f; paint.strokeCap = Paint.Cap.ROUND
        figure.reset(); figure.moveTo(45f, 57f); figure.quadTo(50f, 61f, 55f, 57f)
        canvas.drawPath(figure, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
        // light ring and a short mint accent arc bottom right
        ring.strokeWidth = ringW
        canvas.drawCircle(cx, cy, r, ring)
        accent.strokeWidth = ringW * 1.3f
        oval.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, 20f, 48f, false, accent)
    }
}

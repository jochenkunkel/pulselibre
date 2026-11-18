package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Compact, code-drawn profile portrait; independent of emoji fonts and user-entered sex. */
internal class BodyProfileAvatarView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Constant geometry built once instead of allocating paths on every draw.
    private val shouldersPath = Path().apply {
            moveTo(22f, 105f); quadTo(24f, 78f, 48f, 76f)
            lineTo(64f, 76f); quadTo(89f, 79f, 91f, 105f); close()
    }
    private val hairPath = Path().apply {
            moveTo(33f, 43f); quadTo(28f, 18f, 54f, 14f)
            quadTo(79f, 13f, 80f, 40f); lineTo(75f, 32f)
            quadTo(62f, 25f, 49f, 31f); quadTo(42f, 34f, 36f, 42f); close()
    }
    private val beardPath = Path().apply {
            moveTo(35f, 51f); quadTo(39f, 60f, 43f, 57f)
            quadTo(55f, 63f, 68f, 56f); quadTo(74f, 60f, 78f, 50f)
            lineTo(75f, 68f); quadTo(68f, 82f, 56f, 83f)
            quadTo(43f, 81f, 37f, 68f); close()
    }

    init {
        contentDescription = "Illustration eines bärtigen Mannes"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width / 112f, height / 112f)
        if (scale <= 0f) return
        canvas.save()
        canvas.translate((width - 112f * scale) / 2f, (height - 112f * scale) / 2f)
        canvas.scale(scale, scale)

        // Soft, unobtrusive portrait backdrop.
        paint.color = 0xFFE9E5F4.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(56f, 54f, 49f, paint)

        // Neck and shoulders.
        paint.color = 0xFFE9B992.toInt()
        canvas.drawRoundRect(47f, 68f, 65f, 88f, 7f, 7f, paint)
        paint.color = 0xFF66528D.toInt()
        canvas.drawPath(shouldersPath, paint)

        // Head, ears, hair and beard: a clear, friendly masculine portrait.
        paint.color = 0xFFE9B992.toInt()
        canvas.drawOval(34f, 20f, 78f, 76f, paint)
        canvas.drawOval(29f, 43f, 38f, 57f, paint)
        canvas.drawOval(74f, 43f, 83f, 57f, paint)

        paint.color = 0xFF594033.toInt()
        canvas.drawPath(hairPath, paint)
        canvas.drawPath(beardPath, paint)

        // Eyes and nose remain visible above the beard.
        paint.color = 0xFF382B27.toInt()
        canvas.drawCircle(46f, 47f, 1.8f, paint)
        canvas.drawCircle(66f, 47f, 1.8f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.7f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(56f, 48f, 54f, 55f, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF8B513F.toInt()
        canvas.drawOval(48f, 56f, 64f, 64f, paint)

        canvas.restore()
    }
}

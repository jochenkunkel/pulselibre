package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * Round design illustration for the body profile card (#124): a friendly, neutral figure with a baseball cap in a violet hoodie
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
        contentDescription = "Illustration einer Person mit Basecap und violettem Hoodie"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private var cache: android.graphics.Bitmap? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cache?.recycle(); cache = null
    }

    /** The illustration is static: render it once per size and just blit it afterwards (no gradient/path work per frame). */
    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        val bmp = cache ?: android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888).also {
            drawIllustration(Canvas(it)); cache = it
        }
        canvas.drawBitmap(bmp, 0f, 0f, null)
    }

    private fun drawIllustration(canvas: Canvas) {
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
        // --- background: dusk sky, glow, two mountain layers ---
        paint.shader = LinearGradient(0f, 0f, 0f, 80f, intArrayOf(0xFF2A1458.toInt(), 0xFF7A3F9E.toInt(), 0xFFE58FA0.toInt(), 0xFFFFC9A0.toInt()),
            floatArrayOf(0f, 0.45f, 0.8f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        paint.shader = RadialGradient(74f, 56f, 30f, intArrayOf(0xAAFFE1B5.toInt(), 0x00FFE1B5), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(74f, 56f, 30f, paint)
        paint.shader = null
        paint.color = 0xFFFFE2B8.toInt(); canvas.drawCircle(76f, 57f, 6f, paint)           // sun
        paint.color = 0xFF7440A8.toInt()
        figure.reset(); figure.moveTo(0f, 100f); figure.lineTo(0f, 60f); figure.lineTo(18f, 48f); figure.lineTo(34f, 60f)
        figure.lineTo(52f, 44f); figure.lineTo(70f, 62f); figure.lineTo(86f, 52f); figure.lineTo(100f, 60f); figure.lineTo(100f, 100f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFF4B2A82.toInt()
        figure.reset(); figure.moveTo(0f, 100f); figure.lineTo(0f, 74f); figure.lineTo(24f, 64f); figure.lineTo(48f, 76f)
        figure.lineTo(74f, 66f); figure.lineTo(100f, 76f); figure.lineTo(100f, 100f); figure.close()
        canvas.drawPath(figure, paint)

        // --- person: broad shoulders in a violet hoodie, hood behind the neck ---
        paint.shader = LinearGradient(0f, 60f, 0f, 100f, 0xFF6C3FD0.toInt(), 0xFF3C2290.toInt(), Shader.TileMode.CLAMP)
        figure.reset(); figure.moveTo(50f, 62f); figure.cubicTo(30f, 62f, 20f, 70f, 14f, 100f); figure.lineTo(86f, 100f)
        figure.cubicTo(80f, 70f, 70f, 62f, 50f, 62f); figure.close()
        canvas.drawPath(figure, paint)
        paint.shader = null
        paint.color = 0xFF54309F.toInt()                                                   // hood (back rim)
        figure.reset(); figure.moveTo(34f, 66f); figure.cubicTo(36f, 56f, 64f, 56f, 66f, 66f); figure.cubicTo(60f, 72f, 40f, 72f, 34f, 66f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFFD9A07F.toInt()                                                   // neck
        figure.reset(); figure.moveTo(43f, 56f); figure.lineTo(57f, 56f); figure.lineTo(59f, 70f); figure.quadTo(50f, 77f, 41f, 70f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFF7448DA.toInt()                                                   // hoodie front opening
        figure.reset(); figure.moveTo(40f, 68f); figure.quadTo(50f, 80f, 60f, 68f); figure.quadTo(50f, 74f, 40f, 68f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFFE9DFFF.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.9f; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(46f, 74f, 45f, 84f, paint); canvas.drawLine(54f, 74f, 55f, 84f, paint)   // drawstrings
        paint.style = Paint.Style.FILL

        // --- head ---
        paint.shader = LinearGradient(0f, 26f, 0f, 62f, 0xFFF8D2B0.toInt(), 0xFFE2A98A.toInt(), Shader.TileMode.CLAMP)
        figure.reset(); figure.moveTo(50f, 26f)
        figure.cubicTo(62f, 26f, 66f, 36f, 65f, 46f)
        figure.cubicTo(64f, 56f, 58f, 62f, 50f, 63f)
        figure.cubicTo(42f, 62f, 36f, 56f, 35f, 46f)
        figure.cubicTo(34f, 36f, 38f, 26f, 50f, 26f); figure.close()
        canvas.drawPath(figure, paint)
        paint.shader = null
        paint.color = 0xFFE6B08F.toInt()                                                   // ears
        oval.set(32.5f, 43f, 37.5f, 53f); canvas.drawOval(oval, paint); oval.set(62.5f, 43f, 67.5f, 53f); canvas.drawOval(oval, paint)
        // short side hair showing under the cap
        paint.color = 0xFF2E2147.toInt()
        oval.set(33.2f, 38f, 37.6f, 47f); canvas.drawOval(oval, paint); oval.set(62.4f, 38f, 66.8f, 47f); canvas.drawOval(oval, paint)
        // baseball cap: domed crown, front seam, button, forward-pointing brim with a soft shadow on the forehead
        paint.shader = LinearGradient(0f, 18f, 0f, 37f, 0xFF5A2F8C.toInt(), 0xFF3B1E5C.toInt(), Shader.TileMode.CLAMP)
        figure.reset(); figure.moveTo(32.5f, 36.5f); figure.cubicTo(31f, 22f, 40f, 17.5f, 50f, 17.5f)
        figure.cubicTo(60f, 17.5f, 69f, 22f, 67.5f, 36.5f); figure.quadTo(50f, 33.5f, 32.5f, 36.5f); figure.close()
        canvas.drawPath(figure, paint)
        paint.shader = null
        paint.color = 0x33FFFFFF                                                            // crown highlight
        figure.reset(); figure.moveTo(38f, 27f); figure.quadTo(43f, 20.5f, 49f, 20f); figure.quadTo(43f, 23f, 40.5f, 29f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFFB79BEA.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.8f; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(50f, 18.5f, 50f, 34.2f, paint)                                      // front seam
        figure.reset(); figure.moveTo(41f, 21.5f); figure.quadTo(39f, 29f, 37f, 35f); canvas.drawPath(figure, paint)   // side panel seams
        figure.reset(); figure.moveTo(59f, 21.5f); figure.quadTo(61f, 29f, 63f, 35f); canvas.drawPath(figure, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFFB79BEA.toInt(); canvas.drawCircle(50f, 18.2f, 1.5f, paint)       // button
        paint.color = 0x40000000                                                            // shadow under the brim
        oval.set(36f, 38.5f, 64f, 44.5f); canvas.drawOval(oval, paint)
        paint.color = 0xFF2F1750.toInt()                                                    // brim
        figure.reset(); figure.moveTo(30.5f, 36f); figure.quadTo(50f, 32.5f, 69.5f, 36f); figure.quadTo(74f, 38.8f, 63.5f, 41.2f)
        figure.quadTo(50f, 44f, 36.5f, 41.2f); figure.quadTo(26f, 38.8f, 30.5f, 36f); figure.close()
        canvas.drawPath(figure, paint)
        paint.color = 0xFF6B3FB0.toInt()                                                    // brim top edge light
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.7f
        figure.reset(); figure.moveTo(32f, 36.2f); figure.quadTo(50f, 33.2f, 68f, 36.2f); canvas.drawPath(figure, paint)
        paint.style = Paint.Style.FILL
        // face: brows, eyes with highlight, nose, cheeks, smile
        paint.color = 0xFF3B2A4E.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f
        canvas.drawLine(41.5f, 44.2f, 47f, 43.6f, paint); canvas.drawLine(53f, 43.6f, 58.5f, 44.2f, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF2B1F36.toInt(); oval.set(42.3f, 45.3f, 46.7f, 50f); canvas.drawOval(oval, paint); oval.set(53.3f, 45.3f, 57.7f, 50f); canvas.drawOval(oval, paint)
        paint.color = 0xFFFFFFFF.toInt(); canvas.drawCircle(45.4f, 46.5f, 0.8f, paint); canvas.drawCircle(56.4f, 46.5f, 0.8f, paint)
        paint.color = 0x55D9806E; canvas.drawCircle(42f, 54f, 3f, paint); canvas.drawCircle(58f, 54f, 3f, paint)   // cheeks
        paint.color = 0xFFC98B6F.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.strokeCap = Paint.Cap.ROUND
        figure.reset(); figure.moveTo(50f, 48f); figure.quadTo(48.5f, 53f, 50.5f, 54f); canvas.drawPath(figure, paint)   // nose
        paint.color = 0xFFA85A55.toInt(); paint.strokeWidth = 1.5f
        figure.reset(); figure.moveTo(44.5f, 57f); figure.quadTo(50f, 61.5f, 55.5f, 57f); canvas.drawPath(figure, paint)   // smile
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

package de.pulselibre

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Header background: dark violet gradient, a small warm sunrise and three layered mountain ridges at the bottom.
 * Pure vector drawing, built once per size (no bitmaps, no animation, no blur).
 */
internal class HeaderBackdropDrawable(private val dark: Boolean) : Drawable() {
    private val sky = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sun = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ridge = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paths = Array(3) { Path() }
    private var sunX = 0f; private var sunY = 0f; private var sunR = 0f

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        sky.shader = LinearGradient(0f, 0f, w * 0.6f, h, DesignTokens.headerColors(dark), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        // Sun sits low on the right, partly behind the far ridge.
        sunR = h * 0.16f; sunX = w * 0.74f; sunY = h * 0.70f
        sun.shader = RadialGradient(sunX, sunY, sunR * 2.4f,
            intArrayOf(0xFFFFC796.toInt(), 0x66FF9F8A, 0x00FF9F8A), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        // Three ridges, far to near; x positions are fractions of the width, y of the height.
        ridge(paths[0], w, h, 0.62f, floatArrayOf(0f, .16f, .30f, .46f, .62f, .78f, .90f, 1f), floatArrayOf(.78f, .60f, .72f, .56f, .70f, .52f, .68f, .62f))
        ridge(paths[1], w, h, 0.82f, floatArrayOf(0f, .12f, .28f, .44f, .58f, .74f, .88f, 1f), floatArrayOf(.88f, .74f, .84f, .72f, .86f, .70f, .84f, .78f))
        ridge(paths[2], w, h, 1f, floatArrayOf(0f, .20f, .38f, .54f, .72f, .90f, 1f), floatArrayOf(.98f, .90f, .96f, .88f, .97f, .91f, .95f))
    }

    private fun ridge(path: Path, w: Float, h: Float, @Suppress("UNUSED_PARAMETER") depth: Float, xs: FloatArray, ys: FloatArray) {
        path.reset()
        path.moveTo(0f, h)
        for (i in xs.indices) path.lineTo(xs[i] * w, ys[i] * h)
        path.lineTo(w, h)
        path.close()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawRect(b, sky)
        canvas.drawCircle(sunX, sunY, sunR * 2.4f, sun)
        val colors = if (dark) intArrayOf(0xFF4A2872.toInt(), 0xFF3A1F5E.toInt(), 0xFF2A1647.toInt())
            else intArrayOf(0xFF6E49D6.toInt(), 0xFF5A38BC.toInt(), 0xFF45289A.toInt())
        for (i in 0..2) {
            ridge.color = colors[i]
            canvas.drawPath(paths[i], ridge)
        }
    }

    override fun setAlpha(alpha: Int) { sky.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { sky.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

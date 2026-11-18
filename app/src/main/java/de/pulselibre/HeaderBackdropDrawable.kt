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
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Header background: dark violet gradient, a small warm sunrise and the layered mountain range
 * Eiger - Moench - Jungfrau (own stylised drawing, see [HeaderMountainData]).
 *
 * The range is scaled uniformly (never distorted) and centred so that the three massifs stay recognisable on narrow
 * and wide screens; the edges are roughened deterministically for a finer, more natural outline. Pure vector drawing,
 * built once per size (no bitmaps, no animation, no blur, no network). Decorative only.
 */
internal class HeaderBackdropDrawable(private val dark: Boolean) : Drawable() {
    private val sky = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sun = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val mainPath = Path()
    private val midPath = Path()
    private val nearPath = Path()
    private val ribPath = Path()
    private val shadePath = Path()
    private val snowPath = Path()
    private var sunX = 0f; private var sunY = 0f; private var sunR = 0f
    private var mainShader: Shader? = null

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        sky.shader = LinearGradient(0f, 0f, w * 0.6f, h, DesignTokens.headerColors(dark), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)

        // Uniform scale: the range fills about 60 % of the header height, but never wider than the screen allows;
        // the trio (Eiger..Jungfrau plus margin) is centred horizontally.
        val trioLeft = HeaderMountainData.EIGER_X - 160f
        val trioRight = HeaderMountainData.JUNGFRAU_X + 190f
        val scale = min(h * 0.60f / (HeaderMountainData.HEIGHT - HeaderMountainData.TOP_Y), w / (trioRight - trioLeft))
        val tx = w / 2f - (trioLeft + trioRight) / 2f * scale
        fun mx(x: Float) = x * scale + tx
        fun my(y: Float) = h - (HeaderMountainData.HEIGHT - y) * scale

        buildOutline(mainPath, HeaderMountainData.main, ::mx, ::my, w, h, seed = 7, roughness = 5.5f * scale)
        buildOutline(midPath, HeaderMountainData.mid, ::mx, ::my, w, h, seed = 19, roughness = 3.5f * scale)
        buildOutline(nearPath, HeaderMountainData.near, ::mx, ::my, w, h, seed = 31, roughness = 2.5f * scale)
        ribPath.reset(); shadePath.reset(); snowPath.reset()
        HeaderMountainData.ribs.forEachIndexed { i, r -> polyline(ribPath, r, ::mx, ::my, seed = 100 + i, roughness = 1.6f * scale) }
        HeaderMountainData.shades.forEachIndexed { i, r -> polyline(shadePath, r, ::mx, ::my, seed = 200 + i, roughness = 1.2f * scale) }
        // Small snow caps just under each summit (very soft lavender, never white).
        listOf(HeaderMountainData.EIGER_X to 140f, HeaderMountainData.MOENCH_X to 111f, HeaderMountainData.JUNGFRAU_X to 104f).forEach { (px, py) ->
            val cx = mx(px); val cy = my(py + 4f)
            snowPath.moveTo(cx, cy)
            snowPath.lineTo(cx - 26f * scale, cy + 30f * scale)
            snowPath.lineTo(cx - 8f * scale, cy + 24f * scale)
            snowPath.lineTo(cx + 4f * scale, cy + 36f * scale)
            snowPath.lineTo(cx + 12f * scale, cy + 22f * scale)
            snowPath.lineTo(cx + 28f * scale, cy + 32f * scale)
            snowPath.close()
        }
        val peakY = my(HeaderMountainData.TOP_Y)
        mainShader = LinearGradient(0f, peakY, 0f, h,
            if (dark) intArrayOf(0xFF6A428F.toInt(), 0xFF4A2872.toInt(), 0xFF2A1647.toInt())
            else intArrayOf(0xFF8A62E0.toInt(), 0xFF6A45CC.toInt(), 0xFF45289A.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)

        // Sun sits low behind the Jungfrau side, as in the evening light of the reference mood.
        sunR = h * 0.16f; sunX = mx(930f); sunY = my(150f)
        sun.shader = RadialGradient(sunX, sunY, sunR * 2.4f,
            intArrayOf(0xFFFFC796.toInt(), 0x66FF9F8A, 0x00FF9F8A), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        line.strokeWidth = (1.1f * scale).coerceAtLeast(0.8f)
    }

    /** Deterministic pseudo-noise in -1..1, so the outline is identical on every draw and every device. */
    private fun noise(seed: Int, i: Int): Float {
        val v = sin(seed * 12.9898 + i * 78.233) * 43758.5453
        return ((v - floor(v)) * 2.0 - 1.0).toFloat()
    }

    private fun roughen(src: List<Pair<Float, Float>>, seed: Int, roughness: Float): List<Pair<Float, Float>> {
        val out = ArrayList<Pair<Float, Float>>(src.size * 3)
        for (i in 0 until src.size - 1) {
            val (x0, y0) = src[i]; val (x1, y1) = src[i + 1]
            out += x0 to y0
            val len = hypot(x1 - x0, y1 - y0)
            if (len < roughness * 3f) continue
            // three extra vertices per longer segment, displaced perpendicular to the segment
            val nx = -(y1 - y0) / len; val ny = (x1 - x0) / len
            for (k in 1..3) {
                val t = k / 4f
                val d = noise(seed, i * 3 + k) * roughness
                out += (x0 + (x1 - x0) * t + nx * d) to (y0 + (y1 - y0) * t + ny * d)
            }
        }
        out += src.last()
        return out
    }

    private fun toPoints(a: FloatArray, mx: (Float) -> Float, my: (Float) -> Float): List<Pair<Float, Float>> =
        (0 until a.size / 2).map { mx(a[it * 2]) to my(a[it * 2 + 1]) }

    private fun buildOutline(path: Path, data: FloatArray, mx: (Float) -> Float, my: (Float) -> Float, w: Float, h: Float, seed: Int, roughness: Float) {
        path.reset()
        val pts = roughen(toPoints(data, mx, my), seed, roughness)
        // extend to both edges so the range always reaches the sides, then close along the bottom
        path.moveTo(-w, h)
        path.lineTo(-w, pts.first().second)
        path.lineTo(pts.first().first, pts.first().second)
        pts.drop(1).forEach { path.lineTo(it.first, it.second) }
        path.lineTo(2f * w, pts.last().second)
        path.lineTo(2f * w, h)
        path.close()
    }

    private fun polyline(path: Path, data: FloatArray, mx: (Float) -> Float, my: (Float) -> Float, seed: Int, roughness: Float) {
        val pts = roughen(toPoints(data, mx, my), seed, roughness)
        path.moveTo(pts[0].first, pts[0].second)
        pts.drop(1).forEach { path.lineTo(it.first, it.second) }
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawRect(b, sky)
        canvas.drawCircle(sunX, sunY, sunR * 2.4f, sun)
        // far range: Eiger - Moench - Jungfrau
        fill.shader = mainShader
        canvas.drawPath(mainPath, fill)
        fill.shader = null
        // soft lavender snow caps and rock detail inside the range
        fill.color = if (dark) 0x52E3C6F5 else 0x59FFF0FF
        canvas.drawPath(snowPath, fill)
        line.color = if (dark) 0x66D9A8C5 else 0x66F2D8F0
        canvas.drawPath(ribPath, line)
        line.color = if (dark) 0x66160E28 else 0x4D2A1060
        canvas.drawPath(shadePath, line)
        // foothills in front
        fill.color = if (dark) 0xB8321C52.toInt() else 0xB84A2CA6.toInt()
        canvas.drawPath(midPath, fill)
        fill.color = if (dark) 0xFF1E1233.toInt() else 0xFF371F82.toInt()
        canvas.drawPath(nearPath, fill)
    }

    override fun setAlpha(alpha: Int) { sky.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { sky.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

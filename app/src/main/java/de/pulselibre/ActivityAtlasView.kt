package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.text.NumberFormat
import java.util.Locale

/**
 * Canvas view of the activity atlas (#41). Calendar-aligned (Monday first), one cell per day; missing days are gray,
 * the number inside a cell (rounded thousands) keeps levels distinguishable without colour. Tap reports the cell.
 */
internal class ActivityAtlasView(context: Context, private val onCell: (ActivityAtlas.Cell) -> Unit) : View(context) {
    private val palette = AppPalette.of(context)
    private val d = resources.displayMetrics.density
    private var model: ActivityAtlas.Model? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val rect = RectF()
    private val headerHeight = 18 * d
    private val gap = 3 * d
    private val weekdays = listOf(R.string.an_wd_mo, R.string.an_wd_di, R.string.an_wd_mi, R.string.an_wd_do, R.string.an_wd_fr, R.string.an_wd_sa, R.string.an_wd_so).map { context.getString(it) }
    private val oneDecimal = NumberFormat.getNumberInstance(context.displayLocale()).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }

    init { tag = "activity_atlas" }

    fun show(value: ActivityAtlas.Model) {
        model = value
        contentDescription = context.getString(R.string.md_atlas_cd, DomainText.tr(context, value.summary()), value.windowDays.toString())
        requestLayout(); invalidate()
    }

    private fun cellSize(width: Int): Float = (width - paddingLeft - paddingRight - gap * 6) / 7f
    private fun rowHeight(width: Int, weeks: Int): Float {
        val w = cellSize(width)
        return if (weeks > 8) minOf(w, 26 * d) else minOf(w, 44 * d)
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val w = MeasureSpec.getSize(widthSpec)
        val m = model
        val h = if (m == null) 0f else headerHeight + m.weeks * (rowHeight(w, m.weeks) + gap)
        setMeasuredDimension(w, h.toInt() + 1)
    }

    private fun colorOf(level: ActivityAtlas.Level): Int = when (level) {
        ActivityAtlas.Level.NO_DATA -> palette.track
        ActivityAtlas.Level.LOW -> if (palette.dark) 0xFF40365E.toInt() else 0xFFE1D8F5.toInt()
        ActivityAtlas.Level.MID -> if (palette.dark) 0xFF7956C5.toInt() else 0xFFA687E6.toInt()
        ActivityAtlas.Level.HIGH -> if (palette.dark) 0xFFB08AFF.toInt() else 0xFF5B34B5.toInt()
    }

    override fun onDraw(canvas: Canvas) {
        val m = model ?: return
        val w = cellSize(width); val h = rowHeight(width, m.weeks)
        text.textSize = 10f * d; text.color = palette.muted
        for (c in 0 until 7) canvas.drawText(weekdays[c], paddingLeft + c * (w + gap) + w / 2, headerHeight - 5 * d, text)
        val showNumbers = m.weeks <= 8
        m.cells.forEachIndexed { i, cell ->
            val date = cell.date ?: return@forEachIndexed
            val col = i % 7; val row = i / 7
            val x = paddingLeft + col * (w + gap); val y = headerHeight + row * (h + gap)
            rect.set(x, y, x + w, y + h)
            fill.color = colorOf(cell.level)
            canvas.drawRoundRect(rect, 6 * d, 6 * d, fill)
            if (cell.partialToday) {        // today is not finished: dashed-looking outline in the accent colour
                stroke.color = palette.accent; canvas.drawRoundRect(rect, 6 * d, 6 * d, stroke)
            }
            if (showNumbers) {
                val label = cell.steps?.let { if (it >= 1000) "${oneDecimal.format(it / 1000.0)}k" else "$it" } ?: "–"
                text.textSize = 9.5f * d
                text.color = when {
                    cell.steps == null -> palette.muted
                    palette.dark -> if (cell.level == ActivityAtlas.Level.HIGH) 0xFF241447.toInt() else 0xFFFFFFFF.toInt()
                    else -> if (cell.level == ActivityAtlas.Level.LOW) 0xFF3B2C66.toInt() else 0xFFFFFFFF.toInt()
                }
                canvas.drawText(label, x + w / 2, y + h / 2 + 3.5f * d, text)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val m = model ?: return false
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val w = cellSize(width); val h = rowHeight(width, m.weeks)
            val col = ((e.x - paddingLeft) / (w + gap)).toInt(); val row = ((e.y - headerHeight) / (h + gap)).toInt()
            val cell = m.cells.getOrNull(row * 7 + col)
            if (col in 0..6 && row >= 0 && cell?.date != null) { performClick(); onCell(cell) }
        }
        return e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_UP
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}

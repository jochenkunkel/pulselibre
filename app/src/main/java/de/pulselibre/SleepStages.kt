package de.pulselibre

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Sleep stages from the band (FIT 275/0 `sleep_level`, canonical since #696). Owner decision 26.11.2026 for #329: show
 * WHEN the band reported which stage, but no sleep duration or "hours slept" – the intervals between the events are not
 * validated yet. Profile codes: 0 unmeasurable, 1 awake, 2 light, 3 deep, 4 REM.
 */
internal object SleepStages {
    const val METRIC = "sleep_level"
    const val WINDOW_MS = 24L * 3_600_000L
    private const val CACHE_MS = 60_000L

    data class Event(val atMs: Long, val code: Int)
    data class Night(val events: List<Event>) {
        val counts: Map<Int, Int> get() = events.groupingBy { it.code }.eachCount()
        val firstMs: Long? get() = events.firstOrNull()?.atMs
        val lastMs: Long? get() = events.lastOrNull()?.atMs
    }

    /** Keeps only profile codes 0–4 in time order; anything else is not a known stage and is not shown. */
    fun of(points: List<Triple<Long, Double, String>>): Night = Night(points.asSequence()
        .filter { it.third == "profile_code" && it.second.isFinite() && it.second == Math.floor(it.second) && it.second in 0.0..4.0 }
        .map { Event(it.first, it.second.toInt()) }.sortedBy { it.atMs }.toList())

    @Volatile private var cached: Pair<Long, Night>? = null

    /** Band sleep stages of the last 24 h; refreshed at most once a minute because the overview re-renders often. */
    fun read(context: Context, now: Long = System.currentTimeMillis()): Night {
        cached?.let { (at, night) -> if (now - at in 0 until CACHE_MS) return night }
        val night = CirqaDatabase(context).use { db ->
            db.ensureHistoricalStoreCopied()
            val unified = UnifiedMeasurements(db.writableDatabase)
            unified.refresh()
            of(unified.points(MeasurementOrigin.BAND, METRIC, now - WINDOW_MS, now + 1))
        }
        cached = now to night
        return night
    }

    fun invalidate() { cached = null }
}

/**
 * One tick per reported stage change at its time; ticks are not joined into bars, because a filled span would claim a
 * duration that is not validated. Higher ticks = lighter sleep (awake highest, deep lowest), REM between light and deep.
 */
internal class SleepStageStripView(context: Context, private val night: SleepStages.Night, private val colors: Map<Int, Int>,
                                   private val track: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private fun level(code: Int) = when (code) { 1 -> 0; 2 -> 1; 4 -> 2; 3 -> 3; else -> 4 } // awake top … unmeasurable bottom

    override fun onDraw(canvas: Canvas) {
        val first = night.firstMs ?: return
        val last = night.lastMs ?: return
        val span = (last - first).coerceAtLeast(60_000L).toFloat()
        val pad = 6 * density
        val rowH = (height - 2 * pad) / 5f
        paint.color = track; paint.strokeWidth = density
        for (row in 0..4) canvas.drawLine(pad, pad + rowH * (row + 0.5f), width - pad, pad + rowH * (row + 0.5f), paint)
        paint.strokeWidth = 3 * density; paint.strokeCap = Paint.Cap.ROUND
        for (e in night.events) {
            val x = pad + (width - 2 * pad) * ((e.atMs - first) / span)
            val y = pad + rowH * (level(e.code) + 0.5f)
            paint.color = colors[e.code] ?: track
            canvas.drawLine(x, y - rowH * 0.35f, x, y + rowH * 0.35f, paint)
        }
    }
}

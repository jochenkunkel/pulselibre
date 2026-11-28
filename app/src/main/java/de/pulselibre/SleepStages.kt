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
    /** Eight days, so the card can step back through the last week of nights (#762). */
    const val WINDOW_MS = 8L * 24L * 3_600_000L
    private const val CACHE_MS = 60_000L

    /** Longest span one reported stage may fill; a longer silence is shown as a gap, not as that stage (#762). */
    const val MAX_SEGMENT_MS = 2L * 3_600_000L
    /** Silence that separates two nights. */
    const val NIGHT_BREAK_MS = 4L * 3_600_000L

    const val AWAKE = 1; const val LIGHT = 2; const val DEEP = 3; const val REM = 4

    /** Legend and chart order, top to bottom (#762): awake, REM, light, deep. */
    val LEGEND_ORDER = listOf(AWAKE, REM, LIGHT, DEEP)

    /** Fixed stage colours (dark blue deep, light blue light, violet REM, magenta awake); rows and labels carry meaning too. */
    val COLORS: Map<Int, Int> = mapOf(DEEP to 0xFF1E3A8A.toInt(), LIGHT to 0xFF60A5FA.toInt(), REM to 0xFF8B5CF6.toInt(), AWAKE to 0xFFDB2777.toInt())

    data class Event(val atMs: Long, val code: Int)
    /** One stage from a reported change [startMs] until the next report [endMs) (half-open). */
    data class Segment(val startMs: Long, val endMs: Long, val code: Int)

    /**
     * Each stage report starts an interval that lasts until the next report. Same timestamp twice: the later report wins.
     * Code 0 (unmeasurable), spans longer than [maxSegmentMs] and the time after the last report stay empty; the last
     * report only closes the previous interval, because no end of sleep is reported. Equal neighbours are merged.
     */
    fun segments(events: List<Event>, maxSegmentMs: Long = MAX_SEGMENT_MS): List<Segment> {
        val dedup = events.sortedBy { it.atMs }.groupBy { it.atMs }.map { it.value.last() }
        val out = ArrayList<Segment>()
        for (i in 0 until dedup.size - 1) {
            val a = dedup[i]; val b = dedup[i + 1]
            if (a.code !in LEGEND_ORDER || b.atMs - a.atMs > maxSegmentMs) continue
            val prev = out.lastOrNull()
            if (prev != null && prev.code == a.code && prev.endMs == a.atMs) out[out.lastIndex] = prev.copy(endMs = b.atMs)
            else out.add(Segment(a.atMs, b.atMs, a.code))
        }
        return out
    }

    /** All nights, oldest first: the events split at every silence longer than [NIGHT_BREAK_MS]. */
    fun nights(night: Night): List<Night> {
        val out = ArrayList<Night>(); var cur = ArrayList<Event>()
        for (e in night.events) {
            if (cur.isNotEmpty() && e.atMs - cur.last().atMs > NIGHT_BREAK_MS) { out.add(Night(cur)); cur = ArrayList() }
            cur.add(e)
        }
        if (cur.isNotEmpty()) out.add(Night(cur))
        return out
    }

    /** Night for a picked calendar day: the night ending that morning, else the one starting that evening; -1 if none. */
    fun indexForDay(nights: List<Night>, day: java.time.LocalDate, zone: java.time.ZoneId): Int {
        val byMorning = nights.indexOfLast { dates(it, zone)?.second == day }
        return if (byMorning >= 0) byMorning else nights.indexOfLast { dates(it, zone)?.first == day }
    }

    /** The newest night: the events after the last silence longer than [NIGHT_BREAK_MS]. */
    fun latestNight(night: Night): Night {
        val e = night.events
        var from = 0
        for (i in 1 until e.size) if (e[i].atMs - e[i - 1].atMs > NIGHT_BREAK_MS) from = i
        return Night(e.subList(from, e.size))
    }

    /** Calendar dates of the night: evening date to morning date; equal only for daytime sleep (nap after noon). */
    fun dates(night: Night, zone: java.time.ZoneId): Pair<java.time.LocalDate, java.time.LocalDate>? {
        val first = night.firstMs ?: return null
        val last = night.lastMs ?: return null
        val start = java.time.Instant.ofEpochMilli(first).atZone(zone)
        val end = java.time.Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
        // A first report before noon belongs to the night that began the evening before (S10 28.11.: first report 00:57).
        val from = if (start.toLocalDate() == end && start.hour < 12) end.minusDays(1) else start.toLocalDate()
        return from to end
    }

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
 * Stage intervals as filled steps on a true time axis (#762): one row per stage (awake top, deep bottom), each interval
 * filled in its stage colour until the next report. Gaps stay empty; nothing is drawn after the last report.
 */
internal class SleepStageStripView(context: Context, private val night: SleepStages.Night, private val track: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    val segments = SleepStages.segments(night.events)

    override fun onDraw(canvas: Canvas) {
        val first = night.firstMs ?: return
        val last = night.lastMs ?: return
        val span = (last - first).coerceAtLeast(60_000L).toFloat()
        val pad = 6 * density
        val rows = SleepStages.LEGEND_ORDER.size
        val rowH = (height - 2 * pad) / rows
        paint.color = track; paint.strokeWidth = density; paint.style = Paint.Style.STROKE
        for (row in 0..rows) canvas.drawLine(pad, pad + rowH * row, width - pad, pad + rowH * row, paint)
        paint.style = Paint.Style.FILL
        fun x(t: Long) = pad + (width - 2 * pad) * ((t - first) / span)
        for (s in segments) {
            val row = SleepStages.LEGEND_ORDER.indexOf(s.code)
            paint.color = SleepStages.COLORS[s.code] ?: track
            // the bar reaches from its row down to the bottom, so the step height itself shows the stage
            canvas.drawRect(x(s.startMs), pad + rowH * row + density, maxOf(x(s.endMs), x(s.startMs) + density), height - pad, paint)
        }
    }
}

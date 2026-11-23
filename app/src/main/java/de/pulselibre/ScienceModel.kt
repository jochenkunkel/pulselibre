package de.pulselibre

import kotlin.math.sqrt

/** Quality of ONE point; mirrors docs/GUI_DATA_CONTRACT_DRAFT_2026-11-23.md (draft, to be confirmed by the FIT agent). */
enum class SeriesQuality {
    /** Documented measurement. */
    VALID,
    /** Documented sentinel (e.g. heart rate 0): not a measurement, never drawn as a value. */
    INVALID,
    /** Device status instead of a value (e.g. off wrist, large motion): named, never drawn as a value. */
    STATUS,
    /** Value with reduced confidence/mode information; drawn marked. */
    LIMITED,
    /** Plausible value whose meaning/quality is not established (e.g. untrusted FIT candidate); drawn marked. */
    UNVERIFIED
}

/** Where a series comes from. SYNTHETIC data exists only for development previews and must always be labelled. */
enum class SeriesOrigin { LIVE, IMPORT, BAND, CANDIDATE, SYNTHETIC }

/** Clock a series' timestamps follow; Band and phone clocks are never mixed. */
enum class SeriesTimeBasis { BAND_CLOCK, PHONE_RECEPTION, UNKNOWN, SYNTHETIC }

/** [timeMs] null = time unknown (counted, not drawn); [value] null = no measurement; [rawCode] keeps the stored status/sentinel code. */
data class SeriesPoint(
    val timeMs: Long?, val value: Double?, val quality: SeriesQuality = SeriesQuality.VALID, val rawCode: Long? = null,
    /** >1 when this point summarises [count] stored values of one time bucket ([value] = their mean, [low]/[high] = their extremes). */
    val count: Int = 1, val low: Double? = null, val high: Double? = null
)

data class SeriesData(
    val metricKey: String,
    val unit: String,
    val origin: SeriesOrigin,
    val timeBasis: SeriesTimeBasis,
    val rangeStartMs: Long,
    val rangeEndMs: Long,
    /** Nominal spacing of measurements, null when unknown (then it is inferred from the data, never assumed). */
    val nominalIntervalMs: Long?,
    val points: List<SeriesPoint>
)

data class SeriesGap(val startMs: Long, val endMs: Long)

/** Why a series cannot (fully) be shown; the dashboard turns each state into an explaining text. */
enum class SeriesState { EMPTY, NO_VALID_VALUES, INCOMPLETE, COMPLETE }

data class SeriesAssessment(
    val state: SeriesState,
    val total: Int, val valid: Int, val invalid: Int, val status: Int, val limited: Int, val unverified: Int, val unknownTime: Int,
    /** Share of fixed buckets in the range that contain at least one valid/limited/unverified value; 0 when nothing to measure against. */
    val coverage: Double,
    val gaps: List<SeriesGap>,
    val minimum: Double?, val maximum: Double?, val mean: Double?, val standardDeviation: Double?,
    val intervalMs: Long?
)

/** Pure analysis of a series: counts, coverage, gaps and plain statistics over drawn values only. No thresholds, no interpolation. */
object SeriesAnalysis {
    private const val GAP_FACTOR = 3L
    private const val MIN_INTERVAL_MS = 1_000L

    fun assess(series: SeriesData, bucketMs: Long? = null): SeriesAssessment {
        val points = series.points
        fun count(q: SeriesQuality) = points.filter { it.quality == q && (q in NON_VALUE || it.value != null) }.sumOf { it.count }
        val unknownTime = points.count { it.timeMs == null }
        // Drawn = has a time, a finite value and a quality that represents a value.
        val drawn = points.filter { it.timeMs != null && it.value != null && it.value.isFinite() && it.quality !in NON_VALUE }
            .filter { it.timeMs!! in series.rangeStartMs..series.rangeEndMs }
            .sortedBy { it.timeMs }
        val interval = (series.nominalIntervalMs ?: inferInterval(drawn.map { it.timeMs!! })).takeIf { it != null && it >= MIN_INTERVAL_MS }
        val gaps = if (interval == null) emptyList() else findGaps(drawn.map { it.timeMs!! }, series.rangeStartMs, series.rangeEndMs, interval * GAP_FACTOR)
        val bucket = (bucketMs ?: interval ?: 0L)
        val coverage = if (bucket <= 0L || series.rangeEndMs <= series.rangeStartMs) 0.0 else {
            val buckets = ((series.rangeEndMs - series.rangeStartMs + bucket - 1) / bucket).coerceAtLeast(1)
            drawn.map { (it.timeMs!! - series.rangeStartMs) / bucket }.toSet().size.toDouble() / buckets
        }.coerceIn(0.0, 1.0)
        val validPoints = drawn.filter { it.quality == SeriesQuality.VALID }
        val weight = validPoints.sumOf { it.count }
        val mean = if (weight > 0) validPoints.sumOf { it.value!! * it.count } / weight else null
        val aggregated = validPoints.any { it.count > 1 }
        // Standard deviation only from single stored values; a bucketed series would understate it, so it is not reported.
        val sd = if (!aggregated && validPoints.size >= 2 && mean != null) sqrt(validPoints.sumOf { (it.value!! - mean) * (it.value!! - mean) } / (validPoints.size - 1)) else null
        val minimum = validPoints.minOfOrNull { it.low ?: it.value!! }
        val maximum = validPoints.maxOfOrNull { it.high ?: it.value!! }
        val state = when {
            points.isEmpty() -> SeriesState.EMPTY
            drawn.isEmpty() -> SeriesState.NO_VALID_VALUES
            gaps.isNotEmpty() || coverage < 1.0 -> SeriesState.INCOMPLETE
            else -> SeriesState.COMPLETE
        }
        return SeriesAssessment(state, points.sumOf { it.count }, count(SeriesQuality.VALID), count(SeriesQuality.INVALID), count(SeriesQuality.STATUS),
            count(SeriesQuality.LIMITED), count(SeriesQuality.UNVERIFIED), unknownTime, coverage, gaps,
            minimum, maximum, mean, sd, interval)
    }

    private val NON_VALUE = setOf(SeriesQuality.INVALID, SeriesQuality.STATUS)

    private fun inferInterval(times: List<Long>): Long? {
        if (times.size < 3) return null
        val deltas = times.zipWithNext { a, b -> b - a }.filter { it > 0 }.sorted()
        return deltas.getOrNull(deltas.size / 2)
    }

    private fun findGaps(times: List<Long>, start: Long, end: Long, maxSilence: Long): List<SeriesGap> {
        val gaps = ArrayList<SeriesGap>()
        var previous = start
        for (t in times) { if (t - previous > maxSilence) gaps += SeriesGap(previous, t); previous = t }
        if (end - previous > maxSilence) gaps += SeriesGap(previous, end)
        return gaps
    }
}

/** Sleep phases as documented by the data model; UNKNOWN = interval without established phase. */
enum class SleepPhase { AWAKE, LIGHT, DEEP, REM, UNKNOWN }

data class SleepInterval(val startMs: Long, val endMs: Long, val phase: SleepPhase, val origin: SeriesOrigin = SeriesOrigin.SYNTHETIC)

data class SleepAssessment(
    val spanStartMs: Long?, val spanEndMs: Long?,
    val phaseMs: Map<SleepPhase, Long>,
    /** AWAKE intervals with sleep before AND after them. */
    val interruptions: List<SleepInterval>,
    /** Uncovered time between intervals: no data, NOT "awake". */
    val gaps: List<SeriesGap>,
    /** Intervals overlapping their predecessor (data problem, reported instead of hidden). */
    val overlaps: Int
)

object SleepAnalysis {
    private val SLEEP = setOf(SleepPhase.LIGHT, SleepPhase.DEEP, SleepPhase.REM)

    fun assess(input: List<SleepInterval>): SleepAssessment {
        val sorted = input.filter { it.endMs > it.startMs }.sortedBy { it.startMs }
        if (sorted.isEmpty()) return SleepAssessment(null, null, emptyMap(), emptyList(), emptyList(), 0)
        val phaseMs = SleepPhase.entries.associateWith { p -> sorted.filter { it.phase == p }.sumOf { it.endMs - it.startMs } }.filterValues { it > 0 }
        val gaps = ArrayList<SeriesGap>()
        var overlaps = 0
        var reach = sorted.first().endMs
        for (i in 1 until sorted.size) {
            val s = sorted[i]
            if (s.startMs < reach) overlaps++ else if (s.startMs > reach) gaps += SeriesGap(reach, s.startMs)
            reach = maxOf(reach, s.endMs)
        }
        val firstSleep = sorted.indexOfFirst { it.phase in SLEEP }
        val lastSleep = sorted.indexOfLast { it.phase in SLEEP }
        val interruptions = if (firstSleep < 0 || lastSleep <= firstSleep) emptyList()
            else sorted.subList(firstSleep + 1, lastSleep).filter { it.phase == SleepPhase.AWAKE }
        return SleepAssessment(sorted.first().startMs, reach, phaseMs, interruptions, gaps, overlaps)
    }
}

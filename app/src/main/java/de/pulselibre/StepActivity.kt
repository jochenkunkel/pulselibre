package de.pulselibre

import java.time.Instant
import java.time.ZoneId

/** Cumulative step-counter observation with the phone reception timestamp. */
data class StepObservation(val receivedAtEpochMillis: Long, val cumulativeSteps: Long)

/**
 * User-configurable display heuristic. Defaults implement the requested strict cutoffs:
 * below 10 steps/minute is Ruhe and above 100 is Sport; the boundary values are Bewegung.
 * These labels are activity proxies, not measured posture, exercise, or medical states.
 */
data class ActivityDetectionSettings(
    val restBelowStepsPerMinute: Int = 10,
    val sportAboveStepsPerMinute: Int = 100,
    val windowMinutes: Int = 5,
    val maxGapMinutes: Int = 3,
    val minimumSpanSeconds: Int = 30
) {
    fun isValid(): Boolean = restBelowStepsPerMinute in 0..99 &&
        sportAboveStepsPerMinute in (restBelowStepsPerMinute + 1)..300 &&
        windowMinutes in 2..15 && maxGapMinutes in 1..10 && minimumSpanSeconds in 15..120
}

/** Only an estimate from fresh, cumulative step snapshots; UNKNOWN is used for insufficient data. */
data class StepActivityEstimate(
    val context: PulseContext,
    val cadenceStepsPerMinute: Double? = null,
    val sampleCount: Int = 0,
    val spanSeconds: Long = 0,
    val explanation: String = "Noch keine frischen Schrittwerte"
) {
    val caption: String get() = cadenceStepsPerMinute?.let {
        "Schrittbasiert · Ø ${it.toInt()} Schritte/min · ${spanSeconds / 60} min"
    } ?: "Aktivität unbekannt · $explanation"

    companion object {
        val UNKNOWN = StepActivityEstimate(PulseContext.UNKNOWN)
    }
}

/**
 * PULSELIBRE EXPERIMENTAL HEURISTIC — NOT CLINICALLY VALIDATED (version 1).
 * Computes (last cumulative count − first count) × 60,000 / elapsed milliseconds over
 * the newest contiguous, local-calendar-day segment inside a five-minute window. Conflicting
 * timestamps, counter decreases (resets), stale latest samples, gaps, future records, invalid
 * UInt32 counters, and spans shorter than 30 seconds fail closed to UNKNOWN. The configurable
 * 10/100 steps/min cutoffs are user display preferences, not thresholds endorsed by research.
 * Sparse CIRQA snapshots cannot establish sitting or exercise type; a stable counter only means
 * no steps were observed in this sample window. CADENCE-adults reports heuristic walking
 * intensity cutoffs in controlled adult walking bouts, not a classifier for arbitrary activities:
 * Tudor-Locke et al. (2020), doi:10.1186/s12966-020-01045-z.
 */
fun estimateStepActivity(
    observations: List<StepObservation>,
    now: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    settings: ActivityDetectionSettings = ActivityDetectionSettings()
): StepActivityEstimate {
    if (!settings.isValid()) return StepActivityEstimate(PulseContext.UNKNOWN, explanation = "Einstellungen ungültig")
    val nowMillis = now.toEpochMilli()
    val today = now.atZone(zone).toLocalDate()
    val windowStart = nowMillis - settings.windowMinutes * 60_000L
    val eligible = observations.asSequence()
        .filter { it.receivedAtEpochMillis in windowStart..nowMillis }
        .filter { it.cumulativeSteps in 0L..0xFFFF_FFFFL }
        .filter { Instant.ofEpochMilli(it.receivedAtEpochMillis).atZone(zone).toLocalDate() == today }
        .groupBy { it.receivedAtEpochMillis }
        .toSortedMap()
    if (eligible.values.any { rows -> rows.map { it.cumulativeSteps }.distinct().size > 1 }) {
        return StepActivityEstimate(PulseContext.UNKNOWN, explanation = "Widersprüchliche Schrittwerte")
    }
    val sorted = eligible.values.map { it.first() }.sortedBy { it.receivedAtEpochMillis }
    val latest = sorted.lastOrNull()
        ?: return StepActivityEstimate(PulseContext.UNKNOWN, explanation = "Keine frischen Schrittwerte")
    val age = nowMillis - latest.receivedAtEpochMillis
    if (age > settings.maxGapMinutes * 60_000L) {
        return StepActivityEstimate(PulseContext.UNKNOWN, explanation = "Schrittwerte veraltet")
    }

    val segment = mutableListOf(latest)
    var newer = latest
    for (previous in sorted.asReversed().drop(1)) {
        val gap = newer.receivedAtEpochMillis - previous.receivedAtEpochMillis
        if (gap > settings.maxGapMinutes * 60_000L || previous.cumulativeSteps > newer.cumulativeSteps) break
        segment.add(previous)
        newer = previous
    }
    val first = segment.last()
    val spanMillis = latest.receivedAtEpochMillis - first.receivedAtEpochMillis
    if (segment.size < 2 || spanMillis < settings.minimumSpanSeconds * 1_000L) {
        return StepActivityEstimate(PulseContext.UNKNOWN, sampleCount = segment.size,
            spanSeconds = spanMillis / 1_000L, explanation = "Zu wenige frische Schrittwerte")
    }
    val cadence = (latest.cumulativeSteps - first.cumulativeSteps) * 60_000.0 / spanMillis
    val context = when {
        cadence < settings.restBelowStepsPerMinute -> PulseContext.REST
        cadence > settings.sportAboveStepsPerMinute -> PulseContext.SPORT
        else -> PulseContext.MOVEMENT
    }
    return StepActivityEstimate(context, cadence, segment.size, spanMillis / 1_000L,
        "Schrittbasierte Einordnung · heuristisch")
}

/** Local-only preferences, kept separate from personal body-profile fields. */
object ActivityDetectionPreference {
    private const val STORE = "activity_detection"

    fun read(context: android.content.Context): ActivityDetectionSettings {
        val prefs = context.getSharedPreferences(STORE, android.content.Context.MODE_PRIVATE)
        return ActivityDetectionSettings(
            restBelowStepsPerMinute = prefs.getInt("rest_below", 10),
            sportAboveStepsPerMinute = prefs.getInt("sport_above", 100),
            windowMinutes = prefs.getInt("window_minutes", 5)
        ).takeIf { it.isValid() } ?: ActivityDetectionSettings()
    }

    fun save(context: android.content.Context, settings: ActivityDetectionSettings) {
        require(settings.isValid())
        context.getSharedPreferences(STORE, android.content.Context.MODE_PRIVATE).edit()
            .putInt("rest_below", settings.restBelowStepsPerMinute)
            .putInt("sport_above", settings.sportAboveStepsPerMinute)
            .putInt("window_minutes", settings.windowMinutes)
            .apply()
    }
}

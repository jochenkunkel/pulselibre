package de.pulselibre

/** Presentation only: preserves received values and notes without calculating health scores. */
data class DashboardOverview(
    val vitalSigns: List<DashboardMetric>,
    val sleep: DashboardMetric,
    val steps: DashboardMetric,
    val stepGoal: DashboardMetric,
    val details: List<DashboardMetric>
)

fun dashboardOverview(metrics: List<DashboardMetric>): DashboardOverview {
    // Select explicitly rather than relying on the database reader's list positions.
    val vitalTitles = listOf("Herzfrequenz", "Sauerstoffsättigung · SpO₂")
    val todayTitles = listOf("Schritte (Tageszähler)", "Schrittziel")
    fun select(title: String) = metrics.firstOrNull { it.title == title }
        ?: DashboardMetric(title, "Keine Messdaten", "Nicht gemessen oder ungültig")
    val steps = select("Schritte (Tageszähler)")
    val goal = select("Schrittziel")
    val featured = (vitalTitles + todayTitles + "Schlaf").toSet()
    return DashboardOverview(
        vitalTitles.map(::select),
        metrics.firstOrNull { it.title == "Schlaf" }
            ?: DashboardMetric("Schlaf", "Keine Messdaten", "Schlafdauer und Schlafphasen noch nicht verfügbar"),
        steps,
        goal,
        metrics.filter { it.title !in featured }
    )
}

/**
 * Device-goal progress is a product display derived from the latest observed daily counter
 * and device-provided goal. It is not a population target, activity recommendation, or health score.
 * Percent is kept above 100 when the goal is exceeded; the scale drawing may clamp its fill.
 */
internal fun stepGoalProgress(steps: DashboardMetric, goal: DashboardMetric): Double? {
    val count = steps.value.toLongOrNull()?.takeIf { it >= 0 } ?: return null
    val target = goal.value.toLongOrNull()?.takeIf { it > 0 } ?: return null
    return 100.0 * count / target
}

/** Marker in [DashboardMetric.note] of a step goal the user set in the settings (#266). */
internal const val LOCAL_STEP_GOAL_NOTE = "Eigenes Ziel (Einstellungen)"

/**
 * A step goal the user entered in the settings takes precedence over the device goal; both are shown with their
 * source. Invalid input (non-numeric, outside 100..100000 as in the settings dialog) is ignored, never "repaired".
 */
internal fun applyLocalStepGoal(metrics: List<DashboardMetric>, localRaw: String?): List<DashboardMetric> {
    val local = localStepGoal(localRaw) ?: return metrics
    val goal = DashboardMetric("Schrittziel", local.toString(), LOCAL_STEP_GOAL_NOTE)
    return if (metrics.any { it.title == "Schrittziel" }) metrics.map { if (it.title == "Schrittziel") goal else it }
    else metrics + goal
}

/** Reached-goal text for the step tile; `null` when steps or goal are unknown. Never a health judgement. */
internal fun stepGoalStatus(steps: DashboardMetric, goal: DashboardMetric): String? {
    val progress = stepGoalProgress(steps, goal) ?: return null
    return if (progress >= 100.0) "Tagesziel erreicht" else null
}

/** The user's own step goal from the settings (100..100000) or null; invalid input is ignored, never repaired. */
internal fun localStepGoal(raw: String?): Int? = raw?.trim()?.toIntOrNull()?.takeIf { it in 100..100_000 }

/** Goal used by charts: the user's own goal wins over the device goal (#265). */
internal fun effectiveStepGoal(localRaw: String?, deviceGoal: Long?): Long? =
    localStepGoal(localRaw)?.toLong() ?: deviceGoal?.takeIf { it > 0 }

/**
 * Regularity (#41): on how many of the complete days before today with a step value the user's own goal was reached.
 * Days without data are neither "reached" nor "missed"; they are not counted at all. No score, no rating.
 */
internal object GoalRegularity {
    data class Result(val reached: Int, val validDays: Int, val windowDays: Int, val goal: Int) {
        fun text(): String? =
            if (validDays == 0) null
            else "Eigenes Ziel (${java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(goal)}) erreicht an " +
                "$reached von $validDays Tagen mit Daten ($windowDays-Tage-Fenster)"
    }

    fun build(daily: Map<java.time.LocalDate, Double>, today: java.time.LocalDate, windowDays: Int, goal: Int): Result {
        val values = (1..windowDays).mapNotNull { daily[today.minusDays(it.toLong())]?.takeIf { v -> v.isFinite() && v >= 0.0 } }
        return Result(values.count { it >= goal }, values.size, windowDays, goal)
    }
}

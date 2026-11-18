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

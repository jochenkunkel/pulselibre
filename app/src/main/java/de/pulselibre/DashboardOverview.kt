package de.pulselibre

/** Presentation only: preserves received values and notes without calculating health scores. */
data class DashboardOverview(
    val vitalSigns: List<DashboardMetric>,
    val sleep: DashboardMetric,
    val today: List<DashboardMetric>,
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
    val progress = stepGoalProgressMetric(steps, goal)
    val featured = (vitalTitles + todayTitles + "Schlaf").toSet()
    return DashboardOverview(
        vitalTitles.map(::select),
        metrics.firstOrNull { it.title == "Schlaf" }
            ?: DashboardMetric("Schlaf", "Keine Messdaten", "Schlafdauer und Schlafphasen noch nicht verfügbar"),
        listOf(steps, goal, progress),
        metrics.filter { it.title !in featured }
    )
}

/**
 * Device-goal progress is a product display derived from the latest observed daily counter
 * and device-provided goal. It is not a population target, activity recommendation, or health score.
 * Percent is kept above 100 when the goal is exceeded; the scale drawing may clamp its fill.
 */
internal fun stepGoalProgressMetric(steps: DashboardMetric, goal: DashboardMetric): DashboardMetric {
    val stepsValue = steps.value.toLongOrNull()?.takeIf { it >= 0 }
    val goalValue = goal.value.toLongOrNull()?.takeIf { it > 0 }
    val receivedAt = listOfNotNull(steps.receivedAt, goal.receivedAt).maxOrNull()
    if (stepsValue == null || goalValue == null) {
        return DashboardMetric(
            "Tagesziel Schritte", "Keine Messdaten",
            "Fortschritt nur mit gültigem Tageszähler und Geräte-Schrittziel verfügbar.",
            receivedAt
        )
    }
    val percent = ((stepsValue.toDouble() / goalValue.toDouble()) * 100.0).toInt()
    return DashboardMetric(
        "Tagesziel Schritte", "$percent %",
        "$stepsValue von $goalValue Schritten · Fortschritt zum Geräte-Schrittziel; keine Gesundheitsbewertung.",
        receivedAt
    )
}

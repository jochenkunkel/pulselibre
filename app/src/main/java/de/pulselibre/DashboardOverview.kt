package de.pulselibre

/** Presentation only: preserves received values and notes without calculating health scores. */
data class DashboardOverview(
    val vitalSigns: List<DashboardMetric>,
    val sleep: DashboardMetric,
    val today: List<DashboardMetric>,
    val details: List<DashboardMetric>
)

fun dashboardOverview(metrics: List<DashboardMetric>): DashboardOverview {
    // These titles are the existing CirqaDashboardReader presentation contract.
    // Select explicitly rather than relying on the database reader's list positions.
    val vitalTitles = listOf("Herzfrequenz", "Sauerstoffsättigung · SpO₂")
    val todayTitles = listOf("Schritte (Tageszähler)", "Schrittziel")
    fun select(title: String) = metrics.firstOrNull { it.title == title }
        ?: DashboardMetric(title, "Keine Messdaten", "Nicht gemessen oder ungültig")
    val featured = (vitalTitles + todayTitles + "Schlaf").toSet()
    return DashboardOverview(
        vitalTitles.map(::select),
        metrics.firstOrNull { it.title == "Schlaf" }
            ?: DashboardMetric("Schlaf", "Keine Messdaten", "Schlafdauer und Schlafphasen noch nicht verfügbar"),
        todayTitles.map(::select),
        metrics.filter { it.title !in featured }
    )
}

package de.pulselibre

/** Pure presentation model. Missing readings must never be replaced with plausible demo values. */
data class HealthSnapshot(
    val pulseBpm: Int? = null,
    val oxygenPercent: Int? = null,
    val steps: Int? = null,
    val sleepMinutes: Int? = null
)

data class DashboardMetric(val title: String, val value: String, val note: String)

fun dashboardMetrics(snapshot: HealthSnapshot): List<DashboardMetric> = listOf(
    DashboardMetric("Herzfrequenz", snapshot.pulseBpm?.takeIf { it in 20..250 }?.let { "$it bpm" } ?: "Keine Messdaten", "Nur echte Messwerte"),
    DashboardMetric("Sauerstoffsättigung · SpO₂", snapshot.oxygenPercent?.takeIf { it in 0..100 }?.let { "$it %" } ?: "Keine Messdaten", "Verlauf erst bei verfügbaren Zeitreihen"),
    DashboardMetric("Schritte", snapshot.steps?.takeIf { it >= 0 }?.toString() ?: "Keine Messdaten", "Keine erfundenen Schrittziele"),
    DashboardMetric("Schlaf", snapshot.sleepMinutes?.takeIf { it >= 0 }?.let { "${it / 60} h ${it % 60} min" } ?: "Keine Messdaten", "Keine Diagnose"),
    DashboardMetric("Ground Rules", "WHO · DGE", "Quellenbasierte Orientierung; Profil noch nicht eingerichtet"),
    DashboardMetric("Ernährung & Energie", "Noch nicht verfügbar", "Keine Kalorienschätzung ohne belastbare Daten")
)

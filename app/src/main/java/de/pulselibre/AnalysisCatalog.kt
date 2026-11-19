package de.pulselibre

/**
 * Information architecture of the main tab "Analysen" (#227): five module groups. A module without verified data is shown
 * as "Noch nicht verfügbar" with a stated reason; nothing here computes values or invents health data.
 */
internal object AnalysisCatalog {
    enum class Category(val number: Int, val title: String, val subtitle: String) {
        HEART(1, "Herz & Kreislauf", "Ruhepuls, Fitnessentwicklung, Sauerstoff"),
        SLEEP(2, "Schlaf & Erholung", "Nacht, Folgetag, verzögerte Erholung"),
        MOVEMENT(3, "Bewegung & Entwicklung", "Aktivität, Fortschritt, 90-Tage-Trends"),
        PATTERNS(4, "Zusammenhänge & Muster", "Korrelationen, Tagesmuster, Brüche"),
        QUALITY(5, "Datenqualität & Methoden", "Abdeckung, Herkunft, Grenzen")
    }

    enum class Availability(val label: String) { AVAILABLE("Verfügbar"), PARTIAL("Teilweise"), UNAVAILABLE("Noch nicht verfügbar") }

    /** Where a tap leads. INLINE = the module is shown on this page; HISTORY = the Verlauf tab; METHODS = method dialog. */
    enum class Route { NONE, INLINE, HISTORY, METHODS }

    enum class ModuleId {
        REST_FITNESS, HRV, HR_RECOVERY, SPO2_STABILITY,
        SLEEP_RECOVERY, ACTIVITY_NEXT_NIGHT, DELAYED_RECOVERY,
        MATCHED_EXERTION, TREND_90, ACTIVITY_SIGNATURE,
        CORRELATION, CIRCADIAN, CHANGE_POINT, CLUSTERS,
        COVERAGE, METHODS
    }

    data class Module(
        val id: ModuleId, val category: Category, val title: String, val availability: Availability,
        val note: String, val route: Route, val ticket: String, val icon: PlIcon
    )

    val modules: List<Module> = listOf(
        Module(ModuleId.REST_FITNESS, Category.HEART, "Ruhepuls · Fitnessentwicklung", Availability.PARTIAL,
            "Geräte-Ruhepuls, Median, Vergleich mit der Vorperiode (7/28/90 Tage). Hängt von der Historie ab.", Route.INLINE, "#41", PlIcon.HEART),
        Module(ModuleId.HRV, Category.HEART, "Herzfrequenzvariabilität", Availability.UNAVAILABLE,
            "Es gibt keine verifizierte RR-Tagesreihe; HRV wird nicht aus Pulswerten geschätzt.", Route.NONE, "#47", PlIcon.RR_INTERVAL),
        Module(ModuleId.HR_RECOVERY, Category.HEART, "Herzfrequenz-Erholung", Availability.UNAVAILABLE,
            "Braucht vergleichbare Belastungseinheiten mit Belastungsmetadaten; Schritte und Puls allein reichen nicht.", Route.NONE, "#46", PlIcon.RECOVERY),
        Module(ModuleId.SPO2_STABILITY, Category.HEART, "Sauerstoffstabilität (SpO₂)", Availability.PARTIAL,
            "SpO₂-Verlauf ist im Reiter Verlauf sichtbar; eine nächtliche Auswertung ist nichtdiagnostisch und noch begrenzt.", Route.HISTORY, "#39", PlIcon.OXYGEN),
        Module(ModuleId.SLEEP_RECOVERY, Category.SLEEP, "Schlaf und Regeneration", Availability.UNAVAILABLE,
            "Es gibt keine verifizierte Schlafquelle; Schlafphasen werden nicht geschätzt.", Route.NONE, "#43", PlIcon.RECOVERY),
        Module(ModuleId.ACTIVITY_NEXT_NIGHT, Category.SLEEP, "Aktivität → folgende Nacht", Availability.UNAVAILABLE,
            "Braucht verifizierte Schlaf-/Nachtintervalle (Historien-Download).", Route.NONE, "#44", PlIcon.SESSIONS),
        Module(ModuleId.DELAYED_RECOVERY, Category.SLEEP, "Verzögerte Erholung (Lag)", Availability.UNAVAILABLE,
            "Braucht Schlaf- und Belastungsdaten über längere Zeiträume.", Route.NONE, "#44", PlIcon.HISTORY),
        Module(ModuleId.MATCHED_EXERTION, Category.MOVEMENT, "Fitnessfortschritt bei vergleichbarer Belastung", Availability.UNAVAILABLE,
            "Ohne verifizierte Belastungsmetadaten keine Ergebnis-Kennzahl („Keine ausreichend vergleichbaren Einheiten“).", Route.NONE, "#46", PlIcon.FITNESS),
        Module(ModuleId.TREND_90, Category.MOVEMENT, "90-Tage-Trends", Availability.PARTIAL,
            "Schalter 90 T im Trend-Block; es wird nie extrapoliert – fehlende Tage bleiben leer.", Route.INLINE, "#47", PlIcon.TRENDS),
        Module(ModuleId.ACTIVITY_SIGNATURE, Category.MOVEMENT, "Aktivitäts-Atlas (Schritte)", Availability.PARTIAL,
            "Kalenderraster der Tages-Schrittstände; graue Zelle = keine verifizierten Daten.", Route.INLINE, "#41", PlIcon.STEPS),
        Module(ModuleId.CORRELATION, Category.PATTERNS, "Schritte ↔ Ruhepuls am Folgetag", Availability.PARTIAL,
            "Explorativ (Spearman, 95-%-Intervall, Robustheit); ab 30 gepaarten Tagen, nie als Ursache zu lesen.", Route.INLINE, "#45", PlIcon.TREND_LINK),
        Module(ModuleId.CIRCADIAN, Category.PATTERNS, "Zirkadiane Tagesmuster", Availability.UNAVAILABLE,
            "Forschungs-Backlog: noch keine freigegebene Methode.", Route.NONE, "#219", PlIcon.CALENDAR),
        Module(ModuleId.CHANGE_POINT, Category.PATTERNS, "Change-Point-Analyse", Availability.UNAVAILABLE,
            "Forschungs-Backlog: braucht deutlich längere Historie.", Route.NONE, "#47", PlIcon.TRENDS),
        Module(ModuleId.CLUSTERS, Category.PATTERNS, "Explorative Cluster", Availability.UNAVAILABLE,
            "Forschungs-Backlog: keine Cluster ohne validierte Merkmale.", Route.NONE, "#45", PlIcon.OVERVIEW),
        Module(ModuleId.COVERAGE, Category.QUALITY, "Datenabdeckung", Availability.AVAILABLE,
            "Ruhepuls und Schritte getrennt: „x von y Tagen“ im Trend-Block.", Route.INLINE, "#41", PlIcon.DATA),
        Module(ModuleId.METHODS, Category.QUALITY, "Methoden und Grenzen", Availability.AVAILABLE,
            "Quellen, Zeitbezug, Annahmen und was die Auswertungen nicht aussagen.", Route.METHODS, "#227", PlIcon.INFO)
    )

    fun byCategory(): Map<Category, List<Module>> = Category.entries.associateWith { c -> modules.filter { it.category == c } }

    const val METHODS_TEXT = "• Quellen: nur lokal gespeicherte, vom Gerät gelieferte Werte (Ruhepuls vom Gerät berechnet, Schrittzähler kumulativ je Tag; letzter gültiger Tagesstand).\n" +
        "• Zeitbezug: Empfangszeit auf dem Telefon, lokale Kalendertage; heute ist unvollständig.\n" +
        "• Lücken bleiben Lücken: fehlende Tage werden nicht geschätzt, nicht verbunden und nicht als 0 gezählt.\n" +
        "• Vergleiche: nicht überlappende, gleich lange Vorperiode; Mindesttage 4 / 14 / 45 (7 / 28 / 90 Tage) – eine begründete Produktheuristik, kein wissenschaftlicher Grenzwert.\n" +
        "• Zusammenhänge sind explorativ und beschreiben nur diese Daten; sie sagen nichts über Ursache und Wirkung.\n" +
        "• Keine Diagnose, kein Fitness- oder Gesundheitsscore, keine medizinische Bewertung."
}

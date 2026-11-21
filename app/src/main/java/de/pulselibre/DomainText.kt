package de.pulselibre

import android.content.Context

/**
 * Display text for fixed phrases produced by the domain layer. The model classes keep their German phrases as stable,
 * test-pinned identifiers (like [MetricText] does for metric titles); the views translate them here (#i18n).
 * Unknown phrases are shown unchanged, so a new domain phrase never disappears from the screen.
 */
internal object DomainText {
    private val exact = mapOf(
        "Modus unbekannt" to R.string.dom_mode_unknown,
        "Ruhe" to R.string.dom_mode_rest,
        "Leichte Bewegung" to R.string.dom_mode_movement,
        "Sport" to R.string.dom_mode_sport,
        "Noch keine frischen Schrittwerte" to R.string.dom_expl_none,
        "Einstellungen ungültig" to R.string.dom_expl_settings,
        "Widersprüchliche Schrittwerte" to R.string.dom_expl_conflict,
        "Keine frischen Schrittwerte" to R.string.dom_expl_nofresh,
        "Schrittwerte veraltet" to R.string.dom_expl_stale,
        "Zu wenige frische Schrittwerte" to R.string.dom_expl_few,
        "Schrittbasierte Einordnung · heuristisch" to R.string.dom_expl_heuristic,
        "Unterhalb der Anzeigeskala" to R.string.dom_gauge_below,
        "Oberhalb der Anzeigeskala" to R.string.dom_gauge_above,
        "Messwertanzeige · keine Zielzone" to R.string.dom_gauge_plain,
        "Keine Messdaten" to R.string.metric_no_data,
        "Erwachsene in Ruhe" to R.string.dom_ref_adults_rest,
        "Durchschnittliche Erwachsene, in Ruhe" to R.string.dom_ref_avg_adults_rest,
        "Der aktuelle Puls ist aktivitätsabhängig; die Ruhe-Referenz ist hier nur Kontext." to R.string.dom_cav_pulse,
        "Allgemeine Orientierung, kein persönlicher Ziel- oder Diagnosewert." to R.string.dom_cav_resting,
        "Finger-Pulsoximeter-Orientierung; nicht für CIRQA am Handgelenk validiert." to R.string.dom_cav_spo2,
        "Gilt für Erwachsene in Ruhe; Momentanwert kann durch Aktivität abweichen." to R.string.dom_cav_resp,
        "Proprietäre Geräteskala; kein klinischer Normbereich." to R.string.dom_cav_device_scale,
        "Einzelnes RR-Intervall; keine HRV-Kennzahl und kein Normvergleich." to R.string.dom_cav_rr,
        "Fortschritt zum geräteeigenen Tagesziel, keine WHO-Norm." to R.string.dom_cav_steps,
        "Produktfortschritt zum vom Gerät gelieferten Tagesziel; keine Gesundheitsbewertung oder Bewegungsempfehlung." to R.string.dom_cav_goal,
        "Schritte" to R.string.dom_unit_steps,
        "24 Stunden" to R.string.hv_range_day,
        "7 Tage" to R.string.hv_range_week,
        "30 Tage" to R.string.hv_range_month,
        "15 min" to R.string.hv_res_15m,
        "2 Std." to R.string.hv_res_2h,
        "1 Std." to R.string.hv_res_1h,
        "6 Std." to R.string.hv_res_6h,
        "1 Tag" to R.string.hv_res_1d,
        "Punkte" to R.string.hv_mode_points,
        "Verteilung" to R.string.hv_mode_density,
        "Farbsicher" to R.string.hv_mode_safe,
        "Herzfrequenz · Band-Verlauf (FIT)" to R.string.hv_title_fit,
        "Herzfrequenz" to R.string.metric_pulse,
        "Sauerstoffsättigung · SpO₂" to R.string.metric_spo2,
        HistoryOverviewView.TimeBasis.PHONE_RECEIVED.legend to R.string.hv_basis_phone_legend,
        HistoryOverviewView.TimeBasis.PHONE_RECEIVED.info to R.string.hv_basis_phone_info,
        HistoryOverviewView.TimeBasis.PHONE_RECEIVED.emptyText to R.string.hv_basis_phone_empty,
        HistoryOverviewView.TimeBasis.DEVICE_MEASURED.legend to R.string.hv_basis_band_legend,
        HistoryOverviewView.TimeBasis.DEVICE_MEASURED.info to R.string.hv_basis_band_info,
        HistoryOverviewView.TimeBasis.DEVICE_MEASURED.emptyText to R.string.hv_basis_band_empty,
        "Tagesziel erreicht" to R.string.dom_goal_reached,
        "Zu wenige gültige Messwerte." to R.string.dom_night_few,
        "Nachtserie nicht lesbar." to R.string.dom_night_unreadable,
        "Nicht gemessen oder ungültig" to R.string.dom_note_not_measured,
        "Schlafphasen/-dauer erfordern validierte FIT-Historie" to R.string.dom_note_sleep_fit,
        "Schlafdauer und Schlafphasen noch nicht verfügbar" to R.string.dom_note_sleep_na,
        "Keine Diagnosen; keine geschätzten Werte; kein RR→RMSSD ohne Zeitreihe" to R.string.dom_note_ground,
        "Geräteskala 0–100" to R.string.dom_unit_device_scale,
    )

    private val sourceRx = Regex("^Quelle: CIRQA-Service (\\S+)$")

    fun tr(context: Context, german: String): String = translate(Strings.of(context), german)

    /** Context free core of [tr]; with the German resources it returns every known phrase unchanged (unit-tested). */
    fun translate(t: Strings, german: String): String =
        exact[german]?.let { t.get(it) }
            ?: DomainTextAnalysis.exact[german]?.let { t.get(it) }
            ?: sourceRx.matchEntire(german)?.let { t.get(R.string.dom_note_source, it.groupValues[1]) }
            ?: DomainTextAnalysis.patterns.firstNotNullOfOrNull { (rx, id) ->
                rx.matchEntire(german)?.let { m -> t.get(id, *m.groupValues.drop(1).map { translate(t, it) }.toTypedArray()) }
            }
            ?: german

    internal fun knownPhrases(): Set<String> = exact.keys + DomainTextAnalysis.exact.keys

    fun mode(context: Context, mode: PulseContext) = tr(context, mode.label)

    /** Caption under the heart-rate gauge (language dependent; the model's own [StepActivityEstimate.caption] stays German). */
    fun activityCaption(context: Context, a: StepActivityEstimate): String =
        a.cadenceStepsPerMinute?.let { context.getString(R.string.dom_caption_cadence, it.toInt(), a.spanSeconds / 60) }
            ?: context.getString(R.string.dom_caption_unknown, tr(context, a.explanation))

    private val windowRx = Regex("^Zeitfenster kürzer als (\\d+) h\\.$")
    private val coverageRx = Regex("^Abdeckung (\\S+) % unter (\\d+) %\\.$")
    private val gapRx = Regex("^Abtastabstand zu groß für ODI \\(Median (\\d+) s, nötig ≤ (\\d+) s\\)\\.$")

    /** Reason text of a night summary; numbers are carried over from the German phrase. */
    fun nightReason(context: Context, reason: String): String {
        windowRx.matchEntire(reason)?.let { return context.getString(R.string.dom_night_window, it.groupValues[1].toInt()) }
        coverageRx.matchEntire(reason)?.let { return context.getString(R.string.dom_night_coverage, it.groupValues[1], it.groupValues[2].toInt()) }
        gapRx.matchEntire(reason)?.let { return context.getString(R.string.dom_night_gap, it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        return tr(context, reason)
    }

    /** Spoken description of the resting-pulse hero (language dependent; the model's own [RestingPulseHero.Model.description] stays German). */
    fun heroDescription(context: Context, m: RestingPulseHero.Model): String = buildString {
        append(tr(context, "Ruhepuls, Entwicklung über ${m.windowDays} Tage. "))
        m.valueText?.let { append(tr(context, "Median $it. ")) }
        m.deltaText?.let { append(tr(context, it)).append(". ") }
        append(tr(context, m.message))
    }

    fun weekRowText(context: Context, row: WeekSummary.Row): String =
        context.getString(R.string.tr_week_value, tr(context, row.label), row.value?.let { tr(context, it) } ?: context.getString(R.string.tr_no_data))

    fun weekRowCoverage(context: Context, row: WeekSummary.Row): String = tr(context, row.coverage())

    fun weekRowDescription(context: Context, row: WeekSummary.Row): String =
        weekRowText(context, row) + (if (row.value != null) ", ${weekRowCoverage(context, row)}" else "") + (row.detail?.let { ". ${tr(context, it)}" } ?: "")

    fun coverageDescription(context: Context, line: DataCoverage.Line): String =
        tr(context, line.text()) + (line.hint()?.let { ", ${tr(context, it)}" } ?: "")
}

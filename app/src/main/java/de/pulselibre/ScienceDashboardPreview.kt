package de.pulselibre

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Development preview of the science dashboard (#45/#329) built ONLY from [ScienceFixtures]. It is reachable from the hidden
 * test lab, always carries a synthetic-data banner, and is not connected to any real measurement.
 */
object ScienceDashboardPreview {
    private const val HOUR = 3_600_000L
    private val RANGES = listOf("24 Stunden" to 24, "7 Tage" to 168, "30 Tage" to 720)

    fun show(activity: AppCompatActivity) {
        val palette = AppPalette.of(activity)
        fun dp(v: Int) = (v * activity.resources.displayMetrics.density + 0.5f).toInt()
        fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(activity).apply {
            text = t; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD)
        }
        val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16)); tag = "science_preview" }
        val warn = Color.parseColor(if (palette.dark) "#FFB74D" else "#B26A00")
        content.addView(text("Dashboard-Vorschau", 20f, palette.ink, true))
        content.addView(text("${ScienceFixtures.LABEL}E TESTDATEN – keine echten Gesundheitsdaten. Nur Entwicklungsvorschau, nicht mit dem Gerät verbunden.", 13f, warn, true).apply {
            tag = "science_banner"
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setStroke(dp(2), warn); cornerRadius = dp(10).toFloat(); setColor(palette.card) }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(12) })

        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        var selected = 0
        val rangeRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        lateinit var render: () -> Unit
        RANGES.forEachIndexed { index, (label, _) ->
            rangeRow.addView(text(label, 12f, palette.accent, true).apply {
                tag = "science_range_$index"
                gravity = Gravity.CENTER; minHeight = dp(48); isClickable = true; isFocusable = true
                background = GradientDrawable().apply { cornerRadius = dp(12).toFloat() }
                setOnClickListener { selected = index; render() }
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        }
        content.addView(rangeRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        content.addView(body)

        fun card(title: String, vararg views: View): LinearLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(18).toFloat() }
            addView(text(title, 15f, palette.ink, true))
            views.forEach { addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }) }
        }
        fun badge(origin: String, basis: String) = text("Quelle: $origin · Zeitbasis: $basis", 11f, warn, true)
        fun explain(data: SeriesData, a: SeriesAssessment) = text(ScienceText.describe(data, a), 12f, palette.muted)

        render = {
            body.removeAllViews()
            val hours = RANGES[selected].second
            val start = 1_700_000_000_000L - 1_700_000_000_000L % HOUR
            RANGES.indices.forEach { i ->
                (content.findViewWithTag<TextView>("science_range_$i"))?.apply {
                    isSelected = i == selected
                    setTextColor(if (i == selected) palette.card else palette.accent)
                    (background as GradientDrawable).setColor(if (i == selected) palette.primary else palette.tinted)
                    contentDescription = RANGES[i].first + if (i == selected) ", ausgewählt" else ""
                }
            }
            listOf("Herzfrequenz" to ScienceFixtures.heartRate(start, hours), "Atemfrequenz" to ScienceFixtures.respiration(start, hours),
                "SpO₂ (ungeprüft)" to ScienceFixtures.spo2(start, hours), "Stress (Geräteskala)" to ScienceFixtures.stress(start, hours),
                "Aktivität (Schritte je 15 min)" to ScienceFixtures.activity(start, hours)).forEach { (title, data) ->
                val chart = ScienceSeriesView(activity, palette).apply { show(data) }
                body.addView(card(title, badge("synthetisch", "synthetisch"), chart, explain(data, SeriesAnalysis.assess(data))),
                    LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
            }
            val sleep = ScienceFixtures.sleep(start)
            val sleepView = SleepTimelineView(activity, palette).apply { show(sleep) }
            body.addView(card("Schlaf (eine synthetische Nacht)", badge("synthetisch", "synthetisch"), sleepView,
                text(ScienceText.describeSleep(SleepAnalysis.assess(sleep)), 12f, palette.muted)), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
            // Zustände, die jede echte Anzeige kennen muss
            fun stateCard(title: String, message: String) = card(title, text(message, 13f, palette.muted)).apply { tag = "science_state_$title" }
            body.addView(text("Zustände", 15f, palette.ink, true), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            body.addView(stateCard("Lädt", "Daten werden gelesen …"), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            body.addView(stateCard("Fehler", "Die Daten konnten nicht gelesen werden. Es wird nichts ersatzweise angezeigt."), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            body.addView(stateCard("Leer", ScienceText.describe(ScienceFixtures.heartRate(start, 0).copy(points = emptyList()), SeriesAnalysis.assess(
                ScienceFixtures.heartRate(start, 0).copy(points = emptyList())))), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            body.addView(stateCard("Unvollständige Datenbasis", "Für diese Auswertung liegen nicht für jeden Abschnitt verifizierte Daten vor. Lücken bleiben sichtbar und werden nicht aufgefüllt."),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        render()

        val close = text("Schließen", 15f, palette.accent, true).apply { gravity = Gravity.CENTER; minHeight = dp(48); isClickable = true; isFocusable = true; tag = "science_close" }
        val scroll = ScrollView(activity).apply { addView(content) }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(palette.background)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); addView(close, LinearLayout.LayoutParams(-1, -2))
        }
        val dialog = DialogProbe.track(AlertDialog.Builder(activity).setView(root).create())
        close.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

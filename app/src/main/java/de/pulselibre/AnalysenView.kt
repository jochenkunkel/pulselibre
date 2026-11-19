package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Main tab "Analysen" (#227): five module groups as section header + cards. The only computed content is the existing
 * [TrendCardView] (single instance, no duplicated calculations); every other module is an honest status card.
 */
internal class AnalysenView(
    context: Context,
    private val goHistory: () -> Unit,
    /** Called with the y offset (inside this view) of the trend block so the host can scroll there. */
    private val reveal: (Int) -> Unit
) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private val trendBlock = TrendCardView(context)

    init {
        orientation = VERTICAL; tag = "analysen_view"
        addView(TextView(context).apply { text = "Analysen"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(palette.ink) })
        addView(TextView(context).apply {
            text = "Wissenschaftliche, explorative Auswertungen deiner erfassten Daten – keine Diagnose. Module ohne belegte Daten sind als „Noch nicht verfügbar“ mit Grund gekennzeichnet."
            textSize = 13f; setTextColor(palette.muted); setPadding(0, dp(4), 0, dp(8))
        })
        AnalysisCatalog.byCategory().forEach { (category, modules) ->
            addView(TextView(context).apply {
                text = "%02d · %s".format(category.number, category.title); textSize = 16f; setTypeface(null, Typeface.BOLD)
                setTextColor(palette.ink); setPadding(0, dp(16), 0, dp(2)); tag = "analysis_group_${category.number}"
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES; isFocusable = true
                contentDescription = "Gruppe ${category.number}: ${category.title}. ${category.subtitle}"
            })
            addView(TextView(context).apply { text = category.subtitle; textSize = 12f; setTextColor(palette.muted); setPadding(0, 0, 0, dp(6)) })
            modules.forEach { addView(card(it), LayoutParams(-1, -2).apply { topMargin = dp(6) }) }
            if (category == AnalysisCatalog.Category.HEART) {
                addView(trendBlock, LayoutParams(-1, -2).apply { topMargin = dp(10) })
            }
        }
    }

    private fun chipColor(a: AnalysisCatalog.Availability) = when (a) {
        AnalysisCatalog.Availability.AVAILABLE -> palette.accent
        AnalysisCatalog.Availability.PARTIAL -> palette.indicator
        AnalysisCatalog.Availability.UNAVAILABLE -> palette.muted
    }

    private fun card(m: AnalysisCatalog.Module): View = LinearLayout(context).apply {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(56); tag = "analysis_module_${m.id.name}"
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply { setColor(palette.card); cornerRadius = dp(16).toFloat(); setStroke(dp(1), palette.track) }
        val clickable = m.route != AnalysisCatalog.Route.NONE
        isClickable = clickable; isFocusable = true
        contentDescription = "${m.title}. ${m.availability.label}. ${m.note}" + if (clickable) " Tippen zum Öffnen." else ""
        addView(PlIcon.view(context, m.icon, 24, if (clickable) palette.accent else palette.muted), LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(10) })
        val texts = LinearLayout(context).apply { orientation = VERTICAL }
        texts.addView(TextView(context).apply { text = m.title; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(if (clickable) palette.ink else palette.muted)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
        texts.addView(TextView(context).apply { text = m.note; textSize = 11f; setTextColor(palette.muted)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO })
        addView(texts, LayoutParams(0, -2, 1f))
        addView(TextView(context).apply {
            text = m.availability.label; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(chipColor(m.availability))
            gravity = Gravity.END; setPadding(dp(8), 0, 0, 0); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(dp(92), -2))
        if (clickable) setOnClickListener {
            when (m.route) {
                AnalysisCatalog.Route.INLINE -> reveal(trendBlock.top)
                AnalysisCatalog.Route.HISTORY -> goHistory()
                AnalysisCatalog.Route.METHODS -> DialogProbe.track(AlertDialog.Builder(context)
                    .setTitle("Methoden und Grenzen").setMessage(AnalysisCatalog.METHODS_TEXT)
                    .setPositiveButton("Schließen", null).create()).show()
                AnalysisCatalog.Route.NONE -> Unit
            }
        }
    }
}

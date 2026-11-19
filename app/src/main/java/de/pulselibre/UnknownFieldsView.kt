package de.pulselibre

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Subtle hint on the device page: "Unbekannte Datenfelder entdeckt (N Typen)". Hidden when there is nothing to show.
 * Tapping opens the read-only list (coordinates, counters, classification, status); no raw values, no medical labels.
 */
internal class UnknownFieldsView(context: Context) : LinearLayout(context) {
    private val palette = AppPalette.of(context)
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private var summary: UnknownFieldsSummary.Summary? = null

    init { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE; tag = "unknown_fields_hint" }

    fun show(value: UnknownFieldsSummary.Summary) {
        summary = value
        removeAllViews()
        if (value.isEmpty) { visibility = View.GONE; return }
        visibility = View.VISIBLE
        background = GradientDrawable().apply { setColor(palette.tinted); cornerRadius = dp(14).toFloat() }
        setPadding(dp(14), dp(12), dp(14), dp(12))
        isClickable = true; isFocusable = true
        contentDescription = "${value.headline}. ${value.explanation} Tippen für Details."
        addView(PlIcon.view(context, PlIcon.DATA, 22, palette.accent), LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(12) })
        val texts = LinearLayout(context).apply { orientation = VERTICAL }
        texts.addView(TextView(context).apply {
            text = value.headline; textSize = 14f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        texts.addView(TextView(context).apply {
            text = value.explanation; textSize = 12f; setTextColor(palette.muted)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        addView(texts, LayoutParams(0, -2, 1f))
        setOnClickListener { showDetails(value) }
    }

    private fun showDetails(value: UnknownFieldsSummary.Summary) {
        val content = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        content.addView(TextView(context).apply {
            text = "Unbekannt heißt: nicht entschlüsselt. Diese Einträge sind weder Messwerte noch Diagnosen; sie zeigen nur, " +
                "welche Felder die Dateien des Bands enthalten, die PulsLibre noch nicht versteht."
            textSize = 13f; setTextColor(palette.muted); setPadding(0, 0, 0, dp(10))
        })
        value.entries.forEach { e ->
            val item = LinearLayout(context).apply {
                orientation = VERTICAL; setPadding(0, dp(8), 0, dp(8))
                tag = "unknown_field_entry"
            }
            item.addView(TextView(context).apply { text = e.title; textSize = 14f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD) })
            item.addView(TextView(context).apply { text = e.detail; textSize = 12f; setTextColor(palette.muted) })
            item.addView(TextView(context).apply { text = e.occurrence; textSize = 12f; setTextColor(palette.muted) })
            item.addView(TextView(context).apply { text = "${e.classification} · Status: ${e.status}"; textSize = 12f; setTextColor(palette.muted) })
            content.addView(item)
        }
        if (value.omitted > 0) content.addView(TextView(context).apply {
            text = "… und ${value.omitted} weitere Typen"; textSize = 12f; setTextColor(palette.muted)
        })
        DialogProbe.track(AlertDialog.Builder(context)
            .setTitle(value.headline)
            .setView(ScrollView(context).apply { addView(content) })
            .setPositiveButton("Schließen", null)
            .create()).show()
    }
}

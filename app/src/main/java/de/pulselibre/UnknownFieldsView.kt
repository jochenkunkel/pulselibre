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
        // Grouped by FIT message number; a group's field list is inflated lazily on first expand (keeps 900+ types cheap).
        fun entryView(e: UnknownFieldsSummary.Entry) = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(12), dp(6), 0, dp(6)); tag = "unknown_field_entry"
            addView(TextView(context).apply { text = e.title; textSize = 13f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD) })
            addView(TextView(context).apply { text = e.detail; textSize = 12f; setTextColor(palette.muted) })
            addView(TextView(context).apply { text = e.occurrence; textSize = 12f; setTextColor(palette.muted) })
            addView(TextView(context).apply { text = "${e.classification} · Status: ${e.status}"; textSize = 12f; setTextColor(palette.muted) })
        }
        value.groups.forEach { g ->
            val body = LinearLayout(context).apply { orientation = VERTICAL; visibility = View.GONE; tag = "unknown_group_body_${g.messageNumber}" }
            var inflated = false
            val header = TextView(context).apply {
                text = "▸  ${g.title}"; textSize = 14f; setTextColor(palette.ink); setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(12)); minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; tag = "unknown_group_${g.messageNumber}"
                contentDescription = "${g.title}. Tippen zum Ein- oder Ausklappen."
                setOnClickListener {
                    if (!inflated) { g.entries.forEach { body.addView(entryView(it)) }; inflated = true }
                    val open = body.visibility != View.VISIBLE
                    body.visibility = if (open) View.VISIBLE else View.GONE
                    text = (if (open) "▾  " else "▸  ") + g.title
                }
            }
            content.addView(header); content.addView(body)
        }
        DialogProbe.track(AlertDialog.Builder(context)
            .setTitle(value.headline)
            .setView(ScrollView(context).apply { addView(content) })
            .setPositiveButton("Schließen", null)
            .create()).show()
    }
}

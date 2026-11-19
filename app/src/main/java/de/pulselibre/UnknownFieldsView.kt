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

    /** Called after a review status was stored, so the host can refresh the hint (#204). */
    var onStatusChanged: (() -> Unit)? = null

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
            val statusLine = TextView(context).apply {
                text = "${e.classification} · Status: ${e.status}"; textSize = 12f; setTextColor(palette.muted)
            }
            addView(statusLine)
            if (e.records.isNotEmpty()) {
                // #204: der Status ist eine eigene Prüfmarke; er ändert keine Messdaten.
                addView(TextView(context).apply {
                    text = "Status ändern"; textSize = 12f; setTextColor(palette.accent); setTypeface(null, Typeface.BOLD)
                    minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL; isClickable = true; isFocusable = true
                    tag = "unknown_field_status_change"; contentDescription = "Status ändern für ${e.title}"
                    setOnClickListener { chooseStatus(e, statusLine) }
                })
            }
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

    private val statusChoices = listOf(
        FitFieldInventoryStatus.UNREVIEWED to "ungeprüft",
        FitFieldInventoryStatus.INVESTIGATING to "in Prüfung",
        FitFieldInventoryStatus.SUPPORTED to "unterstützt",
        FitFieldInventoryStatus.IGNORED to "ignoriert"
    )

    private fun chooseStatus(e: UnknownFieldsSummary.Entry, statusLine: TextView) {
        val current = e.records.map { it.status }.distinct().singleOrNull()
        lateinit var dialog: AlertDialog
        // Eigene Ansicht statt setMessage + setSingleChoiceItems: AppCompat gibt der Nachricht den ganzen Platz und versteckt die Liste.
        val content = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(8)) }
        content.addView(TextView(context).apply {
            text = "Nur deine Prüfmarke für dieses Feld. Es werden keine Messdaten verändert."
            textSize = 13f; setTextColor(palette.muted); setPadding(0, 0, 0, dp(8))
        })
        statusChoices.forEach { (status, label) ->
            content.addView(TextView(context).apply {
                text = (if (status == current) "●  " else "○  ") + label
                textSize = 15f; setTextColor(palette.ink); minimumHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
                isClickable = true; isFocusable = true; tag = "unknown_status_$label"
                contentDescription = label + if (status == current) ", aktuell gewählt" else ""
                setOnClickListener {
                    dialog.dismiss()
                    Thread {
                        val ok = try {
                            CirqaDatabase(context.applicationContext).use { db -> e.records.forEach { db.updateFitUnmappedFieldStatus(it, status) } }
                            true
                        } catch (_: Exception) { false }
                        statusLine.post {
                            statusLine.text = if (ok) "${e.classification} · Status: ${UnknownFieldsSummary.statusLabel(listOf(status))}"
                                else "${e.classification} · Status konnte nicht gespeichert werden"
                            if (ok) onStatusChanged?.invoke()
                        }
                    }.start()
                }
            })
        }
        dialog = DialogProbe.track(AlertDialog.Builder(context)
            .setTitle("Status: ${e.title}")
            .setView(content)
            .setNegativeButton("Abbrechen", null)
            .create())
        dialog.show()
    }
}

package de.pulselibre

import android.content.Context
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** "Startseite anpassen" (#267): visibility, order and favourites; every control has a text label (no icon-only state). */
internal fun showDashboardCustomize(
    context: Context,
    initial: List<DashboardLayout.Entry>,
    onSave: (List<DashboardLayout.Entry>) -> Unit,
    onReset: () -> Unit,
) {
    val palette = AppPalette.of(context)
    val density = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * density + 0.5f).toInt()
    var entries = initial
    val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
    list.addView(TextView(context).apply {
        text = "Ausgeblendete Kacheln werden weiter aufgezeichnet, nur nicht angezeigt. Favoriten erscheinen zuerst unter „Im Fokus“."
        textSize = 12f; setTextColor(palette.muted)
    })
    fun small(label: String, description: String, enabled: Boolean = true, action: () -> Unit) = Button(context).apply {
        text = label; contentDescription = description; isAllCaps = false; minWidth = 0; minimumWidth = 0; isEnabled = enabled
        textSize = 12f; setTextColor(palette.accent)
        setOnClickListener { action() }
    }
    lateinit var render: () -> Unit
    render = {
        while (list.childCount > 1) list.removeViewAt(1)
        entries.forEachIndexed { index, e ->
            val name = DashboardLayout.displayName(e.key)
            val row = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(4)); tag = "customize_row_${e.key}" }
            row.addView(CheckBox(context).apply {
                text = name; isChecked = e.visible; textSize = 15f; setTextColor(palette.ink); tag = "customize_visible_${e.key}"
                contentDescription = "$name anzeigen"
                setOnCheckedChangeListener { _, _ -> entries = DashboardLayout.toggleVisible(entries, e.key); render() }
            })
            val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            controls.addView(small(if (e.favorite) "★ Favorit" else "☆ Favorit", "$name ${if (e.favorite) "aus Im Fokus entfernen" else "zu Im Fokus hinzufügen"}") {
                entries = DashboardLayout.toggleFavorite(entries, e.key); render()
            })
            controls.addView(small("▲ Hoch", "$name nach oben", index > 0) { entries = DashboardLayout.moveUp(entries, e.key); render() })
            controls.addView(small("▼ Runter", "$name nach unten", index < entries.lastIndex) { entries = DashboardLayout.moveDown(entries, e.key); render() })
            row.addView(controls)
            list.addView(row)
        }
    }
    render()
    AlertDialog.Builder(context).setTitle("Startseite anpassen")
        .setView(ScrollView(context).apply { addView(list) })
        .setPositiveButton("Speichern") { _, _ -> onSave(entries) }
        .setNeutralButton("Zurücksetzen") { _, _ -> onReset() }
        .setNegativeButton("Abbrechen", null).show()
}

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
        text = context.getString(R.string.cust_note, context.getString(R.string.ov_in_focus))
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
            val name = when (e.key) {
                DashboardLayout.SPO2 -> MetricText.shortTitle(context, e.key)
                DashboardLayout.STEPS -> MetricText.title(context, "Schritte heute")
                DashboardLayout.NIGHT -> context.getString(R.string.ov_night_title)
                else -> MetricText.title(context, e.key)
            }
            val row = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(4)); tag = "customize_row_${e.key}" }
            row.addView(CheckBox(context).apply {
                text = name; isChecked = e.visible; textSize = 15f; setTextColor(palette.ink); tag = "customize_visible_${e.key}"
                contentDescription = context.getString(R.string.cust_show_cd, name)
                setOnCheckedChangeListener { _, _ -> entries = DashboardLayout.toggleVisible(entries, e.key); render() }
            })
            val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            controls.addView(small(context.getString(if (e.favorite) R.string.cust_fav_on else R.string.cust_fav_off),
                context.getString(if (e.favorite) R.string.cust_fav_remove_cd else R.string.cust_fav_add_cd, name, context.getString(R.string.ov_in_focus))) {
                entries = DashboardLayout.toggleFavorite(entries, e.key); render()
            })
            controls.addView(small(context.getString(R.string.cust_up), context.getString(R.string.cust_up_cd, name), index > 0) { entries = DashboardLayout.moveUp(entries, e.key); render() })
            controls.addView(small(context.getString(R.string.cust_down), context.getString(R.string.cust_down_cd, name), index < entries.lastIndex) { entries = DashboardLayout.moveDown(entries, e.key); render() })
            row.addView(controls)
            list.addView(row)
        }
    }
    render()
    AlertDialog.Builder(context).setTitle(R.string.cust_title)
        .setView(ScrollView(context).apply { addView(list) })
        .setPositiveButton(R.string.action_save) { _, _ -> onSave(entries) }
        .setNeutralButton(R.string.cust_reset) { _, _ -> onReset() }
        .setNegativeButton(R.string.action_cancel, null).show()
}

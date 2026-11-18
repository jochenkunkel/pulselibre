package de.pulselibre

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Shared tab strip. Reorders existing screens in the task so their forms and scroll state survive. */
internal class MainNavigationView(
    private val activity: AppCompatActivity,
    initialTab: Int = 0,
    private val showMainTab: (Int) -> Unit = {}
) : LinearLayout(activity) {
    private val palette = AppPalette.of(activity)
    private val labels = listOf("Übersicht", "Verlauf", "Gerät", "Testlabor", "Daten")
    private val icons = listOf(
        DashboardInfoIconView.Kind.HOME, DashboardInfoIconView.Kind.HISTORY,
        DashboardInfoIconView.Kind.DEVICES, DashboardInfoIconView.Kind.LAB,
        DashboardInfoIconView.Kind.DATA
    )
    private var selectedTab = initialTab
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    init {
        orientation = HORIZONTAL
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setBackgroundColor(palette.card)
        labels.forEachIndexed { index, label ->
            addView(LinearLayout(activity).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                minimumHeight = dp(52)
                setPadding(dp(2), dp(3), dp(2), dp(3))
                isFocusable = true
                contentDescription = label
                tag = "navigation_item_$label"
                setOnClickListener { navigate(index) }
                addView(DashboardInfoIconView(activity, icons[index], palette.accent).apply {
                    tag = "navigation_icon_$label"
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LayoutParams(dp(18), dp(18)))
                addView(TextView(activity).apply {
                    text = label
                    textSize = 10f
                    maxLines = 1
                    // Shrinks long labels at large fonts / narrow widths instead of breaking words ("Übersich/t").
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this, 7, 11, 1, android.util.TypedValue.COMPLEX_UNIT_DIP) // dp: stays inside its slot even at 200 % font
                    gravity = Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LayoutParams(-1, -2).apply { topMargin = dp(2) })
            }, LayoutParams(0, -2, 1f))
        }
        select(initialTab)
    }

    fun select(index: Int) {
        selectedTab = index
        for (i in labels.indices) {
            val item = getChildAt(i) as LinearLayout
            item.isSelected = i == index
            item.background = if (item.isSelected) GradientDrawable().apply {
                setColor(palette.tinted)
                cornerRadius = dp(12).toFloat()
            } else null
            (item.getChildAt(1) as TextView).setTextColor(if (item.isSelected) palette.accent else palette.muted)
        }
    }

    private fun navigate(index: Int) {
        if (index == selectedTab) return
        if (index < 3 && activity is MainActivity) {
            showMainTab(index)
            return
        }
        val destination = when (index) {
            3 -> CirqaTestLaboratoryActivity::class.java
            4 -> ExportActivity::class.java
            else -> MainActivity::class.java
        }
        activity.startActivity(Intent(activity, destination).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (index < 3) putExtra(EXTRA_TAB, index)
        })
    }

    companion object {
        const val EXTRA_TAB = "de.pulselibre.MAIN_TAB"
    }
}

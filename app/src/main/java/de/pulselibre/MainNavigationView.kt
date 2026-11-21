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
    /** Stable German ids: used for view tags and tests, never shown. */
    private val ids = listOf("Übersicht", "Verlauf", "Gerät", "Testlabor", "Daten", "Analysen")
    private val labels = listOf(R.string.nav_overview, R.string.nav_history, R.string.nav_device, R.string.nav_testlab, R.string.nav_data, R.string.nav_analyses)
        .map { activity.getString(it) }
    private val icons = listOf(PlIcon.OVERVIEW, PlIcon.HISTORY, PlIcon.DEVICE, PlIcon.TESTLAB, PlIcon.DATA, PlIcon.FITNESS)
    private var selectedTab = initialTab
    /** Tab indexes shown in the strip. The test laboratory (index 3) is no longer a visible tab (#172): it opens by a 3 s long press on the CIRQA heading. */
    private val visibleTabs = listOf(0, 1, 5, 2, 4)   // Übersicht, Verlauf, Analysen (#227), Gerät, Daten
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    init {
        orientation = HORIZONTAL
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setBackgroundColor(palette.card)
        // Children are added in strip order (visibleTabs), so child i belongs to tab visibleTabs[i].
        visibleTabs.forEach { index ->
            val label = labels[index]
            val id = ids[index]
            addView(LinearLayout(activity).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                minimumHeight = dp(52)
                setPadding(dp(2), dp(3), dp(2), dp(3))
                isFocusable = true
                contentDescription = label
                tag = "navigation_item_$id"
                setOnClickListener { navigate(index) }
                addView(PlIcon.view(activity, icons[index], 24, palette.muted).apply {
                    tag = "navigation_icon_$id"
                }, LayoutParams(dp(24), dp(24)))
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
        for (i in 0 until childCount) {
            val item = getChildAt(i) as LinearLayout
            item.isSelected = visibleTabs[i] == index
            item.background = if (item.isSelected) GradientDrawable().apply {
                setColor(palette.tinted)
                cornerRadius = dp(12).toFloat()
            } else null
            (item.getChildAt(1) as TextView).setTextColor(if (item.isSelected) palette.accent else palette.muted)
            (item.getChildAt(0) as android.widget.ImageView).setColorFilter(if (item.isSelected) palette.accent else palette.muted)
        }
    }

    private fun navigate(index: Int) {
        if (index == selectedTab) return
        if ((index < 3 || index == ANALYSEN) && activity is MainActivity) {
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
            if (index < 3 || index == ANALYSEN) putExtra(EXTRA_TAB, index)
        })
    }

    companion object {
        const val EXTRA_TAB = "de.pulselibre.MAIN_TAB"
        /** Tab index of the main tab "Analysen" (#227); 3 stays the hidden test laboratory, 4 the data tab. */
        const val ANALYSEN = 5
    }
}

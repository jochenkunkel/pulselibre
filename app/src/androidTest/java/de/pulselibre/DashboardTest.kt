package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardTest {
    @Test fun dashboardShowsApprovedOverviewSections() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(containsText(root, "Herzfrequenz"))
                assertTrue(containsText(root, "Sauerstoffsättigung · SpO₂"))
                assertTrue(containsText(root, "Schlaf"))
                assertTrue(containsText(root, "Heute auf einen Blick"))
                assertTrue(containsText(root, "Weitere Gesundheitswerte"))
            }
        }
    }
    @Test fun dashboardHasPulsLibreHeading() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(containsText(root, "PulsLibre"))
            }
        }
    }
    @Test fun humerusSettingsButtonShowsAccessibleConnectionStatusAndKeepsTouchTarget() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val button = findStatusIcon(root) ?: error("Verbindungsstatus-Icon fehlt")
                assertTrue(button.isClickable)
                assertTrue(button.isFocusable)
                assertTrue(button.contentDescription.startsWith("CIRQA-Verbindungsstatus:"))
                val expectedSize = (48 * activity.resources.displayMetrics.density + 0.5f).toInt()
                assertEquals(expectedSize, button.layoutParams.width)
                assertTrue(button.contentDescription.contains("schwarzer Rand") ||
                    button.contentDescription.contains("grüner Rand"))
            }
        }
    }
    private fun findStatusIcon(view: android.view.View): ImageView? {
        if (view is ImageView && view.contentDescription?.toString()?.startsWith("CIRQA-Verbindungsstatus:") == true) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findStatusIcon(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun historyTabIsPresentAndOpensLocalTimeSeries() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val historyTab = findText(root, "Verlauf") ?: error("Verlauf-Reiter fehlt")
                historyTab.performClick()
                assertTrue(containsText(root, "Verlauf · lokal gespeicherte Messpunkte"))
                assertTrue(containsDescription(root, "Zeitpunkt: Empfang auf dem Telefon."))
            }
        }
    }
    private fun containsDescription(view: View, expected: String): Boolean =
        view.contentDescription?.contains(expected) == true ||
            (view is ViewGroup && (0 until view.childCount).any { containsDescription(view.getChildAt(it), expected) })

    private fun findText(view: android.view.View, expected: String): TextView? {
        if (view is TextView && view.text.toString() == expected) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findText(view.getChildAt(i), expected)?.let { return it }
        return null
    }
    private fun containsText(view: android.view.View, expected: String): Boolean {
        if (view is TextView && view.text.toString() == expected) return true
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            if (containsText(view.getChildAt(i), expected)) return true
        }
        return false
    }
}

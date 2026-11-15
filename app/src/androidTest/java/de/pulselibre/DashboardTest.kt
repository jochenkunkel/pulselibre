package de.pulselibre

import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardTest {
    @Test fun dashboardContainsSixCardsAndNoFakeMeasurements() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val scroll = findScroll(activity.findViewById(android.R.id.content))
                assertNotNull(scroll)
                val root = scroll!!.getChildAt(0) as ViewGroup
                val cards = (0 until root.childCount).map { root.getChildAt(it) }
                    .filter { it.tag?.toString()?.startsWith("metric_") == true }
                assertEquals(6, cards.size)
                assertTrue(cards[1].contentDescription.contains("Keine Messdaten"))
                assertTrue(cards[5].contentDescription.contains("Noch nicht verfügbar"))
            }
        }
    }
    @Test fun dashboardHasPulseLibreHeading() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(containsText(root, "PulseLibre"))
            }
        }
    }
    private fun findScroll(view: android.view.View): ScrollView? {
        if (view is ScrollView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findScroll(view.getChildAt(i))?.let { return it }
        }
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

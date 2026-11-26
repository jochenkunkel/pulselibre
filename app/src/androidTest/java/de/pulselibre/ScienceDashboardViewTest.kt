package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The dashboard is visible in the Analysen tab, loads its cards and explains what is not available yet. */
@RunWith(AndroidJUnit4::class)
class ScienceDashboardViewTest {
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun dashboardShowsCardsAndExplainedEmptyStatesInTheAnalysenTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Analysen").performClick()
            }
            // The cards load off the main thread; on a cold CI emulator this took longer than 20 s once.
            // Wait for the tagged card itself (language-independent) instead of a localized text.
            val end = System.currentTimeMillis() + 45_000
            var texts = emptyList<String>()
            var ready = false
            while (System.currentTimeMillis() < end) {
                scenario.onActivity { activity ->
                    val views = all(activity.findViewById(android.R.id.content))
                    texts = views.filterIsInstance<TextView>().map { it.text.toString() }
                    ready = views.any { it.tag == "science_not_available" }
                }
                if (ready) break
                InstrumentationRegistry.getInstrumentation().waitForIdleSync(); Thread.sleep(250)
            }
            scenario.onActivity { activity ->
                assertTrue("Dashboard sichtbar", all(activity.findViewById(android.R.id.content)).any { it.tag == "science_dashboard" })
                assertTrue("Leerzustände erklärt", all(activity.findViewById(android.R.id.content)).any { it.tag == "science_not_available" })
            }
            assertTrue(texts.any { it.contains("Schlaf") || it.contains("Sleep") })
        }
    }
}

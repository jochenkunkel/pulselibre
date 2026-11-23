package de.pulselibre

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #227: main tab "Analysen" with five groups, honest module states, methods dialog and links. */
@RunWith(AndroidJUnit4::class)
class AnalysenTabTest {
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    private fun openTab(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Analysen").performClick() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // The tab hosts the science dashboard, which loads asynchronously; wait until the tab is really on screen
        // instead of asserting right after the click (avoids a timing flake on slow emulators).
        val end = System.currentTimeMillis() + 10_000
        var shown = false
        while (!shown && System.currentTimeMillis() < end) {
            scenario.onActivity { shown = it.findViewById<View>(android.R.id.content).findViewWithTag<View>("analysen_view")?.isShown == true }
            if (!shown) Thread.sleep(100)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test fun tabShowsFiveGroupsAndHonestModuleStates() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openTab(scenario)
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                val view = root.findViewWithTag<View>("analysen_view")
                assertNotNull(view); assertTrue(view.isShown)
                val texts = all(view).filterIsInstance<TextView>().map { it.text.toString() }
                (1..5).forEach { n -> assertTrue("Gruppe $n fehlt", texts.any { it.startsWith("%02d · ".format(n)) }) }
                // unavailable modules are labelled with a reason and are not clickable
                val sleep = root.findViewWithTag<View>("analysis_module_SLEEP_RECOVERY")
                assertFalse(sleep.isClickable)
                assertTrue(sleep.contentDescription.toString().contains("Noch nicht verfügbar"))
                assertTrue(sleep.contentDescription.toString().contains("nicht geschätzt"))
                // the single trend block is hosted here, not in Verlauf
                assertTrue(all(view).any { it.tag == "resting_hero" })
                assertEquals(1, all(root).count { it.tag == "resting_hero" })
            }
        }
    }

    @Test fun trendBlockIsNoLongerPartOfTheHistoryTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Verlauf").performClick() }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val shownHero = all(activity.findViewById(android.R.id.content)).filter { it.tag == "resting_hero" && it.isShown }
                assertTrue("Trend-Block darf in Verlauf nicht mehr sichtbar sein", shownHero.isEmpty())
            }
        }
    }

    @Test fun methodsCardOpensTheDialogAndHistoryLinkSwitchesTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openTab(scenario)
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("analysis_module_METHODS").performClick()
            }
            val dialog = DialogTestSupport.awaitDialog("Methoden und Grenzen")
            assertTrue(DialogTestSupport.containsText(dialog, "Keine Diagnose"))
            DialogTestSupport.clickText(dialog, "Schließen")
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("analysis_module_SPO2_STABILITY").performClick()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Verlauf").isSelected)
            }
        }
    }

    @Test fun deepLinkOpensTheAnalysenTab() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(MainNavigationView.EXTRA_TAB, MainNavigationView.ANALYSEN)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                assertTrue(root.findViewWithTag<View>("navigation_item_Analysen").isSelected)
                assertTrue(root.findViewWithTag<View>("analysen_view").isShown)
            }
        }
    }

    @Test fun overviewTeaserLinksToTheAnalysenTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                val teaser = root.findViewWithTag<View>("analysen_teaser")
                assertTrue("Teaser auf der Übersicht fehlt", teaser.isShown)
                assertTrue(teaser.performClick())
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                assertTrue(root.findViewWithTag<View>("navigation_item_Analysen").isSelected)
                assertFalse(root.findViewWithTag<View>("analysen_teaser").isShown)
            }
        }
    }
}

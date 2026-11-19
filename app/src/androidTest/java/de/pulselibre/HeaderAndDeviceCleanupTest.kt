package de.pulselibre

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #179 / #208 / #209 / #172: header subtitle, system bar icons, contradictory device texts and the hidden test laboratory. */
@RunWith(AndroidJUnit4::class)
class HeaderAndDeviceCleanupTest {
    private fun all(view: View): List<View> =
        listOf(view) + ((view as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    private fun shownTexts(root: View): List<String> =
        all(root).filterIsInstance<TextView>().filter { it.isShown }.map { it.text.toString() }

    @Test fun headerSubtitleHasNoOfflineSuffix() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = shownTexts(activity.findViewById(android.R.id.content))
                assertTrue("Untertitel fehlt", texts.contains("Deine Daten. Deine Kontrolle."))
                assertFalse("\"Offline\" darf nicht mehr im Header stehen",
                    texts.any { it == "Offline" || it == "· Offline" })
            }
        }
    }

    @Test fun systemBarIconsFollowTheTheme() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dark = AppPalette.of(activity).dark
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                assertEquals("Statusleisten-Icons müssen zum Theme passen", !dark, controller.isAppearanceLightStatusBars)
                assertEquals("Navigationsleisten-Icons müssen zum Theme passen", !dark, controller.isAppearanceLightNavigationBars)
            }
        }
    }

    @Test fun testLaboratoryIsNoLongerAVisibleTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                assertNull("Testlabor-Reiter darf nicht sichtbar sein", root.findViewWithTag<View>("navigation_item_Testlabor"))
                listOf("Übersicht", "Verlauf", "Analysen", "Gerät", "Daten").forEach {
                    assertNotNull("Reiter $it fehlt", root.findViewWithTag<View>("navigation_item_$it"))
                }
            }
        }
    }

    @Test fun deviceTabShowsNoContradictoryOrTechnicalStatusTexts() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                assertTrue(root.findViewWithTag<View>("navigation_item_Gerät").performClick())
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val texts = shownTexts(activity.findViewById(android.R.id.content))
                assertTrue("Gerät-Überschrift fehlt", texts.contains("Gerät · CIRQA"))
                listOf("Offline · bereit", "Keine Geräte verbunden.").forEach {
                    assertFalse("Veralteter Statustext sichtbar: $it", texts.contains(it))
                }
                val gatt = all(activity.findViewById(android.R.id.content)).filterIsInstance<TextView>()
                    .filter { it.isShown && it.text.contains("Android GATT") }
                assertTrue("Technischer GATT-Text sichtbar in: " + gatt.joinToString { "${it.javaClass.simpleName}(vis=${it.visibility}, text='${it.text}')" }, gatt.isEmpty())
                assertFalse("Passive-Diagnose-Zeile sichtbar", texts.any { it.contains("Passive Diagnose") })
                // User functions stay: recording state + start/stop.
                assertTrue(texts.any { it.startsWith("Aufzeichnung") })
                assertTrue(texts.contains("Starten") && texts.contains("Stoppen"))
            }
        }
    }

    @Test fun testLaboratoryOpensByHiddenGestureAndHasTalkBackAction() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                root.findViewWithTag<View>("navigation_item_Gerät").performClick()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val heading = all(activity.findViewById(android.R.id.content)).filterIsInstance<TextView>()
                    .first { it.text.toString() == "Gerät · CIRQA" }
                val labels = heading.createAccessibilityNodeInfo().actionList.map { it.label?.toString() }
                assertTrue("TalkBack-Aktion 'Testlabor öffnen' fehlt: $labels", labels.contains("Testlabor öffnen"))
            }
        }
    }

    @Test fun compactHeaderTitleStaysReadableOnThePageBackground() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val palette = AppPalette.of(activity)
                val root = activity.findViewById<View>(android.R.id.content)
                val title = all(root).filterIsInstance<TextView>().first { it.text.toString() == "PulsLibre" }
                // expanded header: text sits on the dark purple backdrop -> white
                assertEquals(android.graphics.Color.WHITE, title.currentTextColor)
                // Make the content scrollable enough and collapse: drive the same listener the user triggers.
                val scroll = all(root).filterIsInstance<android.widget.ScrollView>().first()
                scroll.getChildAt(0).minimumHeight = scroll.height * 3
                scroll.getChildAt(0).requestLayout()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                all(activity.findViewById<View>(android.R.id.content)).filterIsInstance<android.widget.ScrollView>().first().scrollTo(0, 400)
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val palette = AppPalette.of(activity)
                val title = all(activity.findViewById<View>(android.R.id.content)).filterIsInstance<TextView>().first { it.text.toString() == "PulsLibre" }
                if (!palette.dark) assertEquals("kompakter Header in Hell: Titel nicht weiß", palette.ink, title.currentTextColor)
            }
        }
    }
}

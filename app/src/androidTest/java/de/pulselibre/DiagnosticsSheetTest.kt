package de.pulselibre

import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #172 stage 1: hidden diagnostics sheet with five read-only areas. */
@RunWith(AndroidJUnit4::class)
class DiagnosticsSheetTest {
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun showsFiveAreasLoadsReadOnlyDataAndKeepsAdvancedEntry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { DiagnosticsSheet.show(it) }
            val dialog = DialogTestSupport.awaitDialog("Testlabor")
            listOf("A · App-Diagnose", "B · Bluetooth-Diagnose", "C · Kommunikationsaufzeichnung", "D · Synchronisation", "E · Diagnosepaket")
                .forEach { assertTrue("Bereich fehlt: $it", DialogTestSupport.containsText(dialog, it)) }
            assertTrue(DialogTestSupport.containsText(dialog, "Erweitertes Testlabor"))
            assertTrue(DialogTestSupport.containsText(dialog, "Nur lesende Diagnose"))
            // Loaded asynchronously: wait until the app section no longer says "Wird geladen".
            val end = System.currentTimeMillis() + 15000
            var app = ""
            while (System.currentTimeMillis() < end) {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    app = all(dialog.window!!.decorView).filterIsInstance<android.widget.TextView>()
                        .firstOrNull { it.tag == "diagnostics_body_app" }?.text?.toString().orEmpty()
                }
                if (app.isNotEmpty() && !app.startsWith("Wird geladen")) break
                Thread.sleep(200)
            }
            assertTrue("App-Abschnitt geladen: $app", app.contains("App-Version:") && app.contains("quick_check"))
            var trace = ""; var export = ""
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val tv = all(dialog.window!!.decorView).filterIsInstance<android.widget.TextView>()
                trace = tv.first { it.tag == "diagnostics_body_trace" }.text.toString()
                export = tv.first { it.tag == "diagnostics_body_export" }.text.toString()
            }
            assertTrue(trace.contains("noch nicht verfügbar"))
            assertTrue(export.contains("Vorschau"))
            assertFalse("keine MAC im Export", Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}").containsMatchIn(export))
        }
    }

    @Test fun deviceTabHeadingStillOffersTheHiddenEntry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Gerät").performClick()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val heading = all(activity.findViewById(android.R.id.content)).filterIsInstance<android.widget.TextView>()
                    .first { it.text.toString() == "Gerät · CIRQA" }
                assertTrue(heading.createAccessibilityNodeInfo().actionList.any { it.label?.toString() == "Testlabor öffnen" })
                assertEquals(null, activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_Testlabor"))
            }
        }
    }
}

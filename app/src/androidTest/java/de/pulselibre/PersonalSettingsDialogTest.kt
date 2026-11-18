package de.pulselibre

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Settings dialog: version line and the double-confirmed profile deletion. Runs on the emulator only. */
@RunWith(AndroidJUnit4::class)
class PersonalSettingsDialogTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun find(text: String, timeoutMs: Long = 15000): AccessibilityNodeInfo? {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val hit = root?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
            if (hit != null) return hit
            Thread.sleep(100)
        }
        return null
    }

    private fun click(node: AccessibilityNodeInfo) {
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        assertNotNull("kein klickbarer Knoten", n)
        n!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    @Test fun showsVersionAndRequiresTwoConfirmationsBeforeDeleting() {
        val prefs = instrumentation.targetContext.getSharedPreferences("local_personal_profile", 0)
        prefs.edit().putString("display_name", "Testperson").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { PersonalSettings.show(it) }
            instrumentation.waitForIdleSync()
            assertNotNull("Versionszeile fehlt", find("Build"))
            assertNotNull("Entwicklerangabe fehlt", find("Jochen Kunkel"))
            assertNotNull("Hinweis zum violetten Rand fehlt", find("Violetter Rand"))
            assertNotNull("Speichern fehlt", find("Speichern"))
            assertNotNull("Schließen fehlt", find("Schließen"))
            // Step 1 of 2
            click(find("Profil löschen …")!!)
            assertNotNull("erste Bestätigung fehlt", find("Profil löschen?"))
            click(find("Weiter")!!)
            // Step 2 of 2: cancelling must keep the profile
            assertNotNull("zweite Bestätigung fehlt", find("Wirklich endgültig löschen?"))
            click(find("Abbrechen")!!)
            Thread.sleep(300)
            assertEquals("Testperson", prefs.getString("display_name", null))
        }
        prefs.edit().remove("display_name").commit()
    }

    @Test fun cancellingTheFirstConfirmationKeepsTheProfile() {
        val prefs = instrumentation.targetContext.getSharedPreferences("local_personal_profile", 0)
        prefs.edit().putString("display_name", "Testperson").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { PersonalSettings.show(it) }
            instrumentation.waitForIdleSync()
            click(find("Profil löschen …")!!)
            assertNotNull(find("Profil löschen?"))
            click(find("Abbrechen")!!)
            Thread.sleep(300)
            assertEquals("Testperson", prefs.getString("display_name", null))
            assertNull("zweiter Dialog darf nicht erscheinen", find("Wirklich endgültig löschen?", 700))
        }
        prefs.edit().remove("display_name").commit()
    }
}

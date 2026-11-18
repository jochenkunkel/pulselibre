package de.pulselibre

import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Settings dialog: version line and the double-confirmed profile deletion. Uses [DialogProbe], not UiAutomation. */
@RunWith(AndroidJUnit4::class)
class PersonalSettingsDialogTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val prefs get() = instrumentation.targetContext.getSharedPreferences("local_personal_profile", 0)

    @Test fun showsVersionAndRequiresTwoConfirmationsBeforeDeleting() {
        prefs.edit().putString("display_name", "Testperson").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { PersonalSettings.show(it) }
            val settings = DialogTestSupport.awaitDialog("Build")
            assertTrue("Entwicklerangabe fehlt", DialogTestSupport.containsText(settings, "Jochen Kunkel"))
            assertTrue("Hinweis zum violetten Rand fehlt", DialogTestSupport.containsText(settings, "Violetter Rand"))
            assertTrue("Speichern fehlt", DialogTestSupport.containsText(settings, "Speichern"))
            assertTrue("Schließen fehlt", DialogTestSupport.containsText(settings, "Schließen"))
            // Step 1 of 2
            DialogTestSupport.clickText(settings, "Profil löschen …")
            val first = DialogTestSupport.awaitDialog("Profil löschen?")
            DialogTestSupport.clickButton(first, AlertDialog.BUTTON_POSITIVE)          // "Weiter"
            // Step 2 of 2: cancelling must keep the profile
            val second = DialogTestSupport.awaitDialog("Wirklich endgültig löschen?")
            DialogTestSupport.clickButton(second, AlertDialog.BUTTON_NEGATIVE)
            Thread.sleep(300)
            assertEquals("Testperson", prefs.getString("display_name", null))
        }
        prefs.edit().remove("display_name").commit()
    }

    @Test fun cancellingTheFirstConfirmationKeepsTheProfile() {
        prefs.edit().putString("display_name", "Testperson").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { PersonalSettings.show(it) }
            val settings = DialogTestSupport.awaitDialog("Profil löschen …")
            DialogTestSupport.clickText(settings, "Profil löschen …")
            val first = DialogTestSupport.awaitDialog("Profil löschen?")
            DialogTestSupport.clickButton(first, AlertDialog.BUTTON_NEGATIVE)
            Thread.sleep(500)
            assertEquals("Testperson", prefs.getString("display_name", null))
            // the second confirmation must never have appeared
            assertFalse(DialogProbe.current?.let { it.isShowing && DialogTestSupport.containsText(it, "Wirklich endgültig löschen?") } == true)
        }
        prefs.edit().remove("display_name").commit()
    }
}

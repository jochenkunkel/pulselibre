package de.pulselibre

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * i18n smoke test: the whole test suite runs in German ([GermanTestRunner]), so a defect that only exists in another language
 * (a bad format string, a missing argument) was never executed in CI. It once crashed the app at start in English
 * ("UnknownFormatConversionException", build 2125). This test starts the real activities in every non-German language, visits
 * every main tab, and fails on a crash, on the wrong language, or on an unresolved format placeholder shown to the user.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSmokeTest {
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val tabs = listOf("Übersicht", "Verlauf", "Analysen", "Gerät")

    /** Other tests expect German; GermanTestRunner sets it only once at process start. */
    @After fun backToGerman() { AppLanguageSettings.save(app, AppLanguage.GERMAN) }

    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    private fun settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1_200) // the tabs load their data on worker threads and post the result to the UI thread
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun assertNoUnresolvedPlaceholders(root: View, where: String) {
        val bad = Regex("%(\\d+\\$)?[sdf]|%%")
        all(root).filterIsInstance<TextView>().forEach {
            assertFalse("$where: unaufgelöster Platzhalter in \"${it.text}\"", bad.containsMatchIn(it.text))
        }
    }

    private fun runIn(language: AppLanguage) {
        AppLanguageSettings.save(app, language)
        val localized = AppLanguageSettings.wrap(app)
        val expectedAnalyses = localized.getString(R.string.nav_analyses)
        val germanAnalyses = AppLanguageSettings.wrap(app).let {
            AppLanguageSettings.save(app, AppLanguage.GERMAN); val de = AppLanguageSettings.wrap(app).getString(R.string.nav_analyses)
            AppLanguageSettings.save(app, language); de
        }
        assertTrue("Testaufbau: $language soll sich vom Deutschen unterscheiden", expectedAnalyses != germanAnalyses)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            tabs.forEach { id ->
                scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>("navigation_item_$id").performClick() }
                settle()
                scenario.onActivity {
                    val root = it.findViewById<View>(android.R.id.content)
                    if (id == "Analysen") assertEquals("Sprache $language", expectedAnalyses, root.findViewWithTag<View>("navigation_item_Analysen").contentDescription)
                    assertNoUnresolvedPlaceholders(root, "$language/$id")
                }
            }
        }
        ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { assertNoUnresolvedPlaceholders(it.findViewById(android.R.id.content), "$language/Daten") }
        }
    }

    @Test fun englishStartsAndShowsAllTabs() = runIn(AppLanguage.ENGLISH)
    @Test fun frenchStartsAndShowsAllTabs() = runIn(AppLanguage.FRENCH)
    @Test fun spanishStartsAndShowsAllTabs() = runIn(AppLanguage.SPANISH)
}

package de.pulselibre

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * The UI tests assert German texts. Since the app language follows the system language (#i18n), the emulator's
 * language must not decide whether a test passes: this rule selects German for the test and restores the user's choice.
 */
class GermanUiRule : ExternalResource() {
    private var before: AppLanguage = AppLanguage.SYSTEM

    override fun before() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        before = AppLanguageSettings.preference(context)
        instrumentation.runOnMainSync { AppLanguageSettings.save(context, AppLanguage.GERMAN) }
        awaitLanguage(context, "de")
    }

    /** On Android 13+ the system applies the app locale asynchronously; wait until the resources really speak German. */
    private fun awaitLanguage(context: android.content.Context, language: String) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (context.resources.configuration.locales[0].language == language) return
            Thread.sleep(100)
        }
        throw AssertionError("App-Sprache $language wurde nicht wirksam (aktuell ${context.resources.configuration.locales[0]})")
    }

    override fun after() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync { AppLanguageSettings.save(context, before) }
        instrumentation.waitForIdleSync()
    }
}

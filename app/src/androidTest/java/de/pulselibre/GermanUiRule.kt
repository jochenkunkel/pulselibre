package de.pulselibre

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * The UI tests assert German texts. Since the app language follows the system language (#i18n), the emulator's
 * language must not decide whether a test passes: this rule selects German (stored choice, read when an activity starts)
 * and restores the previous choice afterwards.
 */
class GermanUiRule : ExternalResource() {
    private var before: AppLanguage = AppLanguage.SYSTEM
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    override fun before() {
        before = AppLanguageSettings.preference(context)
        AppLanguageSettings.save(context, AppLanguage.GERMAN)
    }

    override fun after() {
        AppLanguageSettings.save(context, before)
    }
}

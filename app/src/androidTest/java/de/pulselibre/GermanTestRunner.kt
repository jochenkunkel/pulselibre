package de.pulselibre

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/**
 * Runner for the instrumentation tests: they assert German UI texts, so the app language is set to German before any
 * activity starts. The emulator's own language therefore never decides whether a test passes (#i18n). Tests that
 * check another language change the stored choice themselves.
 */
class GermanTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        AppLanguageSettings.save(targetContext, AppLanguage.GERMAN)
        super.onCreate(arguments)
    }
}

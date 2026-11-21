package de.pulselibre

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** i18n guard: every language has exactly the same string keys, placeholders match and nothing is empty. */
class StringResourcesParityTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val languages = mapOf("en" to "values", "de" to "values-de", "fr" to "values-fr", "es" to "values-es")
    private val entry = Regex("<string name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("%(\\d+\\$)?[sdf]")

    private fun load(dir: String): Map<String, String> {
        val file = File(res, "$dir/strings.xml")
        assertTrue("$dir/strings.xml fehlt", file.isFile)
        return entry.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test fun allLanguagesHaveTheSameKeys() {
        val base = load("values").keys
        languages.forEach { (lang, dir) -> assertEquals("Schlüssel in $lang", base, load(dir).keys) }
    }

    @Test fun placeholdersMatchAndTextsAreNotEmpty() {
        val base = load("values")
        languages.forEach { (lang, dir) ->
            load(dir).forEach { (key, text) ->
                assertTrue("$lang/$key leer", text.isNotBlank())
                assertEquals("Platzhalter $lang/$key",
                    placeholder.findAll(base.getValue(key)).map { it.value }.sorted().toList(),
                    placeholder.findAll(text).map { it.value }.sorted().toList())
            }
        }
    }

    @Test fun everySupportedLanguageIsDeclaredForTheSystemPicker() {
        val config = File(res, "xml/locales_config.xml").readText()
        AppLanguage.entries.mapNotNull { it.tag }.forEach { assertTrue("locales_config: $it", config.contains("android:name=\"$it\"")) }
    }
}

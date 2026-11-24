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

    /**
     * Strings read through [Strings] go through String.format, so a literal percent sign must be written "%%" there. German is
     * exercised by the whole test suite; the other languages must use "%%" and single "%" in exactly the same keys, otherwise
     * the app crashes at start in that language ("UnknownFormatConversionException", seen in English on a device).
     */
    @Test fun literalPercentSignsAreEscapedLikeInGerman() {
        fun shape(text: String): Pair<Int, Int> =
            text.split("%%").size - 1 to Regex("%(?!(\\d+\\$)?[sdf])").findAll(text.replace("%%", "")).count()
        val german = load("values-de")
        languages.forEach { (lang, dir) ->
            load(dir).forEach { (key, text) -> assertEquals("%-Zeichen $lang/$key wie in de", shape(german.getValue(key)), shape(text)) }
        }
    }

    /** Every text must be formattable with its own placeholders, in every language (what Context.getString(id, args) does). */
    @Test fun everyTextFormatsWithoutException() {
        languages.forEach { (lang, dir) ->
            load(dir).forEach { (key, text) ->
                val args = placeholder.findAll(text).map<MatchResult, Any> { if (it.value.endsWith("d")) 1 else if (it.value.endsWith("f")) 1.5 else "x" }.toList().toTypedArray()
                val clean = text.replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")
                try { String.format(java.util.Locale.ROOT, clean, *args) } catch (e: java.util.IllegalFormatException) {
                    // Strings read with plain getString(id) (no arguments) keep a single "%"; those are the same in German.
                    assertEquals("$lang/$key nicht formatierbar, aber in de ebenfalls ohne %%", 0, load("values-de").getValue(key).split("%%").size - 1)
                }
            }
        }
    }
}

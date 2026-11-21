package de.pulselibre

import java.io.File
import java.util.Locale

/** [Strings] over the real values-de/strings.xml, so unit tests exercise the shipped German texts without Android. */
internal object GermanStrings : Strings {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val entry = Regex("<string name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
    private val byName: Map<String, String> = entry.findAll(File(res, "values-de/strings.xml").readText()).associate {
        it.groupValues[1] to it.groupValues[2].replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
    }
    private val nameById: Map<Int, String> = R.string::class.java.fields.filter { it.type == Int::class.javaPrimitiveType }
        .associate { it.getInt(null) to it.name }

    override fun get(id: Int, vararg args: Any): String {
        val name = nameById[id] ?: error("Unbekannte String-ID $id")
        val template = byName[name] ?: error("Kein deutscher Text für $name")
        return if (args.isEmpty()) template.replace("%%", "%") else String.format(Locale.GERMANY, template, *args)
    }
}

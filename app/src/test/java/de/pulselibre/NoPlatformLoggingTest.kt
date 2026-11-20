package de.pulselibre

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Privacy guard (audit #6, A6-5): production code must not write to logcat or stdout.
 * Logcat can be read by other tools (adb, bug reports) and would bypass the redaction of the
 * diagnostics package. Use [CirqaLocalLog] (local, counters/status) instead.
 */
class NoPlatformLoggingTest {
    private val forbidden = Regex("""\b(android\.util\.)?Log\.[dviwe]\(|\bLog\.wtf\(|\bprintln\(|\bprint\(|printStackTrace\(""")

    @Test fun mainSourcesDoNotUseLogcatOrStdout() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).firstOrNull { it.isDirectory }
        assertTrue("main source directory not found from ${File(".").absolutePath}", root != null)
        val hits = root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.substringBefore("//")
                if (forbidden.containsMatchIn(code)) "${file.name}:${i + 1}: ${line.trim()}" else null
            }
        }.toList()
        assertTrue("Plattform-Logging im Produktionscode gefunden:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}

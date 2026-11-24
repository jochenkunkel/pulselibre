package de.pulselibre

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** A6-11/A6-18/A6-20: every activity that owns a running BLE session or export must survive a rotation (no recreation). */
class ManifestRotationTest {
    private val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml")).first { it.isFile }.readText()

    private fun activityTag(name: String): String =
        Regex("<activity\\s[^>]*android:name=\"\\.$name\"[^>]*>", RegexOption.DOT_MATCHES_ALL).find(manifest)?.value ?: error("no activity $name")

    @Test fun activitiesHandleRotationThemselves() {
        for (name in listOf("MainActivity", "ExportActivity", "CirqaTestLaboratoryActivity")) {
            val tag = activityTag(name)
            assertTrue("$name: configChanges missing", Regex("android:configChanges=\"[^\"]*orientation[^\"]*screenSize").containsMatchIn(tag))
        }
    }
}

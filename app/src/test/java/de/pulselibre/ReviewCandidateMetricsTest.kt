package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 25.11.2026: backups with sleep_level candidates were rejected by the restore check, which only knew three metrics.
 * Every metric the importer writes must be in the one shared set that reading, restore and backup validation use.
 */
class ReviewCandidateMetricsTest {
    private val src = listOf(File("src/main/java/de/pulselibre"), File("app/src/main/java/de/pulselibre")).first { it.isDirectory }

    @Test fun everyImportedCandidateMetricIsAcceptedByBackupAndRestore() {
        val committer = File(src, "FitObservationCommitter.kt").readText()
        val mapping = committer.substringAfter("val metric = when (candidate.profileFieldName) {").substringBefore("else ->")
        val written = Regex("""->\s*"([a-z0-9_]+)"""").findAll(mapping).map { it.groupValues[1] }.toSet()
        assertTrue("mapping not found", written.size >= 8)
        assertEquals(written, CirqaDatabase.FIT_REVIEW_CANDIDATE_METRICS)
    }

    @Test fun noValidatorKeepsItsOwnShorterList() {
        val offenders = src.listFiles().orEmpty().filter { it.extension == "kt" }
            .filter { Regex("""setOf\("spo2", "stress", "respiration"\)""").containsMatchIn(it.readText()) }.map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }
}

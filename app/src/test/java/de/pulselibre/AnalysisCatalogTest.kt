package de.pulselibre

import de.pulselibre.AnalysisCatalog.Availability
import de.pulselibre.AnalysisCatalog.Category
import de.pulselibre.AnalysisCatalog.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisCatalogTest {
    @Test fun fiveGroupsEachWithAtLeastOneModule() {
        assertEquals(5, Category.entries.size)
        AnalysisCatalog.byCategory().forEach { (c, m) -> assertTrue("Gruppe ${c.title} ist leer", m.isNotEmpty()) }
        assertEquals((1..5).toList(), Category.entries.map { it.number })
    }

    @Test fun everyModuleIdIsUniqueAndCovered() {
        assertEquals(AnalysisCatalog.ModuleId.entries.toSet(), AnalysisCatalog.modules.map { it.id }.toSet())
        assertEquals(AnalysisCatalog.modules.size, AnalysisCatalog.modules.map { it.id }.distinct().size)
    }

    @Test fun unavailableModulesStateAReasonAndNeverLeadAnywhere() {
        AnalysisCatalog.modules.filter { it.availability == Availability.UNAVAILABLE }.forEach {
            assertTrue("${it.title}: Grund fehlt", it.note.length >= 20)
            assertEquals("${it.title}: nicht verfügbar darf nicht verlinken", Route.NONE, it.route)
        }
    }

    @Test fun availableOrPartialModulesHaveARoute() {
        AnalysisCatalog.modules.filter { it.availability != Availability.UNAVAILABLE }
            .forEach { assertFalse("${it.title}: Route fehlt", it.route == Route.NONE) }
    }

    @Test fun noClaimsOfDiagnosisOrScoresAndSleepIsNotEstimated() {
        val all = (AnalysisCatalog.modules.map { it.title + " " + it.note } + AnalysisCatalog.METHODS_TEXT).joinToString(" ").lowercase()
        listOf("fitnessalter", "biologisches alter", "vo₂max", "ampel", "schlafapnoe").forEach { assertFalse("verboten: $it", all.contains(it)) }
        assertTrue(AnalysisCatalog.METHODS_TEXT.contains("Keine Diagnose"))
        assertTrue(AnalysisCatalog.modules.first { it.id == AnalysisCatalog.ModuleId.SLEEP_RECOVERY }.note.contains("nicht geschätzt"))
    }
}

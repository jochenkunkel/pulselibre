package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #329 (owner 26.11.2026): band sleep stages are shown as events only; unknown codes are never turned into a stage. */
class SleepStagesTest {
    @Test fun keepsProfileStagesInTimeOrderAndCountsThem() {
        val night = SleepStages.of(listOf(
            Triple(3_000L, 2.0, "profile_code"), Triple(1_000L, 1.0, "profile_code"), Triple(2_000L, 3.0, "profile_code"),
            Triple(4_000L, 4.0, "profile_code"), Triple(5_000L, 0.0, "profile_code")))
        assertEquals(listOf(1, 3, 2, 4, 0), night.events.map { it.code })
        assertEquals(mapOf(1 to 1, 3 to 1, 2 to 1, 4 to 1, 0 to 1), night.counts)
        assertEquals(1_000L, night.firstMs); assertEquals(5_000L, night.lastMs)
    }

    @Test fun unknownCodesFractionsAndOtherUnitsAreDropped() {
        val night = SleepStages.of(listOf(
            Triple(1L, 5.0, "profile_code"), Triple(2L, 2.5, "profile_code"), Triple(3L, 2.0, "percent"),
            Triple(4L, Double.NaN, "profile_code")))
        assertEquals(0, night.events.size); assertNull(night.firstMs)
    }
}

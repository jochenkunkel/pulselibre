package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FitHistoryMappingTest {
    private val provenance = HistoricalObservation.Provenance("a".repeat(64), 1, 20, 3, "PROFILE_VALID", "v1")

    private fun obs(metric: String = "heart_rate", value: Double = 70.0, unit: String = "bpm", at: Long = 1_000,
                    provenance: HistoricalObservation.Provenance? = this.provenance) =
        HistoricalObservation("dev", metric, Instant.ofEpochMilli(at), value, unit, "s$at", null, provenance)

    @Test fun keepsMeasurementTimeNotReceptionTime() {
        val withReceived = obs(at = 5_000).copy(receivedAt = Instant.ofEpochMilli(999_000))
        val point = FitHistoryMapping.toPoints(listOf(withReceived)).single()
        assertEquals(Instant.ofEpochMilli(5_000), point.receivedAt) // field name is historical; value is the measured time
        assertEquals(70, point.value)
    }

    @Test fun onlyFitProvenanceHeartRateInPlausibleRangeCounts() {
        val points = FitHistoryMapping.toPoints(listOf(
            obs(at = 1_000),
            obs(at = 2_000, provenance = null),          // live/legacy row without FIT origin
            obs(metric = "cadence", at = 3_000),         // other metric
            obs(unit = "rpm", at = 4_000),               // wrong unit
            obs(value = 0.0, at = 5_000),                // implausible
            obs(value = 241.0, at = 6_000),              // implausible
            obs(value = Double.NaN, at = 7_000),
            obs(value = 71.6, at = 8_000)                // rounds to 72
        ))
        assertEquals(listOf(1_000L, 8_000L), points.map { it.receivedAt.toEpochMilli() })
        assertEquals(listOf(70, 72), points.map { it.value })
    }

    @Test fun emptyInputGivesNoPoints() = assertTrue(FitHistoryMapping.toPoints(emptyList()).isEmpty())

    @Test fun timeBasesNameDifferentClocks() {
        val phone = HistoryOverviewView.TimeBasis.PHONE_RECEIVED
        val band = HistoryOverviewView.TimeBasis.DEVICE_MEASURED
        assertTrue(phone.legend.contains("Telefonzeit") && !phone.legend.contains("Messzeit"))
        assertTrue(band.legend.contains("Messzeit des Bandes") && !band.legend.contains("Telefonzeit"))
        assertTrue(band.info.contains("nicht der Empfang"))
    }
}

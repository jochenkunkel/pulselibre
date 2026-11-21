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

class FitDaySummaryTest {
    private val zone = java.time.ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(java.time.LocalDate.of(2026, 11, 20), zone)
    private val now = java.time.ZonedDateTime.of(2026, 11, 20, 15, 0, 0, 0, zone).toInstant()
    private fun at(h: Int, m: Int = 0) = java.time.ZonedDateTime.of(2026, 11, 20, h, m, 0, 0, zone).toInstant()

    @Test fun summarizesOnlyTodayUpToNowByMeasurementHour() {
        val s = TileInsightCalc.summarizeFit(listOf(
            HistoryPoint(at(8, 10), 60), HistoryPoint(at(8, 40), 70), HistoryPoint(at(9), 80),
            HistoryPoint(at(16), 99),                                   // after now
            HistoryPoint(at(23).minusSeconds(86_400), 55)               // yesterday
        ), day, now)!!
        assertEquals(3, s.count)
        assertEquals(60.0, s.min, 0.0); assertEquals(80.0, s.max, 0.0)
        assertEquals(65.0, s.hourly[8]!!, 0.0); assertEquals(80.0, s.hourly[9]!!, 0.0)
        assertTrue(s.hourly[16] == null)
    }

    @Test fun noPointsTodayGivesNull() {
        assertTrue(TileInsightCalc.summarizeFit(emptyList(), day, now) == null)
        assertTrue(TileInsightCalc.summarizeFit(listOf(HistoryPoint(at(8).minusSeconds(86_400), 60)), day, now) == null)
    }

    @Test fun pastDaySummaryCarriesItsDateForTheLabel() {
        val past = DashboardDay(java.time.LocalDate.of(2026, 11, 17), zone)
        val t = java.time.ZonedDateTime.of(2026, 11, 17, 10, 0, 0, 0, zone).toInstant()
        val s = TileInsightCalc.summarizeFit(listOf(HistoryPoint(t, 64), HistoryPoint(t.plusSeconds(3_600), 66)), past, now)!!
        assertEquals(java.time.LocalDate.of(2026, 11, 17), s.date)
        assertEquals(2, s.count)
        assertEquals(65.0, s.average, 0.0)
    }
}

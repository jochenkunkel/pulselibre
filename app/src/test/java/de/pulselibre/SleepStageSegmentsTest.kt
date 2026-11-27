package de.pulselibre

import de.pulselibre.SleepStages.AWAKE
import de.pulselibre.SleepStages.DEEP
import de.pulselibre.SleepStages.Event
import de.pulselibre.SleepStages.LIGHT
import de.pulselibre.SleepStages.REM
import de.pulselibre.SleepStages.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** #762: stage reports become bounded intervals; nothing is invented. */
class SleepStageSegmentsTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) = ZonedDateTime.of(y, m, d, h, min, 0, 0, berlin).toInstant().toEpochMilli()

    @Test fun issueExampleGivesFourContinuousIntervals() {
        val e = listOf(Event(at(2026, 11, 27, 1), LIGHT), Event(at(2026, 11, 27, 2), DEEP), Event(at(2026, 11, 27, 3), REM),
            Event(at(2026, 11, 27, 3, 15), AWAKE), Event(at(2026, 11, 27, 4), 0))
        assertEquals(listOf(
            Segment(at(2026, 11, 27, 1), at(2026, 11, 27, 2), LIGHT), Segment(at(2026, 11, 27, 2), at(2026, 11, 27, 3), DEEP),
            Segment(at(2026, 11, 27, 3), at(2026, 11, 27, 3, 15), REM), Segment(at(2026, 11, 27, 3, 15), at(2026, 11, 27, 4), AWAKE)),
            SleepStages.segments(e))
    }

    @Test fun lastReportIsNotExtendedAndSingleOrEmptyNightsGiveNothing() {
        assertTrue(SleepStages.segments(emptyList()).isEmpty())
        assertTrue(SleepStages.segments(listOf(Event(1_000, LIGHT))).isEmpty())
        val s = SleepStages.segments(listOf(Event(0, LIGHT), Event(60_000, DEEP)))
        assertEquals(listOf(Segment(0, 60_000, LIGHT)), s)   // DEEP has no end: not drawn
    }

    @Test fun unknownStagesAndLongSilencesStayGaps() {
        val h = 3_600_000L
        val s = SleepStages.segments(listOf(Event(0, LIGHT), Event(h, 0), Event(2 * h, 9), Event(3 * h, DEEP), Event(6 * h, REM), Event(7 * h, AWAKE)))
        assertEquals(listOf(Segment(0, h, LIGHT), Segment(6 * h, 7 * h, REM)), s)
    }

    @Test fun duplicateTimestampsKeepTheLaterReportAndEqualNeighboursMerge() {
        val s = SleepStages.segments(listOf(Event(0, LIGHT), Event(0, DEEP), Event(60_000, DEEP), Event(120_000, LIGHT), Event(180_000, AWAKE)))
        assertEquals(listOf(Segment(0, 120_000, DEEP), Segment(120_000, 180_000, LIGHT)), s)
    }

    @Test fun nightOverMidnightShowsBothDates() {
        val n = SleepStages.Night(listOf(Event(at(2026, 11, 26, 23, 10), LIGHT), Event(at(2026, 11, 27, 6, 40), AWAKE)))
        assertEquals(LocalDate.of(2026, 11, 26) to LocalDate.of(2026, 11, 27), SleepStages.dates(n, berlin))
        // the same instants seen from UTC-10 fall on one day
        val (a, b) = SleepStages.dates(n, ZoneId.of("Pacific/Honolulu"))!!
        assertEquals(a, b)
        assertNull(SleepStages.dates(SleepStages.Night(emptyList()), berlin))
    }

    @Test fun dstNightKeepsTrueDurations() {
        // 25.10.2026 02:00-03:00 occurs twice in Berlin; intervals use instants, so the night is 1 h longer
        val start = at(2026, 10, 25, 0); val end = at(2026, 10, 25, 4)
        val s = SleepStages.segments(listOf(Event(start, LIGHT), Event(end, AWAKE)), maxSegmentMs = 6 * 3_600_000L)
        assertEquals(5 * 3_600_000L, s.single().endMs - s.single().startMs)
    }

    @Test fun latestNightIsTheBlockAfterTheLastLongSilence() {
        val h = 3_600_000L
        val all = SleepStages.Night(listOf(Event(0, LIGHT), Event(h, AWAKE), Event(20 * h, LIGHT), Event(21 * h, DEEP)))
        assertEquals(listOf(20 * h, 21 * h), SleepStages.latestNight(all).events.map { it.atMs })
    }
}

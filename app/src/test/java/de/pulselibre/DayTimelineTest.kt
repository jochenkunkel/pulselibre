package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayTimelineTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 20), zone)
    private val noon = day.start.plus(Duration.ofHours(12))
    private fun at(min: Long, value: Long) = DayObservation(day.start.plus(Duration.ofMinutes(min)), value)
    private fun hr(points: List<DayObservation>, now: Instant = day.end) =
        DayTimeline.lane(DashboardMeasurement.PULSE, "Herzfrequenz", "bpm", points, day, now)
    private fun steps(points: List<DayObservation>, now: Instant = day.end) =
        DayTimeline.lane(DashboardMeasurement.STEPS, "Schritte", "", points, day, now)

    @Test fun emptyDayIsOneGapNotZero() {
        val lane = hr(emptyList(), noon)
        assertEquals(1, lane.gaps.size)
        assertEquals(Duration.ofHours(12), lane.gaps[0].duration)
        assertNull(lane.nearestIndex(noon))
    }

    @Test fun gapsIncludeLeadingAndTrailingEdgeAndStayUnbridged() {
        val lane = hr(listOf(at(60, 60), at(62, 62), at(120, 70)), noon)
        // 0-60 leading, 62-120 inner (58 min), 120-720 trailing; 60→62 is within 5 min.
        assertEquals(listOf(60L, 58L, 600L), lane.gaps.map { it.duration.toMinutes() })
        assertTrue(lane.connectsToNext(0))
        assertFalse(lane.connectsToNext(1))
    }

    @Test fun nearestIndexOnlyWithinThresholdElseNoData() {
        val lane = hr(listOf(at(100, 60)))
        assertEquals(0, lane.nearestIndex(day.start.plus(Duration.ofMinutes(103))))
        assertNull(lane.nearestIndex(day.start.plus(Duration.ofMinutes(110))))
    }

    @Test fun counterResetBreaksTheStaircase() {
        val lane = steps(listOf(at(100, 500), at(105, 900), at(108, 0), at(112, 40)))
        assertTrue(lane.connectsToNext(0))
        assertFalse("reset 900 -> 0 must not be connected", lane.connectsToNext(1))
        assertTrue(lane.connectsToNext(2))
    }

    @Test fun stepsUseTheLongerCounterGapThanHeartRate() {
        val pts = listOf(at(100, 10), at(108, 20))
        assertTrue(steps(pts).connectsToNext(0))
        assertFalse(hr(pts).connectsToNext(0))
    }

    @Test fun observationsOutsideTheDayOrAfterNowAreIgnored() {
        val lane = hr(listOf(at(-5, 50), at(30, 60), at(24 * 60 + 5, 70), at(800, 80)), noon)
        assertEquals(listOf(60L), lane.points.map { it.value })
    }

    @Test fun dstDayHasTwentyThreeHoursAndGapsFollowRealTime() {
        val dst = DashboardDay(LocalDate.of(2026, 3, 29), zone)
        val lane = DayTimeline.lane(DashboardMeasurement.PULSE, "Herzfrequenz", "bpm", emptyList(), dst, dst.end)
        assertEquals(Duration.ofHours(23), lane.gaps.single().duration)
    }

    @Test fun descriptionStatesRangeCountAndLongestGapOrNoData() {
        val d = DayTimeline.describe(listOf(hr(listOf(at(60, 55), at(61, 131)), noon), steps(emptyList(), noon)), day, noon, zone)
        assertTrue(d, d.contains("Herzfrequenz: 55–131 bpm, 2 Messwerte, 2 Lücken"))
        assertTrue(d, d.contains("längste 659 min"))
        assertTrue(d, d.contains("Schritte: keine Daten an diesem Tag."))
        val s = DayTimeline.describe(listOf(steps(listOf(at(10, 8315)), noon)), day, noon, zone)
        assertTrue(s, s.contains("8315 zuletzt") && !s.contains("  "))
    }

    @Test fun futureDayHasNoGapsAndSaysNotStarted() {
        val lane = hr(emptyList(), day.start.minusSeconds(1))
        assertTrue(lane.gaps.isEmpty())
        assertEquals("Tag noch nicht begonnen.", DayTimeline.describe(listOf(lane), day, day.start.minusSeconds(1), zone))
    }

    @Test fun zoomKeepsTheFocusFractionAndStaysInsideTheDay() {
        val v = DayViewport().zoom(0.5f, 4f)
        assertEquals(0.25f, v.span, 1e-6f)
        assertEquals(0.375f, v.start, 1e-6f)
        val edge = DayViewport().zoom(0f, 8f)
        assertEquals(0f, edge.start, 1e-6f)
        assertEquals(0.125f, edge.end, 1e-6f)
    }

    @Test fun zoomIsLimitedToFifteenMinutesAndToTheWholeDay() {
        var v = DayViewport()
        repeat(20) { v = v.zoom(0.3f, 2f) }
        assertEquals(DayViewport.MIN_SPAN, v.span, 1e-6f)
        assertEquals(DayViewport(), v.zoom(0.3f, 1e-6f))
        assertTrue(DayViewport().zoom(0.5f, 0.5f).isFull)
    }

    @Test fun panIsClampedAtBothEnds() {
        val v = DayViewport(0.4f, 0.6f)
        assertEquals(1f, v.pan(5f).end, 1e-6f)
        assertEquals(0f, v.pan(-5f).start, 1e-6f)
        assertEquals(0.2f, v.pan(5f).span, 1e-6f)
    }

    @Test fun invalidZoomFactorLeavesTheViewportUnchanged() {
        val v = DayViewport(0.2f, 0.4f)
        assertEquals(v, v.zoom(0.3f, 0f))
        assertEquals(v, v.zoom(0.3f, Float.NaN))
        assertEquals(v, v.zoom(0.3f, -2f))
    }

    @Test fun tickSpacingFollowsTheVisibleSpan() {
        val day = 24L * 60
        assertEquals(360, DayViewport.tickMinutes(1f, day))
        assertEquals(180, DayViewport.tickMinutes(0.5f, day))
        assertEquals(60, DayViewport.tickMinutes(0.2f, day))
        assertEquals(30, DayViewport.tickMinutes(0.1f, day))
        assertEquals(10, DayViewport.tickMinutes(0.02f, day))
    }
}

package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScienceModelTest {
    private val minute = 60_000L
    private fun series(points: List<SeriesPoint>, interval: Long? = minute, end: Long = 10 * minute) =
        SeriesData("m", "u", SeriesOrigin.SYNTHETIC, SeriesTimeBasis.SYNTHETIC, 0, end, interval, points)

    @Test fun emptySeriesIsEmptyNotZero() {
        val a = SeriesAnalysis.assess(series(emptyList()))
        assertEquals(SeriesState.EMPTY, a.state); assertNull(a.mean); assertEquals(0.0, a.coverage, 0.0)
    }

    @Test fun onlyInvalidAndStatusPointsGiveNoValidValues() {
        val a = SeriesAnalysis.assess(series(listOf(
            SeriesPoint(1 * minute, null, SeriesQuality.INVALID, 0), SeriesPoint(2 * minute, null, SeriesQuality.STATUS, -100))))
        assertEquals(SeriesState.NO_VALID_VALUES, a.state)
        assertEquals(1, a.invalid); assertEquals(1, a.status); assertEquals(0, a.valid); assertNull(a.minimum)
    }

    @Test fun invalidAndStatusNeverEnterTheStatistics() {
        val a = SeriesAnalysis.assess(series((0..9).map { SeriesPoint(it * minute, 60.0 + it) } +
            listOf(SeriesPoint(5 * minute, 0.0, SeriesQuality.INVALID, 0), SeriesPoint(6 * minute, -100.0, SeriesQuality.STATUS, -100))))
        assertEquals(60.0, a.minimum!!, 0.0); assertEquals(69.0, a.maximum!!, 0.0); assertEquals(64.5, a.mean!!, 0.0)
    }

    @Test fun completeSeriesHasFullCoverageAndNoGaps() {
        val a = SeriesAnalysis.assess(series((0..9).map { SeriesPoint(it * minute, 70.0) }))
        assertEquals(SeriesState.COMPLETE, a.state); assertEquals(1.0, a.coverage, 0.0); assertTrue(a.gaps.isEmpty())
    }

    @Test fun missingStretchIsAGapAndIncompleteNeverFilled() {
        val a = SeriesAnalysis.assess(series(listOf(0, 1, 2, 8, 9).map { SeriesPoint(it * minute, 70.0) }))
        assertEquals(SeriesState.INCOMPLETE, a.state)
        assertEquals(listOf(SeriesGap(2 * minute, 8 * minute)), a.gaps)
        assertEquals(0.5, a.coverage, 0.0)
    }

    @Test fun silenceAtTheEndOfTheRangeIsAGapToo() {
        val a = SeriesAnalysis.assess(series(listOf(0, 1, 2).map { SeriesPoint(it * minute, 70.0) }))
        assertEquals(listOf(SeriesGap(2 * minute, 10 * minute)), a.gaps)
    }

    @Test fun intervalIsInferredFromTheDataWhenUnknownNeverAssumed() {
        val a = SeriesAnalysis.assess(series((0..9).map { SeriesPoint(it * minute, 70.0) }, interval = null))
        assertEquals(minute, a.intervalMs)
        val few = SeriesAnalysis.assess(series(listOf(SeriesPoint(minute, 1.0)), interval = null))
        assertNull(few.intervalMs); assertTrue(few.gaps.isEmpty())
    }

    @Test fun pointsWithoutTimeAreCountedButNotDrawn() {
        val a = SeriesAnalysis.assess(series(listOf(SeriesPoint(null, 70.0), SeriesPoint(minute, 71.0))))
        assertEquals(1, a.unknownTime); assertEquals(71.0, a.minimum!!, 0.0)
    }

    @Test fun limitedAndUnverifiedValuesAreDrawnButCountedApart() {
        val a = SeriesAnalysis.assess(series(listOf(SeriesPoint(minute, 96.0, SeriesQuality.UNVERIFIED), SeriesPoint(2 * minute, 95.0, SeriesQuality.LIMITED))))
        assertEquals(1, a.unverified); assertEquals(1, a.limited); assertEquals(0, a.valid); assertNull(a.mean)
        assertTrue(a.state != SeriesState.NO_VALID_VALUES)
    }

    @Test fun sleepGapsAreNotAwakeAndInterruptionsNeedSleepOnBothSides() {
        val a = SleepAnalysis.assess(ScienceFixtures.sleep(0))
        assertEquals(1, a.gaps.size); assertEquals(25 * minute, a.gaps[0].endMs - a.gaps[0].startMs)
        assertEquals(listOf(15 * minute), a.interruptions.map { it.endMs - it.startMs })
        assertEquals(0, a.overlaps)
        assertEquals(15 * minute + 10 * minute, a.phaseMs.getValue(SleepPhase.AWAKE))
    }

    @Test fun awakeAtTheStartOrEndIsNoInterruptionAndOverlapsAreReported() {
        val a = SleepAnalysis.assess(listOf(
            SleepInterval(0, 10 * minute, SleepPhase.AWAKE), SleepInterval(10 * minute, 40 * minute, SleepPhase.LIGHT),
            SleepInterval(30 * minute, 50 * minute, SleepPhase.DEEP), SleepInterval(50 * minute, 60 * minute, SleepPhase.AWAKE)))
        assertTrue(a.interruptions.isEmpty()); assertEquals(1, a.overlaps)
        assertTrue(SleepAnalysis.assess(emptyList()).spanStartMs == null)
    }

    @Test fun fixturesAreSyntheticAndCoverEveryQualityState() {
        val all = listOf(ScienceFixtures.heartRate(0), ScienceFixtures.respiration(0), ScienceFixtures.spo2(0), ScienceFixtures.stress(0), ScienceFixtures.activity(0))
        all.forEach { assertEquals(SeriesOrigin.SYNTHETIC, it.origin); assertEquals(SeriesTimeBasis.SYNTHETIC, it.timeBasis) }
        val hr = SeriesAnalysis.assess(ScienceFixtures.heartRate(0))
        assertTrue(hr.invalid > 0 && hr.limited > 0 && hr.gaps.isNotEmpty())
        assertEquals(SeriesState.INCOMPLETE, hr.state)
        val resp = SeriesAnalysis.assess(ScienceFixtures.respiration(0))
        assertTrue(resp.status > 0); assertTrue(resp.minimum!! > 0) // status codes never become negative values
        assertTrue(SeriesAnalysis.assess(ScienceFixtures.spo2(0)).unverified > 0)
        assertTrue(SeriesAnalysis.assess(ScienceFixtures.stress(0)).invalid > 0)
        assertTrue(SeriesAnalysis.assess(ScienceFixtures.activity(0)).gaps.isNotEmpty())
    }

    @Test fun fixturesAreDeterministic() = assertEquals(ScienceFixtures.heartRate(0), ScienceFixtures.heartRate(0))
}

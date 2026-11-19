package de.pulselibre

import de.pulselibre.ActivityAtlas.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActivityAtlasTest {
    private val today = LocalDate.of(2026, 11, 19)   // Thursday
    private fun daily(vararg offsets: Pair<Int, Double>) = offsets.associate { today.minusDays(it.first.toLong()) to it.second }
    private fun real(m: ActivityAtlas.Model) = m.cells.filter { it.date != null }

    @Test fun twentyEightDayWindowHasExactlyThoseDaysAndIsCalendarAligned() {
        val m = ActivityAtlas.build(emptyMap(), today, 28)
        assertEquals(28, real(m).size)
        assertEquals(today.minusDays(27), m.start)
        assertEquals(0, m.cells.size % 7)
        // Window starts on a Thursday: three padding cells (Mon..Wed) before it, Monday-first columns.
        val firstReal = m.cells.indexOfFirst { it.date != null }
        assertEquals(today.minusDays(27).dayOfWeek.value - 1, firstReal)
        assertTrue(m.cells.take(firstReal).all { it.date == null })
        assertEquals(m.cells.size / 7, m.weeks)
    }

    @Test fun missingDayBetweenValidDaysIsNoDataNotZero() {
        val m = ActivityAtlas.build(daily(3 to 6000.0, 1 to 8000.0), today, 7)
        val gap = real(m).single { it.date == today.minusDays(2) }
        assertEquals(Level.NO_DATA, gap.level)
        assertNull(gap.steps)
        assertTrue(ActivityAtlas.describe(gap).contains("Keine verifizierten Tagesdaten"))
        assertFalse(ActivityAtlas.describe(gap).contains("0 Schritte"))
    }

    @Test fun ninetyDayWindowWithOnlyThirtyDaysShowsSixtyMissing() {
        val data = (1..30).associate { today.minusDays(it.toLong()) to 5000.0 + it * 10 }
        val m = ActivityAtlas.build(data, today, 90)
        assertEquals(30, m.validDays)
        assertEquals(60, m.missingDays)
        assertEquals("30 von 90 Tagen mit verifizierten Schrittwerten", m.summary())
    }

    @Test fun levelsAreRelativeToTheObservedDaysAndNeverAHealthClaim() {
        val data = (1..9).associate { today.minusDays(it.toLong()) to it * 1000.0 }   // 1000..9000
        val m = ActivityAtlas.build(data, today, 14)
        val byDate = real(m).associateBy { it.date }
        assertEquals(Level.LOW, byDate[today.minusDays(1)]!!.level)    // 1000 steps
        assertEquals(Level.HIGH, byDate[today.minusDays(9)]!!.level)   // 9000 steps
        assertTrue(Level.entries.none { ActivityAtlas.levelLabel(it).contains("gesund") })
    }

    @Test fun todayIsPartialAndDoesNotInfluenceTheScale() {
        val data = (1..9).associate { today.minusDays(it.toLong()) to it * 1000.0 } + (today to 50.0)
        val m = ActivityAtlas.build(data, today, 14)
        val t = real(m).single { it.date == today }
        assertTrue(t.partialToday)
        assertEquals(50, t.steps)
        assertTrue(ActivityAtlas.describe(t).contains("noch nicht abgeschlossen"))
        // scale is the same as without today's value
        val without = ActivityAtlas.build(data - today, today, 14)
        assertEquals(real(without).filter { it.date != today }.map { it.level }, real(m).filter { it.date != today }.map { it.level })
    }

    @Test fun invalidValuesAreTreatedAsMissing() {
        val data = mapOf(today.minusDays(1) to Double.NaN, today.minusDays(2) to -5.0, today.minusDays(3) to 4000.0)
        val m = ActivityAtlas.build(data, today, 7)
        assertEquals(1, m.validDays)
    }

    @Test fun fewValidDaysUseOneNeutralClassInsteadOfMisleadingRelativeLevels() {
        val m = ActivityAtlas.build(daily(1 to 2000.0, 2 to 9000.0), today, 7)
        assertTrue(real(m).filter { it.steps != null }.all { it.level == Level.MID })
    }

    @Test fun zeroStepsOnAKnownDayIsValidData() {
        val m = ActivityAtlas.build(daily(2 to 0.0, 3 to 3000.0, 4 to 5000.0, 5 to 7000.0), today, 7)
        assertEquals(4, m.validDays)
        assertEquals(0, real(m).single { it.date == today.minusDays(2) }.steps)
    }

    @Test fun sevenDayWindowAlignedWeekHasNoTrailingPaddingWhenEndingOnSunday() {
        val sunday = LocalDate.of(2026, 11, 22)
        val m = ActivityAtlas.build(emptyMap(), sunday, 7)
        assertEquals(7, m.cells.size)
        assertTrue(m.cells.all { it.date != null })
    }
}

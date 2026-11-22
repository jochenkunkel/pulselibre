package de.pulselibre

import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Audit #6 A6-33: the trend cache expires by age and by calendar day and can be dropped when stored data changes. */
class TrendCacheTest {
    private val day = LocalDate.of(2026, 11, 22)

    private fun data(today: LocalDate): TrendCardView.Data {
        val empty = emptyMap<LocalDate, Double>()
        val rest = TrendAnalysis.analyze(empty, today, 7, 1.0)
        val steps = TrendAnalysis.analyze(empty, today, 7, 500.0)
        return TrendCardView.Data(
            FitnessOverview.assess(steps, rest), listOf(rest), listOf(steps),
            PairedCorrelation.analyze(empty, empty, 1), emptyList(), today = today
        )
    }

    @After fun clean() = TrendCache.invalidate()

    @Test fun returnsTheStoredResultWhileFreshOnTheSameDay() {
        val d = data(day)
        TrendCache.store(d)
        assertSame(d, TrendCache.fresh(System.currentTimeMillis(), day))
    }

    @Test fun expiresAfterTenMinutes() {
        TrendCache.store(data(day))
        assertNull(TrendCache.fresh(System.currentTimeMillis() + TrendCache.MAX_AGE_MS + 1, day))
    }

    @Test fun expiresWhenTheCalendarDayChanges() {
        TrendCache.store(data(day))
        assertNull(TrendCache.fresh(System.currentTimeMillis(), day.plusDays(1)))
    }

    @Test fun invalidateDropsTheCachedResult() {
        TrendCache.store(data(day))
        assertNotNull(TrendCache.fresh(System.currentTimeMillis(), day))
        TrendCache.invalidate()
        assertNull(TrendCache.fresh(System.currentTimeMillis(), day))
    }
}

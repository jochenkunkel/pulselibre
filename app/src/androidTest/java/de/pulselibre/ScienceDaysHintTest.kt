package de.pulselibre

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic, emulator-only guard for PR #573's sparse-days hint. The predicate is private to
 * the view; reflection is used deliberately so the GUI-owned production file stays untouched.
 * No real measurements, no device, no interpolation.
 */
@RunWith(AndroidJUnit4::class)
class ScienceDaysHintTest {

    @Test fun sparseDaysHintUsesDistinctLocalDaysHalfThresholdAndSevenDayGuard() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val view = ScienceDashboardView(context)
            val method = ScienceDashboardView::class.java
                .getDeclaredMethod("sparseDaysHint", SeriesData::class.java)
            method.isAccessible = true

            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            fun series(daysBack: IntRange, rangeDays: Int): SeriesData = SeriesData(
                metricKey = ScienceSeriesLoader.HEART_RATE_BAND.key,
                unit = "bpm",
                origin = SeriesOrigin.SYNTHETIC,
                timeBasis = SeriesTimeBasis.SYNTHETIC,
                rangeStartMs = now - rangeDays * day,
                rangeEndMs = now,
                nominalIntervalMs = null,
                points = daysBack.map { SeriesPoint(timeMs = now - it * day, value = 60.0 + it) }
            )

            // 7-day view with 3 distinct days: 3 * 2 < 7, so the honest hint must appear.
            val sparse = method.invoke(view, series(0..2, 7)) as String?
            assertNotNull(sparse)
            assertTrue(sparse!!.contains("3"))
            assertTrue(sparse.contains("7"))

            // 4 distinct days is no longer "less than half" of 7 days.
            assertNull(method.invoke(view, series(0..3, 7)))

            // The 24-hour view is excluded entirely (rangeDays < 7).
            assertNull(method.invoke(view, series(0..0, 1)))

            // A series without time information must never claim a day count.
            val unknownTime = SeriesData(
                metricKey = ScienceSeriesLoader.HEART_RATE_BAND.key,
                unit = "bpm", origin = SeriesOrigin.SYNTHETIC, timeBasis = SeriesTimeBasis.SYNTHETIC,
                rangeStartMs = now - 7 * day, rangeEndMs = now, nominalIntervalMs = null,
                points = listOf(SeriesPoint(timeMs = null, value = 60.0))
            )
            assertNull(method.invoke(view, unknownTime))
        }
    }
}

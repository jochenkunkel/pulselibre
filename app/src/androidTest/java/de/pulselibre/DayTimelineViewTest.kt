package de.pulselibre

import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DayTimelineViewTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val day = DashboardDay(LocalDate.of(2026, 11, 20), zone)
    private val now = day.end

    private fun obs(min: Long, v: Long) = DayObservation(day.start.plus(Duration.ofMinutes(min)), v)

    @Test fun crosshairReportsEveryLaneAndSaysNoDataInsideGaps() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val lanes = listOf(
                DayTimeline.lane(DashboardMeasurement.PULSE, "Herzfrequenz", "bpm", listOf(obs(600, 72), obs(602, 74)), day, now),
                DayTimeline.lane(DashboardMeasurement.STEPS, "Schritte", "", listOf(obs(300, 1000), obs(304, 1200)), day, now),
            )
            val view = DayTimelineView(context, day, lanes, zone, 8100, Color.WHITE, Color.GRAY, listOf(Color.CYAN, Color.GREEN))
            view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            var reported = ""
            view.onSelect = { _, line -> reported = line }
            view.select(601f / (24 * 60))
            assertTrue(reported, reported.contains("Herzfrequenz: 72") || reported.contains("Herzfrequenz: 74"))
            assertTrue(reported, reported.contains("Schritte: keine Daten"))
            view.select(302f / (24 * 60))
            assertTrue(reported, reported.contains("Schritte: 1000") || reported.contains("Schritte: 1200"))
            assertTrue(reported, reported.contains("Herzfrequenz: keine Daten"))
            assertTrue(view.measuredHeight > 0)
        }
    }
}

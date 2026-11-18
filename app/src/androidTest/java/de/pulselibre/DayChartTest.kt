package de.pulselibre

import android.graphics.Color
import android.view.MotionEvent
import android.widget.ScrollView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayChartTest {
    @Test fun tapAndHorizontalDragSelectObservationsButVerticalGestureDoesNot() {
        val day = DashboardDay(LocalDate.of(2026, 11, 18), ZoneOffset.UTC)
        val points = listOf(6L, 12L, 18L).map { DayObservation(day.start.plusSeconds(it * 3600), 70) }
        val chart = DayChart(ApplicationProvider.getApplicationContext(), day, points,
            null, false, Color.BLACK, Color.GRAY, Color.BLACK)
        ScrollView(chart.context).addView(chart)
        chart.layout(0, 0, 400, 300)
        chart.onSelect = { chart.selected = it }

        fun send(action: Int, x: Float, y: Float) {
            val event = MotionEvent.obtain(0, 0, action, x, y, 0)
            try {
                chart.onTouchEvent(event)
            } finally {
                event.recycle()
            }
        }

        send(MotionEvent.ACTION_DOWN, 100f, 150f)
        send(MotionEvent.ACTION_UP, 100f, 150f)
        assertEquals(0, chart.selected)

        send(MotionEvent.ACTION_DOWN, 100f, 150f)
        send(MotionEvent.ACTION_MOVE, 200f, 150f)
        assertEquals(1, chart.selected)
        send(MotionEvent.ACTION_UP, 300f, 150f)
        assertEquals(2, chart.selected)

        send(MotionEvent.ACTION_DOWN, 100f, 100f)
        send(MotionEvent.ACTION_MOVE, 100f, 200f)
        send(MotionEvent.ACTION_UP, 100f, 250f)
        assertEquals(2, chart.selected)
    }
}

package de.pulselibre

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryOverviewViewTest {
    @Test fun rangeControlsRefreshAndEmptyStateHasNoSyntheticSamples() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val end = Instant.parse("2026-11-17T12:00:00Z")
        var requested = HistoryRange.DAY
        lateinit var view: HistoryOverviewView
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view = HistoryOverviewView(context) { range ->
                requested = range
                HistorySnapshot(range, range.startAt(end), end, emptyList(), emptyList())
            }
            view.refresh()
            assertTrue(containsText(view, "Keine Messdaten in diesem Zeitraum"))
            assertEquals(3, descendants(view).filter { it.contentDescription?.contains("ausgewählt") == true }.size)
            val week = descendants(view).single { it.contentDescription?.startsWith("7 Tage") == true }
            week.performClick()
            assertEquals(HistoryRange.WEEK, requested)
            assertTrue(containsText(view, "Keine Messdaten in diesem Zeitraum"))
        }
    }

    @Test fun chartAccessibilityReportsOnlyStoredPointsAndNoInterpolation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val end = Instant.parse("2026-11-17T12:00:00Z")
        val start = HistoryRange.DAY.startAt(end)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = HistoryOverviewView(context) { range ->
                HistorySnapshot(range, start, end,
                    listOf(HistoryPoint(end.minusSeconds(60), 72)), emptyList())
            }
            view.refresh()
            val chart = descendants(view).single { it.contentDescription?.contains("Messpunkte") == true }
            assertTrue(chart.contentDescription.contains("1 gespeicherte Messwerte in 1 belegten Zeitfenstern"))
            assertTrue(chart.contentDescription.contains("Markierung ist der Median"))
            assertTrue(chart.contentDescription.contains("keine Verbindung oder Interpolation"))
            assertTrue(containsText(view, "Keine Messdaten in diesem Zeitraum"))
        }
    }

    private fun containsText(view: View, expected: String): Boolean =
        (view is TextView && view.text.toString() == expected) ||
            (view is ViewGroup && (0 until view.childCount).any { containsText(view.getChildAt(it), expected) })

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}

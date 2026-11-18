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
            view.refreshSynchronously()
            assertTrue(containsText(view, "Keine Messdaten in diesem Zeitraum"))
            val controls = descendants(view).filter { candidate ->
                HistoryRange.entries.any { range ->
                    candidate.contentDescription?.toString()?.substringBefore(",") == range.label
                }
            }
            assertEquals(3, controls.size)
            assertEquals(HistoryRange.DAY.label, (controls.single { it.isSelected } as TextView).text)
            val week = descendants(view).single { it.contentDescription?.startsWith("7 Tage") == true }
            week.performClick()
            assertEquals(HistoryRange.WEEK, requested)
            assertTrue(descendants(view).single { it.contentDescription?.startsWith("7 Tage") == true }.isSelected)
            assertTrue(containsText(view, "Keine Messdaten in diesem Zeitraum"))
        }
    }

    @Test fun compactLegendUsesIconsAndKeepsFullMeaningAccessible() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val end = Instant.parse("2026-11-17T12:00:00Z")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = HistoryOverviewView(context) { range ->
                HistorySnapshot(range, range.startAt(end), end, emptyList(), emptyList())
            }
            view.refreshSynchronously()
            val legend = descendants(view).single { it.contentDescription?.startsWith("Verlaufslegende.") == true }
            assertTrue(legend.contentDescription.contains("Empfang auf dem Telefon"))
            assertTrue(legend.contentDescription.contains("Median"))
            assertTrue(legend.contentDescription.contains("Leere Fenster"))
            assertEquals(3, descendants(legend).filterIsInstance<DashboardInfoIconView>().size)
            assertFalse(containsText(view, "Werte werden je Zeitfenster als Median"))
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
            view.refreshSynchronously()
            val chart = descendants(view).single { it.contentDescription?.startsWith("1 gespeicherte Messwerte") == true }
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

    @Test fun asyncRefreshRendersOffTheUiThreadAndOnlyNewestRequestWins() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            .let { androidx.appcompat.view.ContextThemeWrapper(it, androidx.appcompat.R.style.Theme_AppCompat) }
        val loadThreads = java.util.Collections.synchronizedList(mutableListOf<Boolean>())
        lateinit var view: HistoryOverviewView
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view = HistoryOverviewView(context) { range ->
                loadThreads += (android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
                val now = java.time.Instant.parse("2026-11-18T12:00:00Z")
                HistorySnapshot(range, range.startAt(now), now, emptyList(), emptyList())
            }
            view.refresh(); view.refresh()
        }
        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline && view.childCount == 0) Thread.sleep(50)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertTrue("view must have rendered", view.childCount > 0)
        assertTrue("loading must not run on the main thread", loadThreads.isNotEmpty() && loadThreads.none { it })
    }
}

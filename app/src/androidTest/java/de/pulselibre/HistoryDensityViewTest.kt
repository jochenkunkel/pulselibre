package de.pulselibre

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** #219: switch between "Punkte" and "Verteilung" per metric card. */
@RunWith(AndroidJUnit4::class)
class HistoryDensityViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun all(v: View): List<View> = listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun densityModeShowsTheDensityChartAndLegendAndKeepsThePointsViewAvailable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val end = Instant.parse("2026-11-17T12:00:00Z")
            val start = HistoryRange.DAY.startAt(end)
            val hr = (0 until 200).map { HistoryPoint(start.plusSeconds(it * 400L), 60 + (it % 7) * 3) }
            val spo2 = (0 until 100).map { HistoryPoint(start.plusSeconds(it * 800L), 94 + (it % 5)) }
            val view = HistoryOverviewView(context) { range -> HistorySnapshot(range, start, end, hr, spo2) }
            view.refreshSynchronously()
            val title = "Herzfrequenz"
            val pointsButton = all(view).first { it.tag == "history_mode_Punkte_$title" }
            val densityButton = all(view).first { it.tag == "history_mode_Verteilung_$title" }
            // default: points view
            assertTrue(all(view).any { it.tag == "history_chart_legend" && it.isShown.not() || it.tag == "history_chart_legend" })
            assertEquals(null, all(view).firstOrNull { it.tag == "history_density_chart" })
            densityButton.performClick()
            val chart = all(view).first { it.tag == "history_density_chart" }
            assertNotNull(chart)
            assertEquals(View.VISIBLE, chart.visibility)
            val legend = all(view).filterIsInstance<TextView>().first { it.tag == "history_density_legend" }
            assertEquals(View.VISIBLE, legend.visibility)
            assertTrue(legend.text.toString().contains("relative Häufigkeit"))
            pointsButton.performClick()
            assertEquals(View.GONE, chart.visibility)
            assertEquals(View.GONE, all(view).filterIsInstance<TextView>().first { it.tag == "history_density_legend" }.visibility)
        }
    }
}

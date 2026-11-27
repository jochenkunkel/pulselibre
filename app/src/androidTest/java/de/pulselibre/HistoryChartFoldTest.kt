package de.pulselibre

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** #763/#764: one foldable chart per measure, fold state survives a new view (app restart). */
@RunWith(AndroidJUnit4::class)
class HistoryChartFoldTest {
    private val context = AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>())
    private fun all(v: View): List<View> = listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())
    private fun prefs() = context.getSharedPreferences("history_charts", Context.MODE_PRIVATE)

    @Before fun clear() { prefs().edit().clear().commit() }
    @After fun cleanUp() { prefs().edit().clear().commit() }

    private val end = Instant.parse("2026-11-17T12:00:00Z")
    private fun snapshot(range: HistoryRange): HistorySnapshot {
        val start = range.startAt(end)
        val hr = (0 until 20).map { HistoryPoint(start.plusSeconds(it * 600L), 60 + it) }
        val stress = (0 until 10).map { HistoryPoint(start.plusSeconds(it * 900L), 20 + it) }
        return HistorySnapshot(range, start, end, emptyList(), emptyList(), unified = mapOf("heart_rate" to hr, "stress" to stress))
    }

    @Test fun eachMeasureHasOneChartAndFoldingPersists() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = HistoryOverviewView(context) { snapshot(it) }
            view.refreshSynchronously()
            val views = all(view)
            assertEquals(1, views.count { it.tag == "history_series_Herzfrequenz" })
            assertNotNull(views.firstOrNull { it.tag == "history_series_Stress" })
            assertNotNull("SpO2 stays visible without data", views.firstOrNull { it.tag == "history_series_Sauerstoffsättigung · SpO₂" })
            assertNull("measures without data are not shown", views.firstOrNull { it.tag == "history_series_HRV (ms)" })
            // stress starts folded: no chart, expand button
            val stressFold = views.first { it.tag == "history_fold_Stress" } as TextView
            assertEquals("+", stressFold.text.toString())
            assertTrue(stressFold.contentDescription.isNotBlank())
            assertNull(all(views.first { it.tag == "history_series_Stress" }).firstOrNull { it.tag == "history_mode_Stress" })
            // fold heart rate
            (views.first { it.tag == "history_fold_Herzfrequenz" } as TextView).performClick()
            assertNull(all(view).firstOrNull { it.tag == "history_mode_Herzfrequenz" })
            assertEquals(1, all(view).count { it.tag == "history_series_Herzfrequenz" })
            // expand stress
            (all(view).first { it.tag == "history_fold_Stress" }).performClick()
            assertNotNull(all(view).firstOrNull { it.tag == "history_mode_Stress" })
            assertFalse(prefs().getBoolean("expanded_heart_rate", true))
            // a new view (as after a restart) keeps the choice
            val again = HistoryOverviewView(context) { snapshot(it) }
            again.refreshSynchronously()
            assertNull(all(again).firstOrNull { it.tag == "history_mode_Herzfrequenz" })
            assertNotNull(all(again).firstOrNull { it.tag == "history_mode_Stress" })
        }
    }
}

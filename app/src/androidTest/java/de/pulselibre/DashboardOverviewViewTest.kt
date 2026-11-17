package de.pulselibre

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardOverviewViewTest {
    private fun context(fontScale: Float, width: Int): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            this.fontScale = fontScale
            screenWidthDp = width
        })
    }

    @Test fun emptyCardsAreAccessibleAndNeverSeedMeasurements() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DashboardOverviewView(context(1f, 400))
            view.render(emptyList())
            val cards = cards(view)
            assertEquals(5, cards.size)
            assertTrue(cards.all { it.contentDescription.contains("Keine Messdaten") })
            assertEquals("dashboard_metric_Herzfrequenz", cards[0].tag)
            assertEquals("dashboard_metric_Sauerstoffsättigung · SpO₂", cards[1].tag)
            assertEquals("dashboard_metric_Schlaf", cards[2].tag)
            assertEquals(LinearLayout.VERTICAL, (cards[0].parent as LinearLayout).orientation)
        }
    }

    @Test fun largeFontsAndNarrowScreensStackCardsAndKeepReceiptNotes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for ((scale, width) in listOf(2f to 400, 1f to 320)) {
                val view = DashboardOverviewView(context(scale, width))
                val note = "Letzter Empfang: 17.11. 08:00 · älter als 15 min"
                view.render(listOf(DashboardMetric("Herzfrequenz", "71 bpm", note)))
                val pulse = cards(view).first()
                assertTrue(pulse.contentDescription.contains("71 bpm"))
                assertTrue(pulse.contentDescription.contains(note))
                assertEquals(LinearLayout.VERTICAL, (pulse.parent as LinearLayout).orientation)
                // A later empty refresh must remove the previous observed value.
                view.render(emptyList())
                assertFalse(cards(view).first().contentDescription.contains("71 bpm"))
            }
        }
    }

    private fun cards(view: View): List<View> {
        if (view.tag?.toString()?.startsWith("dashboard_metric_") == true) return listOf(view)
        return if (view is ViewGroup) (0 until view.childCount).flatMap { cards(view.getChildAt(it)) }
        else emptyList()
    }
}

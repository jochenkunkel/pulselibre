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
            cards.forEach { card ->
                val title = card.tag.toString().removePrefix("dashboard_metric_")
                assertNotNull(card.findViewWithTag<View>("dashboard_metric_icon_$title"))
            }
            assertEquals(LinearLayout.HORIZONTAL, (cards[0].parent as LinearLayout).orientation)
            cards.forEach { card ->
                val indicator = descendants(card).single { it.tag?.toString()?.startsWith("reception_indicator_") == true }
                assertTrue(indicator.isClickable)
                assertTrue(indicator.isFocusable)
                assertTrue(indicator.minimumWidth >= (48 * view.resources.displayMetrics.density).toInt())
                assertTrue(indicator.contentDescription.contains("Antippen"))
            }
        }
    }

    @Test fun largeFontsStackButNarrowDefaultScreensKeepTwoColumnsAndHideReceiptNotes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for ((scale, width, stacked) in listOf(Triple(2f, 400, true), Triple(1f, 320, false))) {
                val view = DashboardOverviewView(context(scale, width))
                val note = "Letzter Empfang: 17.11. 08:00 · älter als 15 min"
                view.render(listOf(DashboardMetric("Herzfrequenz", "71 bpm", note)))
                val pulse = cards(view).first()
                assertTrue(pulse.contentDescription.contains("71 bpm"))
                assertFalse(pulse.contentDescription.contains(note))
                assertEquals(if (stacked) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL,
                    (pulse.parent as LinearLayout).orientation)
                val visibleLabels = descendants(pulse).filterIsInstance<android.widget.TextView>().map { it.text.toString() }
                assertFalse(visibleLabels.any { it.contains("Letzter Empfang") || it.contains("älter als") })
                assertNotNull(pulse.findViewWithTag<View>("reception_indicator_Herzfrequenz"))
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

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}

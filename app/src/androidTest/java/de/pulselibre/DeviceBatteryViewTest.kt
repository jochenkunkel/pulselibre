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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #159: the battery row is honest without data and shows percent, bar and freshness with stored data. */
@RunWith(AndroidJUnit4::class)
class DeviceBatteryViewTest {
    private val context = AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>())
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun withoutStoredBatteryTheRowStaysNotAvailable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DeviceOverviewView(context)
            view.showBattery(null)
            assertNull(view.findViewWithTag<View>("device_battery_row"))
            val texts = all(view).filterIsInstance<TextView>().map { it.text.toString() }
            assertEquals(4, texts.count { it == "Nicht verfügbar" })
        }
    }

    @Test fun storedBatteryShowsPercentBarFreshnessAndDescription() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DeviceOverviewView(context)
            view.showBattery(BatteryReading(37, null, System.currentTimeMillis() - 120_000))
            val row = view.findViewWithTag<View>("device_battery_row")
            assertNotNull(row)
            val texts = all(row).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(texts.contains("37 %"))
            assertTrue(texts.any { it.startsWith("Stand: vor") })
            assertNotNull(row.findViewWithTag<View>("battery_bar"))
            assertTrue(row.contentDescription.toString().startsWith("Akku: 37 Prozent"))
            // the other three band values stay honestly unavailable
            assertEquals(3, all(view).filterIsInstance<TextView>().count { it.text.toString() == "Nicht verfügbar" })
        }
    }

    @Test fun staleBatteryIsLabelledAsLastKnownState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DeviceOverviewView(context)
            view.showBattery(BatteryReading(37, null, System.currentTimeMillis() - 48L * 3600_000))
            val texts = all(view.findViewWithTag<View>("device_battery_row")).filterIsInstance<TextView>().map { it.text.toString() }
            assertTrue(texts.any { it.startsWith("Veraltet · letzter bekannter Stand") })
        }
    }
}

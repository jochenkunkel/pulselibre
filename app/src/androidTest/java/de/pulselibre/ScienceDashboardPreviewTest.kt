package de.pulselibre

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic science dashboard preview: always labelled, quality states drawn and described, large series cheap to draw. */
@RunWith(AndroidJUnit4::class)
class ScienceDashboardPreviewTest {
    private fun all(v: View): List<View> =
        listOf(v) + ((v as? ViewGroup)?.let { g -> (0 until g.childCount).flatMap { all(g.getChildAt(it)) } } ?: emptyList())

    @Test fun previewIsAlwaysLabelledSyntheticAndShowsEveryStateAndRange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ScienceDashboardPreview.show(it) }
            val dialog = DialogTestSupport.awaitDialog("Dashboard-Vorschau")
            assertTrue(DialogTestSupport.containsText(dialog, "SYNTHETISCHE TESTDATEN"))
            assertTrue(DialogTestSupport.containsText(dialog, "keine echten Gesundheitsdaten"))
            listOf("Herzfrequenz", "Atemfrequenz", "SpO₂ (ungeprüft)", "Stress (Geräteskala)", "Schlaf", "Zustände", "Lädt", "Fehler", "Leer", "Unvollständige Datenbasis")
                .forEach { assertTrue("Fehlt: $it", DialogTestSupport.containsText(dialog, it)) }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                (0..2).forEach { i -> all(dialog.window!!.decorView).first { it.tag == "science_range_$i" }.performClick() }
            }
            assertTrue(DialogTestSupport.containsText(dialog, "SYNTHETISCHE TESTDATEN")) // still labelled after switching to 30 days
        }
    }

    @Test fun seriesViewDescribesQualityAndNeverTurnsStatusIntoValues() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val palette = AppPalette.of(context)
            val view = ScienceSeriesView(context, palette)
            val data = ScienceFixtures.respiration(0)
            view.show(data)
            val description = view.contentDescription.toString()
            assertTrue(description.contains("Gerätestatus"))
            assertTrue(description.contains("nicht gezeichnet"))
            assertTrue(description.contains("Quelle synthetic"))
        }
    }

    @Test fun thirtyDayMinuteSeriesDrawsWithoutError() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val view = ScienceSeriesView(context, AppPalette.of(context))
            view.show(ScienceFixtures.heartRate(0, 720)) // 43,200 points
            view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            assertEquals(1000, bitmap.width)
        }
    }

    @Test fun sleepViewDescribesGapsAsNoDataAndInterruptions() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val view = SleepTimelineView(context, AppPalette.of(context))
            view.show(ScienceFixtures.sleep(0))
            val d = view.contentDescription.toString()
            assertTrue(d.contains("1 Unterbrechungen") && d.contains("1 Datenlücken") && d.contains("nicht gleich wach"))
            view.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            view.draw(Canvas(Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)))
        }
    }
}

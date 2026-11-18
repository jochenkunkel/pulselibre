package de.pulselibre

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceAndGaugeTest {
    @Test fun themeChoicePersistsAndActivitiesUseTheOverride() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val original = AppAppearance.preference(app)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            for (choice in listOf(ThemePreference.DARK, ThemePreference.LIGHT)) {
                instrumentation.runOnMainSync { AppAppearance.save(app, choice) }
                assertEquals(choice, AppAppearance.preference(app))
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity {
                        assertEquals(choice == ThemePreference.DARK, AppPalette.of(it).dark)
                    }
                }
                ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
                    scenario.onActivity {
                        assertEquals(choice == ThemePreference.DARK, AppPalette.of(it).dark)
                    }
                }
            }
            instrumentation.runOnMainSync { AppAppearance.save(app, ThemePreference.SYSTEM) }
            assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
        } finally {
            instrumentation.runOnMainSync { AppAppearance.save(app, original) }
        }
    }

    @Test fun compactGaugeShowsAutomaticStepContextAndRendersContinuousGradientInBothPalettes() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val prefs = app.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE)
        val original = prefs.getString("pulse_context", null)
        try {
            // A previously stored manual mode is deliberately ignored by the automatic model.
            prefs.edit().putString("pulse_context", "SPORT").commit()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                    val config = Configuration(app.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    }
                    val context = app.createConfigurationContext(config)
                    val view = DashboardOverviewView(context)
                    view.render(emptyList())
                    var gauge = descendants(view).filterIsInstance<HeartRateGaugeView>().single()
                    assertTrue(gauge.contentDescription.contains("Keine Messdaten"))
                    assertTrue(gauge.contentDescription.contains("Modus unbekannt"))
                    view.render(listOf(DashboardMetric("Herzfrequenz", "110 bpm", "Empfangen")),
                        StepActivityEstimate(PulseContext.SPORT, 101.0, 3, 180, "Schrittbasiert · heuristisch"))
                    gauge = descendants(view).filterIsInstance<HeartRateGaugeView>().single()
                    assertTrue(gauge.contentDescription.contains("Sport"))
                    assertTrue(gauge.contentDescription.contains("Schrittzähler abgeleitet"))
                    assertTrue(gauge.contentDescription.contains("110 bpm"))
                    gauge.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1100, View.MeasureSpec.AT_MOST))
                    gauge.layout(0, 0, gauge.measuredWidth, gauge.measuredHeight)
                    val bitmap = Bitmap.createBitmap(gauge.width, gauge.height, Bitmap.Config.ARGB_8888)
                    gauge.draw(Canvas(bitmap))
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val palette = AppPalette.of(context)
                    assertTrue(pixels.contains(palette.ink))
                    assertTrue(pixels.contains(palette.muted))
                    assertTrue(pixels.any { android.graphics.Color.green(it) > android.graphics.Color.red(it) * 1.2f &&
                        android.graphics.Color.green(it) > android.graphics.Color.blue(it) * 1.2f })
                    assertTrue(pixels.any { android.graphics.Color.red(it) > android.graphics.Color.green(it) * 1.35f &&
                        android.graphics.Color.green(it) > android.graphics.Color.blue(it) * 1.1f })
                    // At 110 bpm the needle points upward from the pivot along the centreline.
                    val radius = gauge.width * HeartRateGaugeModel.DIAL_RADIUS_PER_WIDTH
                    val centre = gauge.height * HeartRateGaugeModel.DIAL_CENTER_Y_PER_HEIGHT
                    val pivot = centre + HeartRateGaugeModel.pivotOffsetFromCircleCenter(radius)
                    val tip = centre - radius + gauge.width * 0.025f
                    val midpoint = ((tip + pivot) / 2f).toInt()
                    assertEquals("Zeigermitte", palette.ink, bitmap.getPixel(gauge.width / 2, midpoint))
                    bitmap.recycle()
                }
            }
        } finally {
            prefs.edit().apply { if (original == null) remove("pulse_context") else putString("pulse_context", original) }.commit()
        }
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}

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

    @Test fun gaugesRenderInBothPalettesAndManualModeDoesNotInventReadings() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val prefs = app.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE)
        val original = prefs.getString("pulse_context", null)
        try {
            prefs.edit().remove("pulse_context").commit()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                    val config = Configuration(app.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    }
                    val context = app.createConfigurationContext(config)
                    val view = DashboardOverviewView(context)
                    view.render(emptyList())
                    val gauge = descendants(view).filterIsInstance<HeartRateGaugeView>().single()
                    assertTrue(gauge.contentDescription.contains("Keine Messdaten"))
                    assertTrue(gauge.contentDescription.contains("Modus unbekannt"))
                    descendants(view).filterIsInstance<TextView>().single { it.tag == PulseContext.SPORT }.performClick()
                    assertTrue(gauge.contentDescription.contains("Sport"))
                    assertTrue(gauge.contentDescription.contains("manuell"))
                    assertTrue(gauge.contentDescription.contains("Keine Messdaten"))
                    descendants(view).filterIsInstance<TextView>().single { it.tag == PulseContext.SPORT }.performClick()
                    assertTrue(gauge.contentDescription.contains("Modus unbekannt"))
                    gauge.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.AT_MOST))
                    gauge.layout(0, 0, gauge.measuredWidth, gauge.measuredHeight)
                    val bitmap = Bitmap.createBitmap(gauge.width, gauge.height, Bitmap.Config.ARGB_8888)
                    gauge.draw(Canvas(bitmap))
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val palette = AppPalette.of(context)
                    assertTrue(pixels.contains(palette.ink))
                    assertTrue(pixels.contains(palette.muted))
                    assertTrue(pixels.contains(palette.track))
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

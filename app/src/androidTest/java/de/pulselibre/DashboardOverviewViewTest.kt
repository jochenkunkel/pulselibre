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

    @Test fun spo2BandsRemainNeutralUntilDeviceValidationIsRecorded() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f, 400)
            val base = AppPalette.of(ctx)
            val settings = SpO2DisplaySettings(95, 90)
            listOf(89, 90, 94, 95, 100).forEach { percent ->
                val lung = SpO2LungView(ctx, percent, settings.colorFor(percent, base), base.track)
                assertEquals(percent / 100f, lung.fillFraction!!, 0.001f)
                assertEquals(base.ink, lung.fillColor)
                assertTrue(lung.contentDescription.contains("keine Signalqualitätsanzeige"))
            }
            val invalid = SpO2LungView(ctx, 0, settings.colorFor(0, base), base.track)
            assertNull(invalid.fillFraction)
            assertEquals(base.muted, invalid.fillColor)
            val missing = SpO2LungView(ctx, null, settings.colorFor(null, base), base.track)
            assertNull(missing.fillFraction)
            assertEquals(base.muted, missing.fillColor)
            assertTrue(missing.contentDescription.contains("keine Messdaten"))
        }
    }

    @Test fun validatedPreviewUses95And90BoundariesOnlyWhenExplicitlyEnabled() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f, 400)
            val palette = AppPalette.of(ctx)
            val settings = SpO2DisplaySettings(95, 90, colorBandsEnabled = true)
            assertEquals(android.graphics.Color.rgb(205, 57, 65), settings.colorFor(89, palette))
            assertEquals(palette.oxygen, settings.colorFor(90, palette))
            assertEquals(palette.oxygen, settings.colorFor(94, palette))
            assertEquals(palette.oxygen, settings.colorFor(90, palette))
            assertEquals(palette.indicator, settings.colorFor(95, palette))
            assertEquals(palette.indicator, settings.colorFor(100, palette))
            assertEquals(palette.muted, settings.colorFor(0, palette))
            assertEquals(palette.muted, settings.colorFor(null, palette))
        }
    }

    @Test fun emptyCardsAreAccessibleAndNeverSeedMeasurements() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DashboardOverviewView(context(1f, 400))
            view.render(emptyList())
            val cards = cards(view)
            assertEquals(4, cards.size)
            assertTrue(cards.all { it.contentDescription.contains("Keine Messdaten") })
            val night = descendants(view).single { it.tag == "dashboard_night_oxygen_insights" }
            assertTrue(night.contentDescription.contains("nicht validiert"))
            assertTrue(night.contentDescription.contains("Keine Diagnose"))
            // loading state ("SpO₂: Nachtserie …") or, if a previous test already cached a result, the loaded cells
            assertTrue(descendants(night).any {
                it.contentDescription?.startsWith("SpO₂") == true || it.contentDescription?.startsWith("Nacht 22 bis 6 Uhr") == true
            })
            assertNull("Hinweis zum violetten Rand liegt jetzt in der Konfiguration", descendants(view).singleOrNull { it.tag == "dashboard_unvalidated_legend" })
            assertTrue(cards[1].contentDescription.contains("violetter Rand"))
            assertTrue(cards[2].contentDescription.contains("violetter Rand"))
            assertFalse(cards[0].contentDescription.contains("violetter Rand"))
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
                val params = indicator.layoutParams as android.widget.FrameLayout.LayoutParams
                assertEquals((48 * view.resources.displayMetrics.density + 0.5f).toInt(), params.width)
                assertEquals((48 * view.resources.displayMetrics.density + 0.5f).toInt(), params.height)
                assertEquals(android.view.Gravity.END,
                    params.gravity and android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                assertEquals(android.view.Gravity.TOP,
                    params.gravity and android.view.Gravity.VERTICAL_GRAVITY_MASK)
                assertTrue(indicator.contentDescription.contains("Antippen"))
            }
        }
    }

    @Test fun stepCardShowsOneProgressBarWithLargeTextOnNarrowScreens() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DashboardOverviewView(context(2f, 320))
            view.render(listOf(
                DashboardMetric("Schritte (Tageszähler)", "136", ""),
                DashboardMetric("Schrittziel", "9330", "")
            ))
            val card = cards(view).single { it.tag == "dashboard_metric_Schritte heute" }
            val labels = descendants(card).filterIsInstance<android.widget.TextView>().map { it.text.toString() }
            assertTrue("136 Schritte" in labels)
            assertTrue("Ziel (Gerät): 9.330 Schritte" in labels)
            assertTrue("≈ 1 %" in labels)
            assertEquals(1, descendants(view).count { it.tag == "dashboard_step_progress" })
        }
    }

    @Test fun ownStepGoalFromSettingsReplacesDeviceGoalAndMarksReachedGoal() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f, 400)
            val prefs = ctx.getSharedPreferences(SpO2DisplaySettings.STORE, android.content.Context.MODE_PRIVATE)
            val before = prefs.getString("step_goal", null)
            try {
                prefs.edit().putString("step_goal", "8100").commit()
                val view = DashboardOverviewView(ctx)
                view.render(listOf(
                    DashboardMetric("Schritte (Tageszähler)", "8155", ""),
                    DashboardMetric("Schrittziel", "9330", "")
                ))
                val card = cards(view).single { it.tag == "dashboard_metric_Schritte heute" }
                val labels = descendants(card).filterIsInstance<android.widget.TextView>().map { it.text.toString() }
                assertTrue(labels.toString(), "Ziel (eigenes): 8.100 Schritte · Tagesziel erreicht" in labels)
            } finally {
                prefs.edit().apply { if (before == null) remove("step_goal") else putString("step_goal", before) }.commit()
            }
        }
    }

    @Test fun customLayoutHidesTilesShowsFavouritesFirstAndResetsToDefault() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f, 400)
            val prefs = ctx.getSharedPreferences("dashboard_display", Context.MODE_PRIVATE)
            val before = prefs.getString("layout_v1", null)
            try {
                val metrics = listOf(
                    DashboardMetric("Herzfrequenz", "70 bpm", ""),
                    DashboardMetric("Sauerstoffsättigung · SpO₂", "97 %", ""),
                    DashboardMetric("Schritte (Tageszähler)", "100", ""),
                    DashboardMetric("Atemfrequenz", "15 / min", ""),
                )
                prefs.edit().remove("layout_v1").commit()
                val plain = DashboardOverviewView(ctx).apply { render(metrics) }
                assertNotNull(plain.findViewWithTag<View>("dashboard_metric_Herzfrequenz"))
                assertNotNull(plain.findViewWithTag<View>("dashboard_customize"))
                val layout = listOf(
                    DashboardLayout.Entry("Atemfrequenz", favorite = true),
                    DashboardLayout.Entry(DashboardLayout.HEART, visible = false),
                )
                prefs.edit().putString("layout_v1", DashboardLayout.encode(layout)).commit()
                val custom = DashboardOverviewView(ctx).apply { render(metrics) }
                assertNull("hidden tile must not be shown", custom.findViewWithTag<View>("dashboard_metric_Herzfrequenz"))
                assertNotNull(custom.findViewWithTag<View>("dashboard_metric_Atemfrequenz"))
                val texts = descendants(custom).filterIsInstance<android.widget.TextView>().map { it.text.toString() }
                assertTrue(texts.toString(), "Im Fokus" in texts)
                assertTrue("Startseite anpassen" in texts)
                prefs.edit().putString("layout_v1", DashboardLayout.encode(layout.map { it.copy(visible = false) })).commit()
                val none = DashboardOverviewView(ctx).apply { render(metrics) }
                assertNotNull(none.findViewWithTag<View>("dashboard_all_hidden"))
            } finally {
                prefs.edit().apply { if (before == null) remove("layout_v1") else putString("layout_v1", before) }.commit()
            }
        }
    }

    @Test fun stepCounterExplanationIsCompactButFullyAccessible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DashboardOverviewView(context(1f, 400))
            view.render(emptyList())
            val chip = descendants(view).single { it.contentDescription?.startsWith("Tagesstand.") == true }
            assertTrue(chip.contentDescription.contains("Kumulativer Gerätezähler"))
            assertTrue(chip.contentDescription.contains("nicht addiert"))
            assertTrue(descendants(chip).any { it is android.widget.ImageView })
            assertFalse(descendants(view).filterIsInstance<android.widget.TextView>()
                .any { it.text.toString().contains("kumulativer Tageszähler, keine Summe") })
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

    @Test fun profilePortraitIsScalableAndAccessible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f, 400)
            val portrait = BodyProfileAvatarView(ctx)
            portrait.measure(
                View.MeasureSpec.makeMeasureSpec(112, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY)
            )
            portrait.layout(0, 0, 112, 120)
            val bitmap = android.graphics.Bitmap.createBitmap(112, 120, android.graphics.Bitmap.Config.ARGB_8888)
            portrait.draw(android.graphics.Canvas(bitmap))
            assertTrue((0 until bitmap.height).any { y ->
                (0 until bitmap.width).any { x -> android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 0 }
            })
            assertTrue(portrait.contentDescription.contains("Illustration"))
        }
    }
}

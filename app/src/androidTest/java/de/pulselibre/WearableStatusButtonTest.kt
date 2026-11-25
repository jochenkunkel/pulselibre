package de.pulselibre

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** #649: one combined header control, four distinguishable states, 48 dp target, settings unchanged on tap. */
@RunWith(AndroidJUnit4::class)
class WearableStatusButtonTest {
    private fun render(button: WearableStatusButton): Bitmap {
        button.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        button.layout(0, 0, button.measuredWidth, button.measuredHeight)
        return Bitmap.createBitmap(button.measuredWidth, button.measuredHeight, Bitmap.Config.ARGB_8888).also { button.draw(Canvas(it)) }
    }

    @Test fun fourStatesDrawDistinctlyAndKeepTheTouchTarget() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val density = context.resources.displayMetrics.density
            val colors = WearState.entries.map { state ->
                val button = WearableStatusButton(context)
                button.show(state, if (state == WearState.OFFLINE) null else WearableStatus.BatteryShown(62, true, 0))
                val bitmap = render(button)
                assertEquals((48 * density).toInt(), bitmap.height)
                // Inside the status circle (centre 40/24, r 11) but clear of every glyph: its colour identifies the state.
                bitmap.getPixel((44 * density).toInt(), (30 * density).toInt())
            }
            assertEquals("each state needs its own status colour", colors.size, colors.toSet().size)
        }
    }

    @Test fun glyphsAreCentredInTheStatusCircle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val d = context.resources.displayMetrics.density
            listOf(WearState.WORN, WearState.NOT_WORN, WearState.UNKNOWN).forEach { state ->
                val button = WearableStatusButton(context)
                button.show(state, null)
                val bmp = render(button)
                var sx = 0.0; var sy = 0.0; var n = 0
                // White glyph pixels inside the circle (centre 40/24, radius 11 design units).
                for (x in ((29 * d).toInt())..((51 * d).toInt())) for (y in ((13 * d).toInt())..((35 * d).toInt())) {
                    val c = bmp.getPixel(x, y)
                    if (android.graphics.Color.red(c) > 230 && android.graphics.Color.green(c) > 230 && android.graphics.Color.blue(c) > 230) {
                        sx += x; sy += y; n++
                    }
                }
                assertTrue("$state: no glyph", n > 0)
                val cx = sx / n / d; val cy = sy / n / d
                assertTrue("$state glyph centre x=$cx", kotlin.math.abs(cx - 40) < 1.6)
                assertTrue("$state glyph centre y=$cy", kotlin.math.abs(cy - 24) < 1.6)
            }
        }
    }

    @Test fun compactVariantLeavesRoomForTheTitle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val d = context.resources.displayMetrics.density
            val button = WearableStatusButton(context)
            button.show(WearState.WORN, WearableStatus.BatteryShown(100, true, 0))
            assertEquals((186 * d).toInt(), render(button).width)
            button.compact = true
            assertEquals((147 * d).toInt(), render(button).width)
        }
    }

    @Test fun batteryFillFollowsTheRealValueAndLowIsRed() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val d = context.resources.displayMetrics.density
            val button = WearableStatusButton(context)
            button.show(WearState.WORN, WearableStatus.BatteryShown(10, true, 0))
            val low = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            button.show(WearState.WORN, WearableStatus.BatteryShown(80, true, 0))
            val normal = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            assertNotEquals(low, normal)
            button.show(WearState.WORN, null)
            val none = render(button).getPixel((76 * d).toInt(), (22 * d).toInt()) // inside the box, above the dash
            assertEquals(render(button).getPixel((4 * d).toInt(), (24 * d).toInt()), none) // empty box shows background
        }
    }

    @Test fun staleBatteryIsDrawnGreyNotPurple() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val d = context.resources.displayMetrics.density
            val button = WearableStatusButton(context)
            button.show(WearState.WORN, WearableStatus.BatteryShown(80, true, 0))
            val fresh = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            button.show(WearState.WORN, WearableStatus.BatteryShown(80, false, 0))
            val stale = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            assertNotEquals(fresh, stale)
        }
    }

    @Test fun headerHasOneControlWithThreeLabelledTouchAreas() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertEquals(1, all(root).count { it.tag == WearableStatusButton.TAG })
                val min = (48 * activity.resources.displayMetrics.density).toInt() - 1
                listOf(WearableStatusControl.TAG_STATUS, WearableStatusControl.TAG_BATTERY, WearableStatusControl.TAG_SETTINGS).forEach { tag ->
                    val zone = root.findViewWithTag<View>(tag)
                    assertTrue("$tag clickable", zone.isClickable && zone.isFocusable)
                    assertTrue("$tag ${zone.width}x${zone.height}", zone.width >= min && zone.height >= min)
                    assertTrue("$tag label", !zone.contentDescription.isNullOrBlank())
                }
                assertTrue(root.findViewWithTag<View>(WearableStatusControl.TAG_BATTERY).contentDescription.contains("Armband-Akku (nicht Telefon)"))
                assertEquals("Einstellungen öffnen", root.findViewWithTag<View>(WearableStatusControl.TAG_SETTINGS).contentDescription)
                // The former separate antenna button is gone; the title is not cut.
                assertTrue(all(root).none { it.contentDescription?.toString()?.startsWith("CIRQA-Verbindungsstatus:") == true })
                val title = all(root).filterIsInstance<android.widget.TextView>().first { it.text.toString() == "PulsLibre" }
                assertTrue("title cut", title.paint.measureText("PulsLibre") <= title.width - title.paddingLeft - title.paddingRight + 1)
            }
        }
    }

    @Test fun eachAreaOpensItsOwnDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            listOf(WearableStatusControl.TAG_STATUS to "Tragestatus", WearableStatusControl.TAG_BATTERY to "nicht der des Telefons",
                WearableStatusControl.TAG_SETTINGS to "Speichern").forEach { (tag, text) ->
                scenario.onActivity { it.findViewById<View>(android.R.id.content).findViewWithTag<View>(tag).performClick() }
                val dialog = DialogTestSupport.awaitDialog(text)
                InstrumentationRegistry.getInstrumentation().runOnMainSync { dialog.dismiss() }
            }
        }
    }

    private fun all(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
}

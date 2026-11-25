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
                button.show(state, if (state == WearState.OFFLINE) null else 62, "x")
                val bitmap = render(button)
                assertEquals((48 * density).toInt(), bitmap.height)
                // Inside the status circle (centre 40/24, r 11) but clear of every glyph: its colour identifies the state.
                bitmap.getPixel((44 * density).toInt(), (30 * density).toInt())
            }
            assertEquals("each state needs its own status colour", colors.size, colors.toSet().size)
        }
    }

    @Test fun compactVariantLeavesRoomForTheTitle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val d = context.resources.displayMetrics.density
            val button = WearableStatusButton(context)
            button.show(WearState.WORN, 100, "x")
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
            button.show(WearState.WORN, 10, "x")
            val low = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            button.show(WearState.WORN, 80, "x")
            val normal = render(button).getPixel((65 * d).toInt(), (24 * d).toInt())
            assertNotEquals(low, normal)
            button.show(WearState.WORN, null, "x")
            val none = render(button).getPixel((76 * d).toInt(), (22 * d).toInt()) // inside the box, above the dash
            assertEquals(render(button).getPixel((4 * d).toInt(), (24 * d).toInt()), none) // empty box shows background
        }
    }

    @Test fun headerHasOneCombinedButtonThatOpensSettings() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                val buttons = all(root).filter { it.tag == WearableStatusButton.TAG }
                assertEquals(1, buttons.size)
                val button = buttons.single()
                assertTrue(button.isClickable && button.isFocusable)
                assertEquals((48 * activity.resources.displayMetrics.density + 0.5f).toInt(), button.layoutParams.height)
                assertTrue(button.contentDescription.toString(), button.contentDescription.startsWith("Einstellungen öffnen. CIRQA-Armband:"))
                assertTrue(button.contentDescription.contains("Armband-Akku (nicht Telefon)"))
                // The title is not cut by the button (S10, 360 dp: "PulsLib" with the full-width variant).
                val title = all(root).filterIsInstance<android.widget.TextView>().first { it.text.toString() == "PulsLibre" }
                assertTrue("title cut", title.paint.measureText("PulsLibre") <= title.width - title.paddingLeft - title.paddingRight + 1)
                // The former separate antenna button is gone.
                assertTrue(all(root).none { it.contentDescription?.toString()?.startsWith("CIRQA-Verbindungsstatus:") == true })
                button.performClick()
            }
            val settings = DialogTestSupport.awaitDialog("Speichern")
            InstrumentationRegistry.getInstrumentation().runOnMainSync { settings.dismiss() }
        }
    }

    private fun all(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
}
